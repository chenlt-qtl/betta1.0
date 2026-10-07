package com.betta.eng.service.impl;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.betta.common.exception.ServiceException;
import com.betta.eng.config.EngPronunciationProperties;
import com.betta.eng.domain.vo.EngPronunciationAssessmentVo;
import com.betta.eng.domain.vo.EngPronunciationPhoneVo;
import com.betta.eng.service.IPronunciationAssessmentClient;
import com.tencent.core.ws.Credential;
import com.tencent.core.ws.SpeechClient;
import com.tencent.core.ws.WebsocketProfile;
import com.tencent.soe.OralEvalConstant;
import com.tencent.soe.OralEvaluationListener;
import com.tencent.soe.OralEvaluationRequest;
import com.tencent.soe.OralEvaluationResponse;
import com.tencent.soe.OralEvaluator;
import com.tencent.soe.PhoneInfo;
import com.tencent.soe.SentenceInfo;
import com.tencent.soe.WordRsp;

/** 使用腾讯云智聆口语评测（新版）英文单词模式进行可信评分。 */
@Service
public class TencentPronunciationAssessmentClient implements IPronunciationAssessmentClient
{
    private static final Logger log = LoggerFactory.getLogger(TencentPronunciationAssessmentClient.class);
    private static final Object CLIENT_INITIALIZATION_LOCK = new Object();
    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int HANDSHAKE_TIMEOUT_SECONDS = 3;
    private static final int CONNECT_MAX_TRY_TIMES = 1;
    private static final long START_TIMEOUT_MS = 5000L;
    private static final long RESULT_TIMEOUT_MS = 10000L;
    private static final double PASS_SCORE = 60D;

    private final EngPronunciationProperties properties;
    private final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock(true);
    private volatile SpeechClient speechClient;
    private volatile boolean closed;

    public TencentPronunciationAssessmentClient(EngPronunciationProperties properties)
    {
        this.properties = properties;
    }

    @Override
    public EngPronunciationAssessmentVo assess(String word, byte[] wavAudio)
    {
        lifecycleLock.readLock().lock();
        try
        {
            ensureOpen();
            if (!properties.isAvailable()) throw new ServiceException("跟读评分服务暂未启用");
            return assessWhileOpen(word, wavAudio);
        }
        finally
        {
            lifecycleLock.readLock().unlock();
        }
    }

    /** 读锁覆盖完整评分过程，允许并发评分且防止关闭在途客户端。 */
    private EngPronunciationAssessmentVo assessWhileOpen(String word, byte[] wavAudio)
    {
        long totalStartedAt = System.nanoTime();
        OralEvaluator evaluator = null;
        AtomicReference<OralEvaluationResponse> responseRef = new AtomicReference<>();
        AtomicReference<ServiceException> failureRef = new AtomicReference<>();
        try
        {
            OralEvaluationRequest request = request(word);
            var credential = new Credential(properties.getTencent().getAppId(),
                    properties.getTencent().getSecretId(), properties.getTencent().getSecretKey());
            evaluator = new OralEvaluator(speechClient(), credential, request, listener(responseRef, failureRef));
            long startStartedAt = System.nanoTime();
            try
            {
                evaluator.start(START_TIMEOUT_MS);
            }
            finally
            {
                log.info("跟读评分客户端启动及连接结束，costMs={}", elapsedMillis(startStartedAt));
            }
            if (failureRef.get() != null) throw failureRef.get();
            long resultStartedAt = System.nanoTime();
            try
            {
                evaluator.write(wavAudio);
                evaluator.stop(RESULT_TIMEOUT_MS);
            }
            finally
            {
                log.info("跟读评分结果等待结束，costMs={}", elapsedMillis(resultStartedAt));
            }
            if (failureRef.get() != null) throw failureRef.get();
            OralEvaluationResponse response = responseRef.get();
            if (response == null || response.getCode() != 0 || response.getResult() == null)
                throw new ServiceException("未取得有效跟读评分，请重试");
            return toView(response.getResult());
        }
        catch (ServiceException exception)
        {
            throw exception;
        }
        catch (Exception exception)
        {
            throw unexpectedFailure(exception);
        }
        finally
        {
            if (evaluator != null) evaluator.close();
            log.info("跟读评分客户端调用结束，costMs={}", elapsedMillis(totalStartedAt));
        }
    }

    /** 延迟创建全局复用的 SDK 客户端，未启用评分时不创建 Netty 资源。 */
    SpeechClient speechClient()
    {
        lifecycleLock.readLock().lock();
        try
        {
            ensureOpen();
            SpeechClient current = speechClient;
            if (current != null) return current;
            synchronized (CLIENT_INITIALIZATION_LOCK)
            {
                current = speechClient;
                if (current != null) return current;
                // SDK 将连接超时和重试次数定义为全局静态字段，仅在单例首次初始化时统一设置。
                SpeechClient.connectTimeout = CONNECT_TIMEOUT_MS;
                SpeechClient.connectMaxTryTimes = CONNECT_MAX_TRY_TIMES;
                WebsocketProfile profile = WebsocketProfile.defaultWebsocketProfile();
                profile.setConnectTimeout(CONNECT_TIMEOUT_MS);
                profile.setHandshakeTimeout(HANDSHAKE_TIMEOUT_SECONDS);
                current = createSpeechClient(profile);
                speechClient = current;
                return current;
            }
        }
        finally
        {
            lifecycleLock.readLock().unlock();
        }
    }

    SpeechClient createSpeechClient(WebsocketProfile profile)
    {
        return new SpeechClient(soeEndpoint(), profile);
    }

    void shutdownSpeechClient(SpeechClient client)
    {
        client.shutdown();
    }

