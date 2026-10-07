package com.betta.eng.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.betta.common.exception.ServiceException;
import com.betta.eng.config.EngPronunciationProperties;
import com.tencent.core.ws.SpeechClient;
import com.tencent.core.ws.WebsocketProfile;
import com.tencent.soe.OralEvaluationResponse;

/** 腾讯云客户端配置保护的独立回归入口，不依赖真实云端凭据。 */
public class TencentPronunciationAssessmentClientTest
{
    public static void main(String[] args) throws Exception
    {
        shouldRejectMissingConfiguration();
        shouldReuseSpeechClientAndShutdownOnlyOnce();
        shouldPreserveUnexpectedException();
        shouldPreserveTencentFailureDetails();
        shouldPreserveWebSocketCloseDetails();
        shouldUseTencentSoeWebSocketEndpoint();
    }

    private static void shouldRejectMissingConfiguration()
    {
        EngPronunciationProperties properties = new EngPronunciationProperties();
        TencentPronunciationAssessmentClient client = new TencentPronunciationAssessmentClient(properties);
        try
        {
            client.assess("apple", new byte[44]);
            throw new AssertionError("未配置凭据时不得调用腾讯云");
        }
        catch (ServiceException expected)
        {
            if (!expected.getMessage().contains("暂未启用")) throw expected;
        }
        if (client.hasInitializedSpeechClient()) throw new AssertionError("未启用时不得初始化 SDK 客户端");
        client.shutdown();
        client.shutdown();
    }

    private static void shouldReuseSpeechClientAndShutdownOnlyOnce() throws Exception
    {
        EngPronunciationProperties properties = new EngPronunciationProperties();
        properties.setEnabled(true);
        properties.getTencent().setAppId("test-app-id");
        properties.getTencent().setSecretId("test-secret-id");
        properties.getTencent().setSecretKey("test-secret-key");
        CountingClient client = new CountingClient(properties);
        int concurrency = 8;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch ready = new CountDownLatch(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<SpeechClient>> futures = new ArrayList<>();
        try
        {
            for (int i = 0; i < concurrency; i++)
            {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("并发初始化栅栏等待超时");
                    return client.speechClient();
                }));
            }
            if (!ready.await(5, TimeUnit.SECONDS)) throw new AssertionError("并发初始化任务未就绪");
            start.countDown();
            SpeechClient expected = futures.get(0).get(5, TimeUnit.SECONDS);
            for (Future<SpeechClient> future : futures)
                if (future.get(5, TimeUnit.SECONDS) != expected)
                    throw new AssertionError("并发获取必须复用同一 SDK 客户端");
            if (client.createCount.get() != 1) throw new AssertionError("SDK 客户端只能创建一次");

            client.shutdown();
            client.shutdown();
            if (client.shutdownCount.get() != 1) throw new AssertionError("SDK 客户端底层只能关闭一次");
            try
            {
                client.speechClient();
                throw new AssertionError("关闭后不得再访问 SDK 客户端");
            }
            catch (ServiceException expectedException)
            {
                if (!expectedException.getMessage().contains("已关闭")) throw expectedException;
            }
        }
        finally
        {
            start.countDown();
            client.shutdown();
            executor.shutdownNow();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS))
                throw new AssertionError("并发测试线程未正常结束");
        }
    }

    /** 通过最小接缝记录 SDK 真实客户端的创建和关闭次数。 */
    private static final class CountingClient extends TencentPronunciationAssessmentClient
    {
        private final AtomicInteger createCount = new AtomicInteger();
        private final AtomicInteger shutdownCount = new AtomicInteger();

        private CountingClient(EngPronunciationProperties properties)
        {
            super(properties);
        }

        @Override
        SpeechClient createSpeechClient(WebsocketProfile profile)
        {
            createCount.incrementAndGet();
            return super.createSpeechClient(profile);
        }

        @Override
        void shutdownSpeechClient(SpeechClient client)
        {
            shutdownCount.incrementAndGet();
            super.shutdownSpeechClient(client);
        }
    }

    private static void shouldPreserveUnexpectedException()
    {
        IllegalStateException cause = new IllegalStateException("sdk failure");
        ServiceException exception = TencentPronunciationAssessmentClient.unexpectedFailure(cause);
        if (exception.getCause() != cause) throw new AssertionError("必须保留 SDK 原始异常");
    }

    private static void shouldPreserveTencentFailureDetails()
    {
        OralEvaluationResponse response = new OralEvaluationResponse();
        response.setCode(1001); response.setMessage("invalid credential");
        ServiceException exception = TencentPronunciationAssessmentClient.responseFailure(response);
        String detail = exception.getCause() == null ? "" : exception.getCause().getMessage();
        if (!detail.contains("1001") || !detail.contains("invalid credential"))
            throw new AssertionError("必须保留腾讯云失败响应的错误码和消息");
    }

    private static void shouldPreserveWebSocketCloseDetails()
    {
        ServiceException exception = TencentPronunciationAssessmentClient.connectionFailure(1008, "policy violation");
        String detail = exception.getCause() == null ? "" : exception.getCause().getMessage();
        if (!detail.contains("1008") || !detail.contains("policy violation"))
            throw new AssertionError("必须保留 WebSocket 关闭码和原因");
    }

    private static void shouldUseTencentSoeWebSocketEndpoint()
    {
        String endpoint = TencentPronunciationAssessmentClient.soeEndpoint();
        if (!"wss://soe.cloud.tencent.com/soe/api/".equals(endpoint))
            throw new AssertionError("必须使用腾讯云 SOE TLS WebSocket 地址");
    }
}