    /** 应用关闭时只释放一次 SDK 全局线程资源。 */
    @PreDestroy
    public void shutdown()
    {
        lifecycleLock.writeLock().lock();
        try
        {
            if (closed) return;
            closed = true;
            if (speechClient != null) shutdownSpeechClient(speechClient);
        }
        finally
        {
            lifecycleLock.writeLock().unlock();
        }
    }

    private void ensureOpen()
    {
        if (closed) throw new ServiceException("跟读评分服务已关闭");
    }

    boolean hasInitializedSpeechClient()
    {
        return speechClient != null;
    }

    private long elapsedMillis(long startedAt)
    {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private OralEvaluationRequest request(String word)
    {
        OralEvaluationRequest request = new OralEvaluationRequest();
        request.setVoiceId(UUID.randomUUID().toString());
        request.setServerEngineType("16k_en");
        request.setVoiceFormat(1);
        request.setTextMode(0);
        request.setRefText(word);
        request.setEvalMode(0);
        request.setScoreCoeff(1D);
        request.setSentenceInfoEnabled(0);
        request.setRecMode(1);
        return request;
    }

    private OralEvaluationListener listener(AtomicReference<OralEvaluationResponse> responseRef,
            AtomicReference<ServiceException> failureRef)
    {
        return new OralEvaluationListener()
        {
            @Override public void OnIntermediateResults(OralEvaluationResponse response) { capture(responseRef, response); }
            @Override public void onRecognitionStart(OralEvaluationResponse response) { }
            @Override public void onRecognitionComplete(OralEvaluationResponse response) { capture(responseRef, response); }
            @Override public void onFail(OralEvaluationResponse response) {
                failureRef.compareAndSet(null, responseFailure(response));
            }
            @Override public void onMessage(OralEvaluationResponse response) { capture(responseRef, response); }
            @Override public void onClose(int closeCode, String reason) {
                if (responseRef.get() == null)
                    failureRef.compareAndSet(null, connectionFailure(closeCode, reason));
                super.onClose(closeCode, reason);
            }
        };
    }

    private void capture(AtomicReference<OralEvaluationResponse> target, OralEvaluationResponse response)
    {
        if (response != null && response.getResult() != null) target.set(response);
    }

    /** 保留腾讯云错误码和原始消息，统一业务文案仅返回给客户端。 */
    static ServiceException responseFailure(OralEvaluationResponse response)
    {
        String detail = response == null ? "Tencent SOE failed without response"
                : "Tencent SOE failed, code=" + response.getCode() + ", message=" + response.getMessage();
        return withCause("跟读评分服务返回失败，请重试", new IllegalStateException(detail));
    }

    /** 连接在评分结果返回前关闭时保留 WebSocket 关闭码与原因。 */
    static ServiceException connectionFailure(int closeCode, String reason)
    {
        String detail = "Tencent SOE WebSocket closed, code=" + closeCode + ", reason=" + reason;
        return withCause("跟读评分服务连接已关闭，请重试", new IllegalStateException(detail));
    }

    static String soeEndpoint()
    {
        return OralEvalConstant.DEFAULT_ORAL_EVAL_REQ_URL;
    }

    /** 保留 SDK 抛出的原始异常，便于全局异常处理器输出完整原因链。 */
    static ServiceException unexpectedFailure(Exception cause)
    {
        return withCause("跟读评分服务繁忙，请稍后重试", cause);
    }

    private static ServiceException withCause(String message, Throwable cause)
    {
        ServiceException exception = new ServiceException(message);
        exception.initCause(cause);
        return exception;
    }

    private EngPronunciationAssessmentVo toView(SentenceInfo result)
    {
        WordRsp word = result.getWords() == null ? null : result.getWords().stream()
                .filter(item -> item.getMatchTag() == 0).findFirst().orElse(null);
        boolean matched = word != null;
        double accuracy = matched ? word.getPronAccuracy() : result.getPronAccuracy();
        EngPronunciationAssessmentVo view = new EngPronunciationAssessmentVo();
        view.setScore(clampScore(result.getSuggestedScore()));
        view.setAccuracy(roundMetric(accuracy));
        view.setFluency(roundMetric(percentMetric(matched ? word.getPronFluency() : result.getPronFluency())));
        view.setCompleteness(roundMetric(percentMetric(result.getPronCompletion())));
        view.setMatched(matched);
        view.setMatchTag(matched ? 0 : firstMatchTag(result.getWords()));
        view.setPassed(matched && accuracy >= PASS_SCORE);
        view.setPhones(phoneViews(matched ? word.getPhoneInfos() : List.of()));
        return view;
    }

    private List<EngPronunciationPhoneVo> phoneViews(List<PhoneInfo> phones)
    {
        List<EngPronunciationPhoneVo> result = new ArrayList<>();
        if (phones == null) return result;
        for (PhoneInfo phone : phones)
        {
            EngPronunciationPhoneVo view = new EngPronunciationPhoneVo();
            view.setPhone(phone.getPhone()); view.setReferencePhone(phone.getReferencePhone());
            view.setReferenceLetter(phone.getReferenceLetter()); view.setAccuracy(roundMetric(phone.getPronAccuracy()));
            view.setMatched(phone.getMatchTag() == 0); view.setStress(phone.isStress());
            view.setDetectedStress(phone.isDetectedStress()); result.add(view);
        }
        return result;
    }

    private int clampScore(double value) { return (int) Math.round(Math.max(0D, Math.min(100D, value))); }
    private int firstMatchTag(List<WordRsp> words) { return words == null || words.isEmpty() ? 2 : (int) words.get(0).getMatchTag(); }
    private double percentMetric(double value) { return value >= 0D && value <= 1D ? value * 100D : value; }
    private double roundMetric(double value) { return Math.round(value * 100D) / 100D; }
}
