package com.betta.eng.service.impl;

import com.betta.common.core.domain.entity.SysUser;
import com.betta.common.core.domain.model.LoginUser;
import com.betta.common.core.redis.RedisCache;
import com.betta.common.exception.ServiceException;
import com.betta.eng.config.EngPronunciationProperties;
import com.betta.eng.domain.*;
import com.betta.eng.domain.dto.*;
import com.betta.eng.domain.vo.*;
import com.betta.eng.mapper.*;
import com.betta.eng.service.*;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** 新词关卡、全局复习、拼写测试、金币与幂等规则的无数据库回归入口。 */
public class EngStudyServiceImplTest
{
    private static final long USER_ID = 7L;
    private static final long ARTICLE_ID = 10L;

    public static void main(String[] args) throws Exception
    {
        setTestLoginUser();
        try
        {
            shouldExposePronunciationAvailabilityOnLevelMap();
            shouldReuseArticleWordsAcrossLevelsAndPronunciation();
            shouldDefaultAndIsolateChallengeSettings();
            shouldApplyChallengeSettingsWithoutChangingSpelling();
            shouldRejectInvalidSettingsAndStaleChallenge();
            shouldDisablePronunciationForInsecureAccess();
            shouldSeparateOrdinaryAndSpellingQuestions();
            shouldExposeSaltedAnswerDigestsAndKeepCheckFallback();
            shouldExposeAndEnforceSpellingEligibility();
            shouldKeepSpellingDefinitionStableWithinAttempt();
            shouldRewardSpellingMilestonesOnlyOnce();
            shouldTopUpSpellingMilestonesByStar();
            shouldExcludeWordsLearnedElsewhereButAllowFailedLevelRetry();
            shouldAutoMasterLevelWhenEveryWordWasLearnedElsewhere();
            shouldUseMatchedTextAndRewardMilestonesOnlyOnce();
            shouldRewardEveryPerfectReviewWithoutMilestoneDuplication();
            shouldStackReviewAndNewMilestoneCoin();
            shouldUseCurrentReadAfterConcurrentAttemptConflict();
            shouldRecordWrongNewWordWithoutExample();
            shouldKeepRecordWordOwnershipInMapperSql();
            shouldKeepAttemptCurrentReadInMapperSql();
            shouldLockArticleBeforeFirstLevelProgressWrite();
            shouldReturnOnlyNextEffectiveLevel();
            shouldKeepNextLevelEmptyAtTerminalLevel();
            shouldKeepHistoricalMigrationConsistent();
            shouldExposeTransactionalMutationBoundary();
            shouldApplyPronunciationBoundaryAndWeightedScore();
            shouldRejectMissingOrCrossAttemptAssessment();
            shouldRejectInvalidOrSilentWav();
            shouldTreatFailedReviewPronunciationAsNotMastered();
            shouldKeepOnlyLatestConcurrentRecording();
            shouldLimitPronunciationAttemptsPerWord();
            shouldReleaseFailedPronunciationReservation();
            shouldPreserveOriginalErrorWhenReservationReleaseFails();
            shouldEnforceConcurrentPronunciationAttemptLimit();
            shouldIsolateExpiredUserAndTestCaches();
            shouldConvergeAssessmentClientFailureWithoutCache();
            shouldRestorePersistedPronunciationOnDuplicateSubmit();
            shouldKeepPronunciationMultipartInMemory();
            shouldRejectForgedQuestionAndEmptyAudio();
            shouldRejectSameModeCrossArticleAndLevel();
            shouldScoreMultiplePronunciationsIndependently();
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
    }

    /** 无记录默认全开，且不同登录用户的设置互不影响。 */
    private static void shouldDefaultAndIsolateChallengeSettings()
    {
        Harness harness = new Harness();
        EngChallengeSettingVo defaults = harness.service.getChallengeSetting();
        assertTrue(defaults.getWordToMeaningEnabled() && defaults.getMeaningToWordEnabled()
                && defaults.getSentenceClozeEnabled() && defaults.getPronunciationEnabled(),
                "无设置记录时四种普通题型必须默认启用");

        EngChallengeSettingUpdateDto request = setting(false, true, false, false);
        harness.service.updateChallengeSetting(request);
        assertTrue(!harness.service.getChallengeSetting().getWordToMeaningEnabled(), "用户设置必须保存");
        setTestLoginUser(8L);
        try
        {
            assertTrue(harness.service.getChallengeSetting().getWordToMeaningEnabled(), "不同用户必须保持默认设置");
            harness.service.updateChallengeSetting(setting(true, false, true, true));
        }
        finally
        {
            setTestLoginUser();
        }
        assertTrue(!harness.service.getChallengeSetting().getWordToMeaningEnabled(), "切回原用户后必须读取自己的设置");
    }

    /** NEW、REVIEW 应按偏好出题，SPELLING 继续只生成独立拼写题。 */
    private static void shouldApplyChallengeSettingsWithoutChangingSpelling()
    {
        Harness harness = spellingHarness();
        harness.service.updateChallengeSetting(setting(false, true, false, false));
        EngChallengeVo fresh = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null, true);
        assertTrue(fresh.getQuestions().stream().allMatch(item -> "CN_TO_WORD".equals(item.getType())),
                "新词测试只能生成当前用户启用的题型");
        EngChallengeVo review = harness.service.getChallenge("REVIEW", null, null, List.of(1L), true);
        assertTrue(review.getQuestions().stream().allMatch(item -> "CN_TO_WORD".equals(item.getType())),
                "复习测试只能生成当前用户启用的题型");
        EngChallengeVo spelling = harness.service.getChallenge("SPELLING", null, null, List.of(1L), true);
        assertTrue(spelling.getQuestions().stream().allMatch(item -> "SENTENCE_FILL".equals(item.getType())),
                "拼写测试不得受普通题型设置影响");
    }

    /** 基础题型不得全关，且保存新设置后旧普通测试必须失效。 */
    private static void shouldRejectInvalidSettingsAndStaleChallenge()
    {
        Harness harness = new Harness();
        EngChallengeVo challenge = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null, false);
        String legacyAttemptId = challenge.getAttemptId().substring(0, challenge.getAttemptId().lastIndexOf('.'));
        EngChallengeCheckDto legacyCheck = challengeCheck(legacyAttemptId);
        assertTrue(harness.service.checkChallengeAnswer(legacyCheck).getCorrect(),
                "从未保存设置时应兼容部署前生成的纯 UUID 普通测试");
        assertThrows(() -> harness.service.checkChallengeAnswer(challengeCheck(".0")),
                "仅版本后缀不得作为测试标识");
        assertThrows(() -> harness.service.checkChallengeAnswer(challengeCheck("invalid.0")),
                "非法 UUID 前缀不得通过版本零校验");
        assertThrows(() -> harness.service.checkChallengeAnswer(challengeCheck(legacyAttemptId + ".extra.0")),
                "多个分隔符不得通过版本零校验");
        assertThrows(() -> harness.service.checkChallengeAnswer(challengeCheck(legacyAttemptId + ".00")),
                "非规范十进制版本零不得通过校验");
        assertThrows(() -> harness.service.checkChallengeAnswer(challengeCheck(legacyAttemptId + ".1")),
                "非当前版本不得通过版本零校验");
        assertThrows(() -> harness.service.updateChallengeSetting(setting(false, false, true, true)),
                "两个基础知识题型不得同时关闭");
        harness.service.updateChallengeSetting(setting(true, false, false, false));
        EngChallengeCheckDto check = challengeCheck(challenge.getAttemptId());
        assertThrows(() -> harness.service.checkChallengeAnswer(check), "设置变化后旧测试必须失效");
        assertThrows(() -> harness.service.checkChallengeAnswer(legacyCheck),
                "保存设置后部署前的纯 UUID 测试也必须失效");
        EngChallengeVo current = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null, false);
        assertTrue(harness.service.checkChallengeAnswer(challengeCheck(current.getAttemptId())).getCorrect(),
                "当前版本的规范测试标识必须有效");
        String currentUuid = current.getAttemptId().substring(0, current.getAttemptId().lastIndexOf('.'));
        assertThrows(() -> harness.service.checkChallengeAnswer(challengeCheck("invalid.1")),
                "非法 UUID 前缀不得通过版本一校验");
        assertThrows(() -> harness.service.checkChallengeAnswer(challengeCheck(currentUuid + ".extra.1")),
                "多个分隔符不得通过版本一校验");
        assertThrows(() -> harness.service.checkChallengeAnswer(challengeCheck(currentUuid + ".01")),
                "非规范十进制版本一不得通过校验");
        assertThrows(() -> harness.service.checkChallengeAnswer(challengeCheck(currentUuid + ".0")),
                "旧版本后缀不得通过版本一校验");
    }

    private static EngChallengeCheckDto challengeCheck(String attemptId)
    {
        EngChallengeCheckDto request = new EngChallengeCheckDto(); request.setAttemptId(attemptId);
        request.setMode("NEW"); request.setArticleId(ARTICLE_ID); request.setLevelNo(1);
        request.setQuestionId("WORD_TO_CN:1"); request.setAnswer("苹果"); return request;
    }

    private static EngChallengeSettingUpdateDto setting(boolean wordToMeaning, boolean meaningToWord,
            boolean sentenceCloze, boolean pronunciation)
    {
        EngChallengeSettingUpdateDto request = new EngChallengeSettingUpdateDto();
        request.setWordToMeaningEnabled(wordToMeaning); request.setMeaningToWordEnabled(meaningToWord);
        request.setSentenceClozeEnabled(sentenceCloze); request.setPronunciationEnabled(pronunciation);
        return request;
    }

    /** 地图接口应明确返回跟读评分配置是否完整可用。 */
    private static void shouldExposePronunciationAvailabilityOnLevelMap()
    {
        EngArticleLevelMapVo unavailable = new Harness(false).service.getArticleLevels(ARTICLE_ID);
        assertTrue(Boolean.FALSE.equals(unavailable.getPronunciationEnabled()),
                "跟读评分未启用时地图必须返回不可用");

        EngArticleLevelMapVo available = new Harness(true).service.getArticleLevels(ARTICLE_ID);
        assertTrue(Boolean.TRUE.equals(available.getPronunciationEnabled()),
                "跟读评分配置完整时地图必须返回可用");
    }

    /** 多关卡地图和跟读校验都只能加载一次文章全部单词。 */
    private static void shouldReuseArticleWordsAcrossLevelsAndPronunciation()
    {
        Harness harness = new Harness(true);
        harness.levelWords.put(2, List.of("dog"));
        EngArticleLevelMapVo map = harness.service.getArticleLevels(ARTICLE_ID);
        assertEquals(2, map.getLevels().size(), "多关卡地图必须保留全部有效关卡");
        assertEquals(1, harness.articleWordListReads.get(), "构建关卡地图只能查询一次文章全部单词");

        EngChallengeVo challenge = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        harness.articleWordListReads.set(0);
        harness.service.assessPronunciation(assessmentRequest(challenge.getAttemptId(), "NEW", ARTICLE_ID, 1,
                "PRONUNCIATION:1", wav(false)));
        assertEquals(1, harness.articleWordListReads.get(), "跟读题校验只能查询一次文章全部单词");
    }

    /** 非安全访问不得生成跟读题，提交时应按知识题独立计分。 */
    private static void shouldDisablePronunciationForInsecureAccess()
    {
        Harness harness = new Harness(true);
        harness.progress.put(2L, progress(2L, 99L, 1, 1));
        EngChallengeVo insecure = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null, false);
        assertTrue(Boolean.FALSE.equals(insecure.getPronunciationEnabled()), "HTTP 挑战必须标记跟读不可用");
        assertTrue(insecure.getQuestions().stream().noneMatch(item -> "PRONUNCIATION".equals(item.getType())),
                "HTTP 挑战不得生成跟读题");

        EngChallengeSubmitDto request = request(insecure.getAttemptId(), "NEW", ARTICLE_ID, 1);
        request.setAnswers(perfectAppleAnswers());
        EngChallengeResultVo result = harness.service.submitChallenge(request, false);
        assertEquals(100, result.getScore(), "HTTP 提交必须仅按知识题计算满分");
        assertEquals(0, result.getPronunciationTotalCount(), "HTTP 提交的跟读题总数必须为零");
        assertEquals(0, result.getPronunciationPassedCount(), "HTTP 提交的跟读合格数必须为零");
        assertEquals(null, result.getPronunciationAverageScore(), "HTTP 提交不得生成跟读平均分");

        Harness secureHarness = new Harness(true);
        secureHarness.progress.put(2L, progress(2L, 99L, 1, 1));
        EngChallengeVo secure = secureHarness.service.getChallenge("NEW", ARTICLE_ID, 1, null, true);
        assertTrue(Boolean.TRUE.equals(secure.getPronunciationEnabled()), "HTTPS 挑战应保留可用的跟读评分");
        assertTrue(secure.getQuestions().stream().anyMatch(item -> "PRONUNCIATION".equals(item.getType())),
                "HTTPS 挑战应继续生成跟读题");
    }

    /** 普通测试不再混入拼写题，拼写模式也不得生成选择题或跟读题。 */
    private static void shouldSeparateOrdinaryAndSpellingQuestions()
    {
        Harness harness = spellingHarness();
        EngChallengeVo ordinary = harness.service.getChallenge("REVIEW", null, null, List.of(1L));
        assertTrue(ordinary.getQuestions().stream().noneMatch(item -> "SENTENCE_FILL".equals(item.getType())),
                "普通复习不得生成拼写题");
        EngChallengeVo newWords = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        assertTrue(newWords.getQuestions().stream().noneMatch(item -> "SENTENCE_FILL".equals(item.getType())),
                "新词测试不得生成拼写题");

        EngChallengeVo spelling = harness.service.getChallenge("SPELLING", null, null, List.of(1L, 4L));
        assertTrue(spelling.getQuestions().stream().allMatch(item -> "SENTENCE_FILL".equals(item.getType())
                && Integer.valueOf(4).equals(item.getAnswerLength())), "拼写模式只能生成四字母挖空题");
        assertTrue(Boolean.FALSE.equals(spelling.getPronunciationEnabled()), "拼写模式不得启用跟读题");
    }

    /** 非跟读题返回当次测试加盐摘要，跟读题不返回，旧单题接口仍可回退。 */
    private static void shouldExposeSaltedAnswerDigestsAndKeepCheckFallback()
    {
        Harness harness = new Harness(true); harness.progress.put(2L, progress(2L, 99L, 1, 1));
        EngChallengeVo first = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        Map<String, String> answers = Map.of("WORD_TO_CN:1", "苹果", "CN_TO_WORD:1", "apple",
                "SENTENCE_CHOICE:1", "apples");
        for (EngChallengeQuestionVo question : first.getQuestions())
        {
            if ("PRONUNCIATION".equals(question.getType()))
            {
                assertEquals(null, question.getAnswerDigest(), "跟读题不得返回答案摘要");
                continue;
            }
            String expected = testAnswerDigest(first.getAttemptId(), question.getQuestionId(),
                    answers.get(question.getQuestionId()));
            assertTrue(question.getAnswerDigest() != null && question.getAnswerDigest().matches("[0-9a-f]{64}"),
                    "知识题必须返回64位小写十六进制摘要");
            assertEquals(expected, question.getAnswerDigest(), "规范正确答案必须命中摘要");
            assertTrue(!testAnswerDigest(first.getAttemptId(), question.getQuestionId(), "wrong")
                    .equals(question.getAnswerDigest()), "错误答案不得命中摘要");
        }

        EngChallengeVo second = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        EngChallengeQuestionVo firstChoice = first.getQuestions().stream()
                .filter(item -> "WORD_TO_CN:1".equals(item.getQuestionId())).findFirst().orElseThrow();
        EngChallengeQuestionVo secondChoice = second.getQuestions().stream()
                .filter(item -> "WORD_TO_CN:1".equals(item.getQuestionId())).findFirst().orElseThrow();
        assertTrue(!firstChoice.getAnswerDigest().equals(secondChoice.getAnswerDigest()),
                "相同题目在不同 attemptId 下必须生成不同摘要");

        Harness spellingHarness = spellingHarness();
        EngChallengeVo spelling = spellingHarness.service.getChallenge("SPELLING", null, null, List.of(1L));
        EngChallengeQuestionVo spellingQuestion = spelling.getQuestions().get(0);
        String spellingAnswer = spellingAnswer(spelling, spellingQuestion);
        assertEquals(testAnswerDigest(spelling.getAttemptId(), spellingQuestion.getQuestionId(), spellingAnswer),
                spellingQuestion.getAnswerDigest(), "拼写题正确答案必须命中摘要");
        assertTrue(harness.service.checkChallengeAnswer(challengeCheck(first.getAttemptId())).getCorrect(),
                "旧单题判题接口必须继续可用于兼容回退");
    }

    /** 复习词返回拼写资格，短词和非纯英文字母词不得进入拼写测试。 */
    private static void shouldExposeAndEnforceSpellingEligibility()
    {
        Harness harness = new Harness();
        harness.progress.put(1L, progress(1L, ARTICLE_ID, 1, 1));
        harness.progress.put(2L, progress(2L, ARTICLE_ID, 1, 1));
        harness.progress.put(8L, progress(8L, ARTICLE_ID, 1, 1));
        harness.progress.put(9L, progress(9L, ARTICLE_ID, 1, 1));
        Map<Long, Boolean> eligibility = harness.service.getReviewOverview().getWords().stream()
                .collect(Collectors.toMap(EngReviewWordVo::getWordId, EngReviewWordVo::getSpellingEligible));
        assertTrue(Boolean.TRUE.equals(eligibility.get(1L)), "apple 应满足拼写条件");
        assertTrue(Boolean.FALSE.equals(eligibility.get(2L)), "cat 不足四个字母不得参加拼写测试");
        assertTrue(Boolean.TRUE.equals(eligibility.get(8L)), "四字母纯英文单词应满足拼写条件");
        assertTrue(Boolean.FALSE.equals(eligibility.get(9L)), "包含连字符的单词不得参加拼写测试");
        assertThrows(() -> harness.service.getChallenge("SPELLING", null, null, List.of(2L)),
                "短词不得绕过服务端拼写资格校验");
        assertThrows(() -> harness.service.getChallenge("SPELLING", null, null, List.of(9L)),
                "非纯英文字母词不得绕过服务端拼写资格校验");

        EngChallengeQuestionVo repeated = harness.service.getChallenge("SPELLING", null, null, List.of(8L))
                .getQuestions().get(0);
        assertTrue(repeated.getPrompt().contains("____"), "恰好四个字母时必须整词挖空");
        assertEquals(2L, repeated.getOptions().stream().filter("n"::equals).count(),
                "重复字母必须保留独立候选项");
        assertEquals(2L, repeated.getOptions().stream().filter("o"::equals).count(),
                "重复字母必须保留独立候选项");
    }

    /** 同一 attempt 的题面、答案和候选字母必须可稳定重建。 */
    private static void shouldKeepSpellingDefinitionStableWithinAttempt()
    {
        Harness harness = spellingHarness();
        EngChallengeVo challenge = harness.service.getChallenge("SPELLING", null, null, List.of(1L));
        EngChallengeQuestionVo question = challenge.getQuestions().get(0);
        String answer = spellingAnswer(challenge, question);
        EngChallengeCheckDto check = new EngChallengeCheckDto(); check.setAttemptId(challenge.getAttemptId());
        check.setMode("SPELLING"); check.setQuestionId(question.getQuestionId()); check.setAnswer(answer);
        assertTrue(harness.service.checkChallengeAnswer(check).getCorrect(), "即时判题必须按同一 attempt 重建答案");
        assertTrue(harness.service.checkChallengeAnswer(check).getCorrect(), "重复即时判题结果必须稳定");

        EngChallengeSubmitDto submit = request(challenge.getAttemptId(), "SPELLING", null, null);
        submit.setAnswers(List.of(answer(question.getQuestionId(), answer)));
        assertEquals(3, harness.service.submitChallenge(submit).getStars(), "最终提交必须复用同一四位置答案");
    }

    /** 五词首次三星共45金币，重复 attempt 和新一轮重复三星均不得再次发放。 */
    private static void shouldRewardSpellingMilestonesOnlyOnce()
    {
        Harness harness = spellingHarness();
        List<Long> ids = List.of(1L, 4L, 5L, 6L, 7L);
        EngChallengeVo challenge = harness.service.getChallenge("SPELLING", null, null, ids);
        EngChallengeSubmitDto request = request(challenge.getAttemptId(), "SPELLING", null, null);
        request.setAnswers(spellingAnswers(challenge, 5));
        EngChallengeResultVo result = harness.service.submitChallenge(request);
        assertEquals(3, result.getStars(), "五词全对必须为三星");
        assertEquals(45L, result.getMilestoneCoin(), "五词首次三星应获得45金币");
        assertEquals(0L, result.getReviewCoin(), "拼写测试不得发普通复习币");
        assertTrue(harness.levelProgress.isEmpty(), "拼写测试不得写入或解锁文章关卡进度");
        harness.service.submitChallenge(request);
        assertEquals(45L, harness.balance, "重复 attempt 不得重复发放拼写金币");

        EngChallengeVo retry = harness.service.getChallenge("SPELLING", null, null, ids);
        EngChallengeSubmitDto retryRequest = request(retry.getAttemptId(), "SPELLING", null, null);
        retryRequest.setAnswers(spellingAnswers(retry, 5));
        assertEquals(0L, harness.service.submitChallenge(retryRequest).getMilestoneCoin(),
                "新一轮重复三星也不得重复发放拼写金币");
        assertEquals(45L, harness.balance, "拼写奖励累计上限应保持45金币");
    }

    /** 五词从一星升至二星、三星时只补足10/25/45的累计差额。 */
    private static void shouldTopUpSpellingMilestonesByStar()
    {
        Harness harness = spellingHarness();
        List<Long> ids = List.of(1L, 4L, 5L, 6L, 7L);
        int[] correctCounts = {3, 4, 5};
        int[] expectedStars = {1, 2, 3};
        long[] expectedRewards = {10L, 15L, 20L};
        for (int index = 0; index < correctCounts.length; index++)
        {
            EngChallengeVo challenge = harness.service.getChallenge("SPELLING", null, null, ids);
            EngChallengeSubmitDto request = request(challenge.getAttemptId(), "SPELLING", null, null);
            request.setAnswers(spellingAnswers(challenge, correctCounts[index]));
            EngChallengeResultVo result = harness.service.submitChallenge(request);
            assertEquals(expectedStars[index], result.getStars(), "拼写整轮星级应按错题数计算");
            assertEquals(expectedRewards[index], result.getMilestoneCoin(), "升星只应补发累计金币差额");
        }
        assertEquals(45L, harness.balance, "逐级升至三星后五词累计应为45金币");
    }

    /** 跨文章已学词不再作为新词，同一首次关卡低分后仍可重试。 */
    private static void shouldExcludeWordsLearnedElsewhereButAllowFailedLevelRetry()
    {
        Harness harness = new Harness();
        harness.progress.put(2L, progress(2L, 99L, 1, 1));
        EngChallengeVo first = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        assertEquals(List.of(1L), first.getWords().stream().map(EngWordVo::getId).toList(),
                "跨文章已学词必须从新词关卡排除");

        harness.progress.put(1L, progress(1L, ARTICLE_ID, 1, 1));
        EngChallengeVo retry = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        assertEquals(List.of(1L), retry.getWords().stream().map(EngWordVo::getId).toList(),
                "首次在本关学习的词即使已标记 learned 也必须允许重试");
    }

    /** 自动掌握关可无限次测试全部单词，并沿用普通 NEW 里程碑幂等规则。 */
    private static void shouldAutoMasterLevelWhenEveryWordWasLearnedElsewhere()
    {
        Harness harness = new Harness();
        harness.progress.put(1L, progress(1L, 98L, 1, 1));
        harness.progress.put(2L, progress(2L, 99L, 1, 1));
        EngArticleLevelVo level = harness.service.getArticleLevels(ARTICLE_ID).getLevels().get(0);
        assertTrue(level.getMasteredByExistingWords(), "全部词在其他文章已学时应自动掌握");
        assertEquals(0, level.getHighestStars(), "自动掌握不得伪造星级");

        EngChallengeVo first = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        assertEquals(List.of(1L, 2L), first.getWords().stream().map(EngWordVo::getId).toList(),
                "自动掌握关必须测试本关全部单词");
        EngChallengeSubmitDto firstRequest = request(first.getAttemptId(), "NEW", ARTICLE_ID, 1);
        firstRequest.setAnswers(perfectAppleAndCatKnowledgeAnswers());
        EngChallengeResultVo firstResult = harness.service.submitChallenge(firstRequest);
        assertEquals(3, firstResult.getStars(), "自动掌握关使用普通 NEW 星级规则");
        assertEquals(12L, firstResult.getMilestoneCoin(), "两个首次三星单词应各发6枚里程碑金币");

        EngChallengeVo retry = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        assertEquals(List.of(1L, 2L), retry.getWords().stream().map(EngWordVo::getId).toList(),
                "自动掌握关达星后仍必须允许再次测试全部单词");
        EngChallengeSubmitDto retryRequest = request(retry.getAttemptId(), "NEW", ARTICLE_ID, 1);
        retryRequest.setAnswers(perfectAppleAndCatKnowledgeAnswers());
        EngChallengeResultVo retryResult = harness.service.submitChallenge(retryRequest);
        assertEquals(0L, retryResult.getMilestoneCoin(), "rewardedStars 已覆盖三星后不得重复发币");
        assertEquals(12L, harness.balance, "重复测试不得重复增加里程碑余额");
    }

    /** 句子题保留实际词形，首次三星只发 1+2+3 的累计里程碑金币且 attempt 幂等。 */
    private static void shouldUseMatchedTextAndRewardMilestonesOnlyOnce()
    {
        Harness harness = new Harness();
        harness.progress.put(2L, progress(2L, 99L, 1, 1));
        EngChallengeVo challenge = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        assertTrue(challenge.getQuestions().stream().anyMatch(item -> "SENTENCE_CHOICE:1".equals(item.getQuestionId())
                && item.getOptions().contains("apples")), "句子选择题必须使用关系保存的实际词形");

        EngChallengeSubmitDto request = request(challenge.getAttemptId(), "NEW", ARTICLE_ID, 1);
        request.setAnswers(perfectAppleAnswers());
        EngChallengeResultVo result = harness.service.submitChallenge(request);
        assertEquals(3, result.getStars(), "100 分必须得到三星");
        assertEquals(6L, result.getMilestoneCoin(), "首次三星应累计获得 6 金币");
        assertEquals(0L, result.getReviewCoin(), "新词关不得发复习金币");
        assertEquals(6L, harness.balance, "钱包应增加里程碑金币");
        assertEquals(1, harness.details.size(), "测试记录必须保存规范词明细");
        assertEquals(1, harness.detailWrites.get(), "首次提交只写一次明细");

        EngChallengeResultVo duplicate = harness.service.submitChallenge(request);
        assertEquals(6L, duplicate.getCoinReward(), "重复提交应返回原测试奖励");
        assertEquals(6L, harness.balance, "相同 attemptId 不得重复发币");
        assertEquals(1, harness.detailWrites.get(), "相同 attemptId 不得重复写明细");
    }

    /** 复习全对每轮每词固定奖励 1 金币，不受里程碑上限影响。 */
    private static void shouldRewardEveryPerfectReviewWithoutMilestoneDuplication()
    {
        Harness harness = new Harness();
        EngUserWordProgress mastered = progress(1L, ARTICLE_ID, 1, 1);
        mastered.setHighestStars(3); mastered.setLatestStars(2); mastered.setRewardedStars(3);
        mastered.setLatestTestTime(new Date(0)); harness.progress.put(1L, mastered);
        for (int round = 0; round < 2; round++)
        {
            EngChallengeVo challenge = harness.service.getChallenge("REVIEW", null, null, List.of(1L));
            EngChallengeSubmitDto request = request(challenge.getAttemptId(), "REVIEW", null, null);
            request.setAnswers(perfectAppleAnswers());
            EngChallengeResultVo result = harness.service.submitChallenge(request);
            assertEquals(0L, result.getMilestoneCoin(), "已领满三星里程碑不得重复发放");
            assertEquals(1L, result.getReviewCoin(), "每轮全对复习应发 1 金币");
        }
        assertEquals(2L, harness.balance, "复习金币不设累计上限");
        assertEquals(2, harness.details.size(), "每轮复习均需保存单词明细");
    }

    /** 复习刷新未领的三星里程碑时，应与本轮复习金币叠加。 */
    private static void shouldStackReviewAndNewMilestoneCoin()
    {
        Harness harness = new Harness();
        EngUserWordProgress learned = progress(1L, ARTICLE_ID, 1, 1);
        learned.setHighestStars(2); learned.setLatestStars(2); learned.setRewardedStars(2);
        harness.progress.put(1L, learned);
        harness.sentenceSourceArticleId = 999L;
        EngChallengeVo challenge = harness.service.getChallenge("REVIEW", null, null, List.of(1L));
        EngChallengeSubmitDto request = request(challenge.getAttemptId(), "REVIEW", null, null);
        request.setAnswers(perfectAppleAnswers());
        EngChallengeResultVo result = harness.service.submitChallenge(request);
        assertEquals(3L, result.getMilestoneCoin(), "二星升三星应补3金币");
        assertEquals(1L, result.getReviewCoin(), "复习全对应另1金币");
        assertEquals(4L, result.getCoinReward(), "两类金币应叠加");
        assertEquals(ARTICLE_ID, harness.details.get(0).getSourceArticleId(),
                "REVIEW 明细必须归属首次学习文章，不受跨文章例句影响");
        EngStudyRecord stored = harness.records.get(challenge.getAttemptId());
        assertEquals(null, stored.getArticleId(), "REVIEW 学习记录 article_id 必须为空");
    }

    /** insert ignore 发现并发冲突后必须当前读原记录，不重复执行任何副作用。 */
    private static void shouldUseCurrentReadAfterConcurrentAttemptConflict()
    {
        Harness harness = new Harness();
        harness.progress.put(2L, progress(2L, 99L, 1, 1));
        EngChallengeVo challenge = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        EngChallengeSubmitDto request = request(challenge.getAttemptId(), "NEW", ARTICLE_ID, 1);
        request.setAnswers(perfectAppleAnswers());
        harness.service.submitChallenge(request);
        long balance = harness.balance; int detailWrites = harness.detailWrites.get();
        harness.hideOrdinaryAttemptRead = true;
        harness.service.submitChallenge(request);
        assertEquals(1, harness.currentAttemptReads.get(), "冲突后应使用 select for update 当前读");
        assertEquals(balance, harness.balance, "并发重复提交不得重复发币");
        assertEquals(detailWrites, harness.detailWrites.get(), "并发重复提交不得重复写明细");
    }

    /** NEW 单词没有任何例句时，答错仍必须按当前文章记录明细和错词。 */
    private static void shouldRecordWrongNewWordWithoutExample()
    {
        Harness harness = new Harness();
        harness.progress.put(1L, progress(1L, 99L, 1, 1));
        EngChallengeVo challenge = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        EngChallengeSubmitDto request = request(challenge.getAttemptId(), "NEW", ARTICLE_ID, 1);
        request.setAnswers(List.of(answer("WORD_TO_CN:2", "错误"), answer("CN_TO_WORD:2", "cat")));
        EngChallengeResultVo result = harness.service.submitChallenge(request);
        assertEquals(0L, result.getCoinReward(), "低于一星不发金币");
        assertEquals(ARTICLE_ID, harness.details.get(0).getSourceArticleId(), "NEW 明细必须固定归属当前文章");
        assertEquals(ARTICLE_ID, harness.lastWrongArticleId, "无例句错词仍必须归属当前文章");
        assertEquals(1, harness.wrongWrites.get(), "错词应写入一次");
        assertEquals(-1, harness.familiarityDelta, "本轮未全对应扣减熟悉度");
    }

    /** 明细 Mapper 必须通过学习记录归属限定当前用户。 */
    private static void shouldKeepRecordWordOwnershipInMapperSql() throws Exception
    {
        try (var input = EngStudyServiceImplTest.class.getClassLoader()
                .getResourceAsStream("mapper/eng/EngStudyRecordWordMapper.xml"))
        {
            String xml = new String(Objects.requireNonNull(input).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            assertTrue(xml.contains("join eng_study_record r on r.id=d.study_record_id"), "明细查询必须联结学习记录");
            assertTrue(xml.contains("r.user_id=#{userid}"), "明细查询必须限定当前用户");
        }
    }

    /** 幂等冲突后的 Mapper 查询必须是 select for update 当前读。 */
    private static void shouldKeepAttemptCurrentReadInMapperSql() throws Exception
    {
        try (var input = EngStudyServiceImplTest.class.getClassLoader()
                .getResourceAsStream("mapper/eng/EngStudyRecordMapper.xml"))
        {
            String xml = new String(Objects.requireNonNull(input).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            assertTrue(xml.contains("selectbyuserandattemptforupdate"), "必须保留并发冲突当前读 Mapper");
            assertTrue(xml.contains("attempt_id=#{attemptid} limit 1 for update"), "幂等记录当前读必须加锁");
        }
    }

    /** NEW 首次写关卡进度前必须先锁定与文章词追加相同的 article 行。 */
    private static void shouldLockArticleBeforeFirstLevelProgressWrite()
    {
        Harness harness = new Harness();
        harness.progress.put(2L, progress(2L, 99L, 1, 1));
        EngChallengeVo challenge = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        harness.lockOrder.clear();
        EngChallengeSubmitDto request = request(challenge.getAttemptId(), "NEW", ARTICLE_ID, 1);
        request.setAnswers(perfectAppleAnswers());
        harness.service.submitChallenge(request);
        int firstLock = harness.lockOrder.indexOf("articleLock");
        int progressWrite = harness.lockOrder.indexOf("levelProgress");
        assertTrue(firstLock >= 0 && progressWrite > firstLock, "文章行锁必须先于首次关卡进度写入");
        try
        {
            assertTrue(EngStudyServiceImpl.class.getMethod("submitChallenge", EngChallengeSubmitDto.class)
                    .isAnnotationPresent(Transactional.class), "文章锁和关卡进度必须共享提交事务");
        }
        catch (ReflectiveOperationException exception)
        {
            throw new AssertionError("事务结构检查失败", exception);
        }
    }

    /** 下一关导航必须包含已自动掌握但仍可正式测试的关卡。 */
    private static void shouldReturnOnlyNextEffectiveLevel()
    {
        Harness harness = new Harness();
        harness.levelWords.clear();
        harness.levelWords.put(1, List.of("apple"));
        harness.levelWords.put(2, List.of("cat"));
        harness.levelWords.put(3, List.of("dog"));
        harness.progress.put(2L, progress(2L, 99L, 1, 1));
        EngChallengeVo challenge = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        EngChallengeSubmitDto request = request(challenge.getAttemptId(), "NEW", ARTICLE_ID, 1);
        request.setAnswers(perfectAppleAnswers());
        EngChallengeResultVo result = harness.service.submitChallenge(request);
        assertEquals(2, result.getNextLevelNo(), "自动掌握的第2关仍应作为下一可测试关返回");
        assertTrue(result.getNextLevelUnlocked(), "存在下一自动掌握关时应允许继续");
    }

    /** 末关通关后不得仅因为有星就返回可解锁。 */
    private static void shouldKeepNextLevelEmptyAtTerminalLevel()
    {
        Harness harness = new Harness();
        harness.progress.put(2L, progress(2L, 99L, 1, 1));
        EngChallengeVo challenge = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        EngChallengeSubmitDto request = request(challenge.getAttemptId(), "NEW", ARTICLE_ID, 1);
        request.setAnswers(perfectAppleAnswers());
        EngChallengeResultVo result = harness.service.submitChallenge(request);
        assertEquals(null, result.getNextLevelNo(), "末关不得返回虚假下一关");
        assertTrue(!result.getNextLevelUnlocked(), "末关 nextLevelUnlocked 必须为 false");
    }

    /** 迁移应保护历史明细，并按旧成绩幂等回填星级而不补发金币。 */
    private static void shouldKeepHistoricalMigrationConsistent() throws Exception
    {
        Path migration = Path.of("sql/1.0.25_eng_global_word_challenge.sql");
        if (!Files.exists(migration)) migration = Path.of("../sql/1.0.25_eng_global_word_challenge.sql");
        String sql = Files.readString(migration, StandardCharsets.UTF_8).replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("foreign key(word_id) references eng_word(id) on delete restrict"),
                "历史学习明细到词条的外键必须禁止级联删除");
        int start = sql.indexOf("update eng_study_record set stars=case");
        int end = sql.indexOf("insert ignore into eng_article_level_progress", start);
        assertTrue(start >= 0 && end > start, "迁移必须包含历史记录星级回填");
        String backfill = sql.substring(start, end);
        assertTrue(backfill.contains("score=100 then 3") && backfill.contains("score>=90 then 2")
                && backfill.contains("score>=80 then 1") && backfill.contains("else 0"),
                "历史星级回填必须使用正式阈值");
        assertTrue(backfill.contains("attempt_id is null") && backfill.contains("stars<>case"),
                "历史星级回填必须限定旧记录并保持幂等");
        assertTrue(!backfill.contains("milestone_coin") && !backfill.contains("review_coin")
                && !backfill.contains("coin_reward"), "历史回填不得补发金币");
    }

    private static void shouldExposeTransactionalMutationBoundary() throws Exception
    {
        assertTrue(EngStudyServiceImpl.class.getMethod("submitChallenge", EngChallengeSubmitDto.class)
                .isAnnotationPresent(Transactional.class), "提交入口必须由事务包裹");
        assertTrue(EngStudyServiceImpl.class.getMethod("submitChallenge", EngChallengeSubmitDto.class, boolean.class)
                .isAnnotationPresent(Transactional.class), "访问环境感知的提交入口必须由事务包裹");
        assertTrue(EngStudyServiceImpl.class.getMethod("getChallenge", String.class, Long.class, Integer.class, List.class)
                .isAnnotationPresent(Transactional.class), "自动掌握关卡写入必须处于事务边界");
        assertTrue(EngStudyServiceImpl.class.getMethod("getChallenge", String.class, Long.class, Integer.class,
                List.class, boolean.class).isAnnotationPresent(Transactional.class),
                "访问环境感知的挑战入口必须由事务包裹");
    }

    /** 59 分不合格、60 分合格，并按知识 90% 与跟读 10% 四舍五入计分。 */
    private static void shouldApplyPronunciationBoundaryAndWeightedScore()
    {
        Harness failed = new Harness(true); failed.progress.put(2L, progress(2L, 99L, 1, 1));
        EngChallengeVo failedChallenge = failed.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        assertTrue(failedChallenge.getQuestions().stream().anyMatch(item -> "PRONUNCIATION:1".equals(item.getQuestionId())),
                "启用且配置完整时每个单词必须生成跟读题");
        failed.assessment = assessment(59, 59D, 0);
        failed.service.assessPronunciation(assessmentRequest(failedChallenge.getAttemptId(), "NEW", ARTICLE_ID, 1,
                "PRONUNCIATION:1", wav(false)));
        String cachedPayload = failed.pronunciationCache.resultPayload();
        assertTrue(cachedPayload.contains("\"score\"") && cachedPayload.contains("\"accuracy\"")
                && cachedPayload.contains("\"matchTag\"") && !cachedPayload.contains("phones"),
                "Redis 只能保留最终计分字段和归属信息");
        assertTrue(failed.pronunciationCache.serializationRoundTrips > 0,
                "Lua 参数与缓存字符串必须按项目 FastJson WriteClassName 语义往返");
        EngChallengeSubmitDto failedRequest = request(failedChallenge.getAttemptId(), "NEW", ARTICLE_ID, 1);
        failedRequest.setAnswers(withPronunciation(perfectAppleAnswers()));
        EngChallengeResultVo failedResult = failed.service.submitChallenge(failedRequest);
        assertEquals(90, failedResult.getScore(), "知识全对但59分跟读失败时总分应为90");
        assertEquals(0, failedResult.getPronunciationPassedCount(), "59分不得计为跟读合格");
        assertTrue(!failed.details.get(0).getAllCorrect().equals(1), "跟读失败时单词不得标记全部正确");

        Harness passed = new Harness(true); passed.progress.put(2L, progress(2L, 99L, 1, 1));
        EngChallengeVo challenge = passed.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        passed.assessment = assessment(60, 60D, 0);
        passed.service.assessPronunciation(assessmentRequest(challenge.getAttemptId(), "NEW", ARTICLE_ID, 1,
                "PRONUNCIATION:1", wav(false)));
        EngChallengeSubmitDto request = request(challenge.getAttemptId(), "NEW", ARTICLE_ID, 1);
        List<EngChallengeAnswerDto> answers = new ArrayList<>(withPronunciation(perfectAppleAnswers()));
        answers.set(0, answer("WORD_TO_CN:1", "错误")); request.setAnswers(answers);
        EngChallengeResultVo result = passed.service.submitChallenge(request);
        assertEquals(70, result.getScore(), "移除普通拼写题后知识答对三分之二，跟读合格总分应为70");
        assertEquals(1, result.getPronunciationPassedCount(), "60分且匹配必须计为合格");
        assertEquals(60, result.getPronunciationAverageScore(), "应返回跟读平均总分");

        Harness perfect = new Harness(true); perfect.progress.put(2L, progress(2L, 99L, 1, 1));
        EngChallengeVo perfectChallenge = perfect.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        perfect.assessment = assessment(100, 100D, 0);
        perfect.service.assessPronunciation(assessmentRequest(perfectChallenge.getAttemptId(), "NEW", ARTICLE_ID, 1,
                "PRONUNCIATION:1", wav(false)));
        EngChallengeSubmitDto perfectRequest = request(perfectChallenge.getAttemptId(), "NEW", ARTICLE_ID, 1);
        perfectRequest.setAnswers(withPronunciation(perfectAppleAnswers()));
        assertEquals(100, perfect.service.submitChallenge(perfectRequest).getScore(), "知识和跟读全部合格应为100分");
    }

    /** 客户端标记不能替代 Redis 可信结果，也不能跨 attempt 复用。 */
    private static void shouldRejectMissingOrCrossAttemptAssessment()
    {
        Harness harness = new Harness(true); harness.progress.put(2L, progress(2L, 99L, 1, 1));
        EngChallengeVo challenge = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        EngChallengeSubmitDto missing = request(challenge.getAttemptId(), "NEW", ARTICLE_ID, 1);
        missing.setAnswers(withPronunciation(perfectAppleAnswers()));
        assertThrows(() -> harness.service.submitChallenge(missing), "无可信缓存时不得提交跟读题");
        harness.service.assessPronunciation(assessmentRequest(challenge.getAttemptId(), "NEW", ARTICLE_ID, 1,
                "PRONUNCIATION:1", wav(false)));
        EngChallengeSubmitDto cross = request("another-attempt", "NEW", ARTICLE_ID, 1);
        cross.setAnswers(withPronunciation(perfectAppleAnswers()));
        assertThrows(() -> harness.service.submitChallenge(cross), "可信评分不得跨 attempt 复用");
    }

    /** WAV 必须具备规范参数、有效时长和非静音 PCM 数据。 */
    private static void shouldRejectInvalidOrSilentWav()
    {
        Harness harness = new Harness(true); harness.progress.put(2L, progress(2L, 99L, 1, 1));
        EngChallengeVo challenge = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        assertThrows(() -> harness.service.assessPronunciation(assessmentRequest(challenge.getAttemptId(), "NEW",
                ARTICLE_ID, 1, "PRONUNCIATION:1", new byte[] {1, 2, 3})), "错误 WAV 必须拒绝");
        assertThrows(() -> harness.service.assessPronunciation(assessmentRequest(challenge.getAttemptId(), "NEW",
                ARTICLE_ID, 1, "PRONUNCIATION:1", wav(true))), "静音录音必须拒绝");
        assertThrows(() -> harness.service.assessPronunciation(assessmentRequest(challenge.getAttemptId(), "NEW",
                ARTICLE_ID, 1, "PRONUNCIATION:1", wav(16000, 1, 16, 5100, false))), "超长录音必须拒绝");
        assertThrows(() -> harness.service.assessPronunciation(assessmentRequest(challenge.getAttemptId(), "NEW",
                ARTICLE_ID, 1, "PRONUNCIATION:1", new byte[256 * 1024 + 1])), "超大录音必须拒绝");
        assertThrows(() -> harness.service.assessPronunciation(assessmentRequest(challenge.getAttemptId(), "NEW",
                ARTICLE_ID, 1, "PRONUNCIATION:1", wav(8000, 1, 16, 500, false))), "8kHz录音必须拒绝");
        assertThrows(() -> harness.service.assessPronunciation(assessmentRequest(challenge.getAttemptId(), "NEW",
                ARTICLE_ID, 1, "PRONUNCIATION:1", wav(16000, 2, 16, 500, false))), "双声道录音必须拒绝");
        assertThrows(() -> harness.service.assessPronunciation(assessmentRequest(challenge.getAttemptId(), "NEW",
                ARTICLE_ID, 1, "PRONUNCIATION:1", wav(16000, 1, 8, 500, false))), "非16bit录音必须拒绝");
        assertThrows(() -> harness.service.assessPronunciation(assessmentRequest(challenge.getAttemptId(), "NEW",
                ARTICLE_ID, 1, "PRONUNCIATION:1", wav(16000, 1, 16, 200, false))), "过短录音必须拒绝");
    }

    /** 复习知识题全对但跟读失败时最高二星、无复习币并进入错词状态。 */
    private static void shouldTreatFailedReviewPronunciationAsNotMastered()
    {
        Harness harness = new Harness(true); EngUserWordProgress learned = progress(1L, ARTICLE_ID, 1, 1);
        learned.setHighestStars(3); learned.setRewardedStars(3); harness.progress.put(1L, learned);
        EngChallengeVo challenge = harness.service.getChallenge("REVIEW", null, null, List.of(1L));
        harness.assessment = assessment(100, 100D, 3);
        harness.service.assessPronunciation(assessmentRequest(challenge.getAttemptId(), "REVIEW", null, null,
                "PRONUNCIATION:1", wav(false)));
        EngChallengeSubmitDto request = request(challenge.getAttemptId(), "REVIEW", null, null);
        request.setAnswers(withPronunciation(perfectAppleAnswers()));
        EngChallengeResultVo result = harness.service.submitChallenge(request);
        assertEquals(2, result.getWordResults().get(0).getStars(), "复习跟读失败时单词最高二星");
        assertEquals(0L, result.getReviewCoin(), "复习跟读失败不得发全部正确金币");
        assertEquals(1, harness.wrongWrites.get(), "复习跟读失败应进入错词状态");
    }

    /** 两次重录并发时，只允许后到请求保存可信结果，慢返回的旧请求必须失效。 */
    private static void shouldKeepOnlyLatestConcurrentRecording() throws Exception
    {
        Harness harness = new Harness(true); harness.progress.put(2L, progress(2L, 99L, 1, 1));
        EngChallengeVo challenge = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        CountDownLatch firstStarted = new CountDownLatch(1), releaseFirst = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        harness.assessmentFunction = ignored -> {
            if (calls.incrementAndGet() == 1)
            {
                firstStarted.countDown();
                try { releaseFirst.await(); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
                return assessment(59, 59D, 0);
            }
            return assessment(100, 100D, 0);
        };
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try
        {
            Future<?> older = executor.submit(() -> assessAsUser(harness, challenge, USER_ID));
            firstStarted.await();
            Future<?> newer = executor.submit(() -> assessAsUser(harness, challenge, USER_ID));
            newer.get(); releaseFirst.countDown();
            try
            {
                older.get(); throw new AssertionError("旧录音结果必须因代次失效");
            }
            catch (ExecutionException exception)
            {
                assertTrue(exception.getCause() instanceof ServiceException
                        && exception.getCause().getMessage().contains("已过期"), "旧录音应返回明确过期提示");
            }
        }
        finally
        {
            releaseFirst.countDown(); executor.shutdownNow();
        }
        EngChallengeSubmitDto request = request(challenge.getAttemptId(), "NEW", ARTICLE_ID, 1);
        request.setAnswers(withPronunciation(perfectAppleAnswers()));
        EngChallengeResultVo result = harness.service.submitChallenge(request);
        assertEquals(100, result.getPronunciationAverageScore(), "最终只能采用后到重录的100分结果");
        assertTrue(harness.pronunciationCache.rejectedSaves > 0, "Lua CAS 必须拒绝旧代次保存");
    }

    /** 同一轮测试的每个单词最多三次，且单词和测试之间相互隔离。 */
    private static void shouldLimitPronunciationAttemptsPerWord()
    {
        Harness harness = new Harness(true);
        EngChallengeVo challenge = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        for (int attempt = 1; attempt <= 3; attempt++)
        {
            harness.assessment = assessment(70 + attempt, 70D + attempt, 0);
            EngPronunciationAssessmentVo result = harness.service.assessPronunciation(assessmentRequest(
                    challenge.getAttemptId(), "NEW", ARTICLE_ID, 1, "PRONUNCIATION:1", wav(false)));
            assertEquals(attempt, result.getAttemptCount(), "应返回已消耗测评次数");
            assertEquals(3 - attempt, result.getRemainingAttempts(), "应返回剩余测评次数");
        }
        assertThrows(() -> harness.service.assessPronunciation(assessmentRequest(challenge.getAttemptId(), "NEW",
                ARTICLE_ID, 1, "PRONUNCIATION:1", wav(false))), "第四次测评必须拒绝");
        EngPronunciationAssessmentVo anotherWord = harness.service.assessPronunciation(assessmentRequest(
                challenge.getAttemptId(), "NEW", ARTICLE_ID, 1, "PRONUNCIATION:2", wav(false)));
        assertEquals(1, anotherWord.getAttemptCount(), "不同单词必须独立计次");
        EngChallengeSubmitDto submit = request(challenge.getAttemptId(), "NEW", ARTICLE_ID, 1);
        submit.setAnswers(perfectAppleAndCatAnswers());
        EngChallengeResultVo submitted = harness.service.submitChallenge(submit);
        assertEquals(73, submitted.getWordResults().stream().filter(item -> item.getWordId() == 1L)
                .findFirst().orElseThrow().getPronunciationScore(), "超限请求不得删除第三次可信结果");

        EngChallengeVo anotherChallenge = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        EngPronunciationAssessmentVo anotherAttempt = harness.service.assessPronunciation(assessmentRequest(
                anotherChallenge.getAttemptId(), "NEW", ARTICLE_ID, 1, "PRONUNCIATION:1", wav(false)));
        assertEquals(1, anotherAttempt.getAttemptCount(), "不同测试必须独立计次");
    }

    /** 前置校验失败不预占，第三方异常或无效响应必须释放已预占次数。 */
    private static void shouldReleaseFailedPronunciationReservation()
    {
        Harness harness = new Harness(true);
        EngChallengeVo challenge = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        for (int index = 0; index < 4; index++)
            assertThrows(() -> harness.service.assessPronunciation(assessmentRequest(challenge.getAttemptId(), "NEW",
                    ARTICLE_ID, 1, "PRONUNCIATION:1", new byte[] {1, 2, 3})), "WAV 校验失败不得计次");
        harness.assessmentFunction = ignored -> { throw new ServiceException("跟读评分服务繁忙，请稍后重试"); };
        for (int index = 0; index < 4; index++)
            assertThrows(() -> assessAsUser(harness, challenge, USER_ID), "第三方异常必须释放次数");
        harness.assessmentFunction = ignored -> null;
        assertThrows(() -> assessAsUser(harness, challenge, USER_ID), "无效第三方响应必须释放次数");
        harness.assessmentFunction = ignored -> assessment(100, 100D, 0);
        for (int attempt = 1; attempt <= 3; attempt++)
        {
            EngPronunciationAssessmentVo result = harness.service.assessPronunciation(assessmentRequest(
                    challenge.getAttemptId(), "NEW", ARTICLE_ID, 1, "PRONUNCIATION:1", wav(false)));
            assertEquals(attempt, result.getAttemptCount(), "失败释放后应从正确次数继续");
        }
    }

    /** 释放预占失败时保留原始评分异常，释放异常只作为 suppressed 诊断。 */
    private static void shouldPreserveOriginalErrorWhenReservationReleaseFails()
    {
        Harness clientFailure = new Harness(true);
        EngChallengeVo clientChallenge = clientFailure.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        clientFailure.pronunciationCache.failRelease = true;
        clientFailure.assessmentFunction = ignored -> { throw new ServiceException("原始腾讯异常"); };
        try
        {
            assessAsUser(clientFailure, clientChallenge, USER_ID);
            throw new AssertionError("第三方异常必须抛出");
        }
        catch (ServiceException exception)
        {
            assertEquals("原始腾讯异常", exception.getMessage(), "释放失败不得覆盖原始腾讯异常");
            assertEquals(1, exception.getSuppressed().length, "释放失败应附加为 suppressed");
            assertEquals("模拟释放失败", exception.getSuppressed()[0].getMessage(), "suppressed 应保留释放失败原因");
        }

        Harness invalidResult = new Harness(true);
        EngChallengeVo invalidChallenge = invalidResult.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        invalidResult.pronunciationCache.failRelease = true;
        invalidResult.assessmentFunction = ignored -> null;
        try
        {
            assessAsUser(invalidResult, invalidChallenge, USER_ID);
            throw new AssertionError("无效结果必须抛出");
        }
        catch (ServiceException exception)
        {
            assertEquals("未取得有效跟读评分，请重试", exception.getMessage(), "释放失败不得覆盖无效结果异常");
            assertEquals(1, exception.getSuppressed().length, "无效结果的释放失败应附加为 suppressed");
        }
    }

    /** 并发请求也只能有三个进入第三方测评。 */
    private static void shouldEnforceConcurrentPronunciationAttemptLimit() throws Exception
    {
        Harness harness = new Harness(true);
        EngChallengeVo challenge = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        CountDownLatch started = new CountDownLatch(3), release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        harness.assessmentFunction = ignored -> {
            calls.incrementAndGet(); started.countDown();
            try { release.await(); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
            return assessment(100, 100D, 0);
        };
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try
        {
            List<Future<?>> accepted = new ArrayList<>();
            for (int index = 0; index < 3; index++)
                accepted.add(executor.submit(() -> assessAsUser(harness, challenge, USER_ID)));
            started.await();
            Future<?> rejected = executor.submit(() -> assessAsUser(harness, challenge, USER_ID));
            try
            {
                rejected.get(); throw new AssertionError("第四个并发测评必须拒绝");
            }
            catch (ExecutionException exception)
            {
                assertTrue(exception.getCause() instanceof ServiceException
                        && exception.getCause().getMessage().contains("最多测评3次"),
                        "超限并发请求应返回明确提示");
            }
            release.countDown();
            for (Future<?> future : accepted)
            {
                try { future.get(); }
                catch (ExecutionException exception)
                {
                    assertTrue(exception.getCause() instanceof ServiceException
                            && exception.getCause().getMessage().contains("已过期"),
                            "旧代次评分仍应被 CAS 拒绝");
                }
            }
        }
        finally
        {
            release.countDown(); executor.shutdownNow();
        }
        assertEquals(3, calls.get(), "超限请求不得进入第三方测评");
        assertThrows(() -> assessAsUser(harness, challenge, USER_ID), "并发成功预占的三次均应消耗次数");
    }

    /** 可信结果过期、跨用户或改变测试模式时均不得复用。 */
    private static void shouldIsolateExpiredUserAndTestCaches()
    {
        Harness expired = assessedHarness();
        EngChallengeVo expiredChallenge = expired.lastChallenge;
        expired.pronunciationCache.removeResults();
        assertThrows(() -> expired.service.submitChallenge(pronunciationSubmit(expiredChallenge, "NEW", ARTICLE_ID, 1)),
                "过期可信结果不得提交");

        Harness ttl = assessedHarness();
        assertTrue(ttl.pronunciationCache.ttls.stream().allMatch(value -> value == 1800), "代次与结果 TTL 必须为1800秒");
        ttl.pronunciationCache.advanceSeconds(1801);
        assertEquals(0, ttl.pronunciationCache.activeSize(), "结果与代次均应在30分钟后到期");

        Harness crossUser = assessedHarness(); EngChallengeVo userChallenge = crossUser.lastChallenge;
        setTestLoginUser(8L);
        try
        {
            assertThrows(() -> crossUser.service.submitChallenge(pronunciationSubmit(userChallenge, "NEW", ARTICLE_ID, 1)),
                    "可信结果不得跨用户复用");
        }
        finally { setTestLoginUser(); }

        Harness crossTest = assessedHarness(); EngChallengeVo testChallenge = crossTest.lastChallenge;
        crossTest.progress.put(1L, progress(1L, ARTICLE_ID, 1, 1));
        assertThrows(() -> crossTest.service.submitChallenge(pronunciationSubmit(testChallenge, "REVIEW", null, null)),
                "可信结果不得跨测试模式复用");
    }

    /** 第三方失败时不生成可信缓存，用户可以安全重试而不会被记零分。 */
    private static void shouldConvergeAssessmentClientFailureWithoutCache()
    {
        Harness harness = new Harness(true); harness.progress.put(2L, progress(2L, 99L, 1, 1));
        EngChallengeVo challenge = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        harness.assessmentFunction = ignored -> { throw new ServiceException("跟读评分服务繁忙，请稍后重试"); };
        assertThrows(() -> assessAsUser(harness, challenge, USER_ID), "第三方异常必须收敛为业务异常");
        assertThrows(() -> harness.service.submitChallenge(pronunciationSubmit(challenge, "NEW", ARTICLE_ID, 1)),
                "第三方失败后不得残留可信评分");

        Harness timeout = new Harness(true); timeout.progress.put(2L, progress(2L, 99L, 1, 1));
        EngChallengeVo timeoutChallenge = timeout.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        timeout.assessmentFunction = ignored -> { throw new ServiceException("跟读评分服务繁忙，请稍后重试"); };
        assertThrows(() -> assessAsUser(timeout, timeoutChallenge, USER_ID), "超时式业务异常必须允许重试");
        assertEquals("", timeout.pronunciationCache.resultPayload(), "超时式异常不得留下评分结果");
    }

    /** 重复提交从持久化明细恢复跟读汇总和单词结果，不再依赖已过期缓存。 */
    private static void shouldRestorePersistedPronunciationOnDuplicateSubmit()
    {
        Harness harness = assessedHarness(); EngChallengeVo challenge = harness.lastChallenge;
        EngChallengeSubmitDto request = pronunciationSubmit(challenge, "NEW", ARTICLE_ID, 1);
        EngChallengeResultVo first = harness.service.submitChallenge(request);
        harness.pronunciationCache.removeResults();
        EngChallengeResultVo duplicate = harness.service.submitChallenge(request);
        assertEquals(first.getPronunciationAverageScore(), duplicate.getPronunciationAverageScore(), "重复提交应恢复跟读平均分");
        assertEquals(first.getPronunciationPassedCount(), duplicate.getPronunciationPassedCount(), "重复提交应恢复跟读合格数");
        assertEquals(first.getWordResults().get(0).getPronunciationScore(),
                duplicate.getWordResults().get(0).getPronunciationScore(), "重复提交应恢复单词跟读分");
    }

    /** Servlet 对业务允许的录音保留在内存，同时保留原有全局上传上限。 */
    private static void shouldKeepPronunciationMultipartInMemory() throws Exception
    {
        Path config = Path.of("betta-admin/src/main/resources/application.yml");
        if (!Files.exists(config)) config = Path.of("../betta-admin/src/main/resources/application.yml");
        String yaml = Files.readString(config, StandardCharsets.UTF_8);
        assertTrue(yaml.contains("file-size-threshold: 256KB"), "multipart 阈值必须覆盖合法跟读录音");
        assertTrue(yaml.contains("max-file-size: 10MB") && yaml.contains("max-request-size: 20MB"),
                "不得改变现有全局文件与请求上限");
    }

    /** 伪造、跨题题目标识和空音频必须在调用评分客户端前拒绝。 */
    private static void shouldRejectForgedQuestionAndEmptyAudio()
    {
        Harness harness = new Harness(true); harness.progress.put(2L, progress(2L, 99L, 1, 1));
        EngChallengeVo challenge = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        assertThrows(() -> harness.service.assessPronunciation(assessmentRequest(challenge.getAttemptId(), "NEW",
                ARTICLE_ID, 1, "PRONUNCIATION:999", wav(false))), "伪造跟读题必须拒绝");
        assertThrows(() -> harness.service.assessPronunciation(assessmentRequest(challenge.getAttemptId(), "NEW",
                ARTICLE_ID, 1, "PRONUNCIATION:2", wav(false))), "其他单词的跟读题不得跨题复用");
        assertThrows(() -> harness.service.assessPronunciation(assessmentRequest(challenge.getAttemptId(), "NEW",
                ARTICLE_ID, 1, "WORD_TO_CN:1", wav(false))), "知识题不得复用跟读接口");
        assertThrows(() -> harness.service.assessPronunciation(assessmentRequest(challenge.getAttemptId(), "NEW",
                ARTICLE_ID, 1, "PRONUNCIATION:1", new byte[0])), "空音频必须拒绝");
    }

    /** 同模式下缓存也必须绑定文章和关卡，不能换测试范围后复用。 */
    private static void shouldRejectSameModeCrossArticleAndLevel()
    {
        Harness article = assessedHarness();
        assertThrows(() -> article.service.submitChallenge(pronunciationSubmit(article.lastChallenge, "NEW", 11L, 1)),
                "同模式跟读评分不得跨文章复用");

        Harness level = new Harness(true); EngUserWordProgress learned = progress(1L, ARTICLE_ID, 1, 1);
        level.progress.put(1L, learned);
        EngChallengeVo challenge = level.service.getChallenge("REVIEW", null, null, List.of(1L));
        level.service.assessPronunciation(assessmentRequest(challenge.getAttemptId(), "REVIEW", null, null,
                "PRONUNCIATION:1", wav(false)));
        assertThrows(() -> level.service.submitChallenge(pronunciationSubmit(challenge, "REVIEW", null, 1)),
                "同模式跟读评分不得跨关卡参数复用");
    }

    /** 多词跟读应分别计分，并正确处理全部合格和部分合格。 */
    private static void shouldScoreMultiplePronunciationsIndependently()
    {
        Harness all = new Harness(true); EngChallengeVo allChallenge = all.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        all.assessment = assessment(100, 100D, 0);
        all.service.assessPronunciation(assessmentRequest(allChallenge.getAttemptId(), "NEW", ARTICLE_ID, 1,
                "PRONUNCIATION:1", wav(false)));
        all.assessment = assessment(60, 60D, 0);
        all.service.assessPronunciation(assessmentRequest(allChallenge.getAttemptId(), "NEW", ARTICLE_ID, 1,
                "PRONUNCIATION:2", wav(false)));
        EngChallengeSubmitDto allRequest = request(allChallenge.getAttemptId(), "NEW", ARTICLE_ID, 1);
        allRequest.setAnswers(perfectAppleAndCatAnswers());
        EngChallengeResultVo allResult = all.service.submitChallenge(allRequest);
        assertEquals(2, allResult.getPronunciationPassedCount(), "两词均达标时都应计为合格");
        assertEquals(80, allResult.getPronunciationAverageScore(), "多词平均跟读分应正确计算");
        assertEquals(100, allResult.getScore(), "知识全对且跟读全部合格应为100分");

        Harness partial = new Harness(true); EngChallengeVo partialChallenge = partial.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        partial.assessment = assessment(100, 100D, 0);
        partial.service.assessPronunciation(assessmentRequest(partialChallenge.getAttemptId(), "NEW", ARTICLE_ID, 1,
                "PRONUNCIATION:1", wav(false)));
        partial.assessment = assessment(100, 100D, 3);
        partial.service.assessPronunciation(assessmentRequest(partialChallenge.getAttemptId(), "NEW", ARTICLE_ID, 1,
                "PRONUNCIATION:2", wav(false)));
        EngChallengeSubmitDto partialRequest = request(partialChallenge.getAttemptId(), "NEW", ARTICLE_ID, 1);
        partialRequest.setAnswers(perfectAppleAndCatAnswers());
        EngChallengeResultVo partialResult = partial.service.submitChallenge(partialRequest);
        assertEquals(1, partialResult.getPronunciationPassedCount(), "部分合格时只计匹配且达标的单词");
        assertEquals(95, partialResult.getScore(), "知识全对且二分之一跟读合格应为95分");
        assertEquals(List.of(true, false), partialResult.getWordResults().stream()
                .map(EngChallengeWordResultVo::getPronunciationPassed).toList(), "单词级跟读状态必须分别保存");
    }

    private static EngChallengeSubmitDto request(String attemptId, String mode, Long articleId, Integer levelNo)
    {
        EngChallengeSubmitDto request = new EngChallengeSubmitDto(); request.setAttemptId(attemptId);
        request.setMode(mode); request.setArticleId(articleId); request.setLevelNo(levelNo); return request;
    }

    private static List<EngChallengeAnswerDto> perfectAppleAnswers()
    {
        return List.of(answer("WORD_TO_CN:1", "苹果"), answer("CN_TO_WORD:1", "apple"),
                answer("SENTENCE_CHOICE:1", "apples"));
    }

    private static Harness spellingHarness()
    {
        Harness harness = new Harness();
        for (Long id : List.of(1L, 4L, 5L, 6L, 7L))
            harness.progress.put(id, progress(id, ARTICLE_ID, 1, 1));
        return harness;
    }

    private static List<EngChallengeAnswerDto> spellingAnswers(EngChallengeVo challenge, int correctCount)
    {
        List<EngChallengeQuestionVo> questions = challenge.getQuestions().stream()
                .sorted(Comparator.comparing(EngChallengeQuestionVo::getQuestionId)).toList();
        List<EngChallengeAnswerDto> result = new ArrayList<>();
        for (int index = 0; index < questions.size(); index++)
        {
            EngChallengeQuestionVo question = questions.get(index);
            result.add(answer(question.getQuestionId(), index < correctCount
                    ? spellingAnswer(challenge, question) : "zzzz"));
        }
        return result;
    }

    private static String spellingAnswer(EngChallengeVo challenge, EngChallengeQuestionVo question)
    {
        Long wordId = Long.valueOf(question.getQuestionId().substring("SENTENCE_FILL:".length()));
        String word = challenge.getWords().stream().filter(item -> wordId.equals(item.getId()))
                .findFirst().orElseThrow().getWordName().toLowerCase(Locale.ROOT);
        int start = question.getPrompt().indexOf('“') + 1;
        int end = question.getPrompt().indexOf('”', start);
        String masked = question.getPrompt().substring(start, end);
        StringBuilder answer = new StringBuilder();
        for (int index = 0; index < masked.length(); index++)
            if (masked.charAt(index) == '_') answer.append(word.charAt(index));
        return answer.toString();
    }

    private static List<EngChallengeAnswerDto> withPronunciation(List<EngChallengeAnswerDto> knowledge)
    {
        List<EngChallengeAnswerDto> result = new ArrayList<>(knowledge);
        result.add(answer("PRONUNCIATION:1", "ASSESSED")); return result;
    }

    private static List<EngChallengeAnswerDto> perfectAppleAndCatAnswers()
    {
        List<EngChallengeAnswerDto> result = new ArrayList<>(perfectAppleAnswers());
        result.add(answer("PRONUNCIATION:1", "ASSESSED"));
        result.add(answer("WORD_TO_CN:2", "猫")); result.add(answer("CN_TO_WORD:2", "cat"));
        result.add(answer("PRONUNCIATION:2", "ASSESSED")); return result;
    }

    private static List<EngChallengeAnswerDto> perfectAppleAndCatKnowledgeAnswers()
    {
        List<EngChallengeAnswerDto> result = new ArrayList<>(perfectAppleAnswers());
        result.add(answer("WORD_TO_CN:2", "猫")); result.add(answer("CN_TO_WORD:2", "cat")); return result;
    }

    private static EngPronunciationAssessmentVo assessment(int score, double accuracy, int matchTag)
    {
        EngPronunciationAssessmentVo result = new EngPronunciationAssessmentVo(); result.setScore(score);
        result.setAccuracy(accuracy); result.setFluency(90D); result.setCompleteness(100D);
        result.setMatchTag(matchTag); result.setMatched(matchTag == 0); result.setPassed(matchTag == 0 && accuracy >= 60D);
        result.setPhones(List.of()); return result;
    }

    private static Harness assessedHarness()
    {
        Harness harness = new Harness(true); harness.progress.put(2L, progress(2L, 99L, 1, 1));
        harness.lastChallenge = harness.service.getChallenge("NEW", ARTICLE_ID, 1, null);
        assessAsUser(harness, harness.lastChallenge, USER_ID); return harness;
    }

    private static void assessAsUser(Harness harness, EngChallengeVo challenge, long userId)
    {
        var previous = SecurityContextHolder.getContext().getAuthentication();
        setTestLoginUser(userId);
        try
        {
            harness.service.assessPronunciation(assessmentRequest(challenge.getAttemptId(), "NEW", ARTICLE_ID, 1,
                    "PRONUNCIATION:1", wav(false)));
        }
        finally
        {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }

    private static EngChallengeSubmitDto pronunciationSubmit(EngChallengeVo challenge, String mode,
            Long articleId, Integer levelNo)
    {
        EngChallengeSubmitDto request = request(challenge.getAttemptId(), mode, articleId, levelNo);
        request.setAnswers(withPronunciation(perfectAppleAnswers())); return request;
    }

    private static EngPronunciationAssessDto assessmentRequest(String attemptId, String mode, Long articleId,
            Integer levelNo, String questionId, byte[] audio)
    {
        EngPronunciationAssessDto request = new EngPronunciationAssessDto(); request.setAttemptId(attemptId);
        request.setMode(mode); request.setArticleId(articleId); request.setLevelNo(levelNo);
        request.setQuestionId(questionId); request.setAudio(multipart(audio)); return request;
    }

    private static MultipartFile multipart(byte[] bytes)
    {
        return proxy(MultipartFile.class, (method, args) -> switch (method) {
            case "isEmpty" -> bytes.length == 0; case "getSize" -> (long) bytes.length;
            case "getBytes" -> bytes; case "getName" -> "audio"; case "getOriginalFilename" -> "voice.wav";
            case "getContentType" -> "audio/wav"; default -> defaultValue(returnType(MultipartFile.class, method)); });
    }

    private static byte[] wav(boolean silent)
    {
        return wav(16000, 1, 16, 500, silent);
    }

    private static byte[] wav(int sampleRate, int channels, int bits, int durationMillis, boolean silent)
    {
        int bytesPerSample = bits / 8;
        int dataSize = sampleRate * channels * bytesPerSample * durationMillis / 1000;
        ByteBuffer buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + dataSize);
        buffer.put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16).putShort((short) 1).putShort((short) channels);
        buffer.putInt(sampleRate).putInt(sampleRate * channels * bytesPerSample);
        buffer.putShort((short) (channels * bytesPerSample)).putShort((short) bits);
        buffer.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(dataSize);
        if (bits == 16)
            for (int index = 0; index < dataSize / 2; index++) buffer.putShort(silent ? (short) 0 : (short) 1000);
        else
            for (int index = 0; index < dataSize; index++) buffer.put(silent ? (byte) 128 : (byte) 180);
        return buffer.array();
    }

    private static EngChallengeAnswerDto answer(String questionId, String value)
    {
        EngChallengeAnswerDto answer = new EngChallengeAnswerDto(); answer.setQuestionId(questionId);
        answer.setAnswer(value); return answer;
    }

    private static String testAnswerDigest(String attemptId, String questionId, String answer)
    {
        String normalized = answer == null ? "" : answer.trim().toLowerCase(Locale.ROOT);
        try
        {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((attemptId + '\0' + questionId + '\0' + normalized).getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException exception)
        {
            throw new IllegalStateException(exception);
        }
    }

    private static EngUserWordProgress progress(Long wordId, Long firstArticleId, Integer firstLevelNo, int learned)
    {
        EngUserWordProgress progress = new EngUserWordProgress(); progress.setId(wordId); progress.setUserId(USER_ID);
        progress.setWordId(wordId); progress.setLearned(learned); progress.setHighestStars(0); progress.setLatestStars(0);
        progress.setRewardedStars(0); progress.setSpellingRewardedStars(0);
        progress.setFirstArticleId(firstArticleId); progress.setFirstLevelNo(firstLevelNo);
        return progress;
    }

    /** 用内存状态模拟 Mapper，验证同一服务实例上的完整业务副作用。 */
    private static class Harness
    {
        private final Map<Long, EngUserWordProgress> progress = new LinkedHashMap<>();
        private final Map<Integer, EngArticleLevelProgress> levelProgress = new LinkedHashMap<>();
        private final Map<Long, EngUserChallengeSetting> challengeSettings = new HashMap<>();
        private final Map<String, EngStudyRecord> records = new HashMap<>();
        private final List<EngStudyRecordWord> details = new ArrayList<>();
        private final AtomicInteger detailWrites = new AtomicInteger();
        private final AtomicInteger currentAttemptReads = new AtomicInteger();
        private final AtomicInteger wrongWrites = new AtomicInteger();
        private final AtomicInteger articleWordListReads = new AtomicInteger();
        private boolean hideOrdinaryAttemptRead;
        private Long lastWrongArticleId;
        private int familiarityDelta;
        private Long sentenceSourceArticleId = ARTICLE_ID;
        private final List<String> lockOrder = new ArrayList<>();
        private long balance;
        private long recordSequence = 100L;
        private final EngWordVo apple = word(1L, "apple", "苹果");
        private final EngWordVo cat = word(2L, "cat", "猫");
        private final EngWordVo dog = word(3L, "dog", "狗");
        private final Map<Long, EngWordVo> wordById = new LinkedHashMap<>();
        private final Map<Integer, List<String>> levelWords = new LinkedHashMap<>();
        private final EngStudyServiceImpl service;
        private EngPronunciationAssessmentVo assessment = assessment(100, 100D, 0);
        private Function<byte[], EngPronunciationAssessmentVo> assessmentFunction = ignored -> assessment;
        private final MemoryRedisCache pronunciationCache = new MemoryRedisCache();
        private EngChallengeVo lastChallenge;

        Harness()
        {
            this(false);
        }

        Harness(boolean pronunciationEnabled)
        {
            wordById.put(1L, apple); wordById.put(2L, cat); wordById.put(3L, dog);
            wordById.put(4L, word(4L, "banana", "香蕉"));
            wordById.put(5L, word(5L, "cherry", "樱桃"));
            wordById.put(6L, word(6L, "grape", "葡萄"));
            wordById.put(7L, word(7L, "peach", "桃子"));
            wordById.put(8L, word(8L, "noon", "中午"));
            wordById.put(9L, word(9L, "ice-cream", "冰淇淋"));
            levelWords.put(1, List.of("apple", "cat"));
            EngArticle article = new EngArticle(); article.setId(ARTICLE_ID); article.setTitle("测试文章");
            IEngArticleService articleService = proxy(IEngArticleService.class, (method, args) ->
                    "selectEngArticleById".equals(method) ? article : defaultValue(returnType(IEngArticleService.class, method)));
            IEngSentenceService sentenceService = proxy(IEngSentenceService.class, (method, args) -> {
                if ("selectFirstWordRelation".equals(method) && Long.valueOf(1L).equals(args[args.length - 1]))
                {
                    EngSentenceWordRel relation = new EngSentenceWordRel(); relation.setArticleId(sentenceSourceArticleId);
                    relation.setWordId(1L); relation.setWordName("apple"); relation.setMatchedText("apples");
                    relation.setSentenceContent("She likes apples every day."); relation.setSentenceAcceptation("她每天喜欢苹果。");
                    return relation;
                }
                return defaultValue(returnType(IEngSentenceService.class, method));
            });
            IEngWordService wordService = proxy(IEngWordService.class, (method, args) -> {
                if ("selectWordListByArticle".equals(method))
                {
                    articleWordListReads.incrementAndGet(); return List.of(apple, cat, dog);
                }
                if ("selectEngWordById".equals(method)) return toWord(wordById.get(args[0]));
                if ("updateFamiliarity".equals(method)) { familiarityDelta += (Integer) args[1]; return 1; }
                return defaultValue(returnType(IEngWordService.class, method));
            });
            EngPronunciationProperties properties = new EngPronunciationProperties();
            properties.setEnabled(pronunciationEnabled); properties.getTencent().setAppId("test-app");
            properties.getTencent().setSecretId("test-id"); properties.getTencent().setSecretKey("test-key");
            IPronunciationAssessmentClient client = (word, audio) -> assessmentFunction.apply(audio);
            service = new EngStudyServiceImpl(articleService, sentenceService,
                    proxy(IEngIcibaSentenceService.class, (method, args) -> List.of()), wordService,
                    recordMapper(), recordWordMapper(), walletMapper(),
                    wrongWordMapper(),
                    wordProgressMapper(), levelProgressMapper(), articleWordMapper(), challengeSettingMapper(), properties, client,
                    pronunciationCache);
        }

        private EngUserChallengeSettingMapper challengeSettingMapper()
        {
            return proxy(EngUserChallengeSettingMapper.class, (method, args) -> {
                if ("selectByUserId".equals(method)) return challengeSettings.get(args[0]);
                if ("upsert".equals(method))
                {
                    EngUserChallengeSetting incoming = (EngUserChallengeSetting) args[0];
                    EngUserChallengeSetting stored = challengeSettings.get(incoming.getUserId());
                    incoming.setSettingVersion(stored == null ? 1L : stored.getSettingVersion() + 1L);
                    challengeSettings.put(incoming.getUserId(), incoming); return 1;
                }
                return defaultValue(returnType(EngUserChallengeSettingMapper.class, method));
            });
        }

        private EngStudyRecordMapper recordMapper()
        {
            return proxy(EngStudyRecordMapper.class, (method, args) -> {
                if ("selectByUserAndAttempt".equals(method))
                    return hideOrdinaryAttemptRead ? null : records.get(((EngStudyRecord) args[0]).getAttemptId());
                if ("selectByUserAndAttemptForUpdate".equals(method))
                {
                    currentAttemptReads.incrementAndGet();
                    return records.get(((EngStudyRecord) args[0]).getAttemptId());
                }
                if ("insertIgnoreEngStudyRecord".equals(method))
                {
                    EngStudyRecord item = (EngStudyRecord) args[0];
                    if (records.containsKey(item.getAttemptId())) return 0;
                    item.setId(recordSequence++); records.put(item.getAttemptId(), item); return 1;
                }
                if ("updateOutcome".equals(method)) return 1;
                return defaultValue(returnType(EngStudyRecordMapper.class, method));
            });
        }

        private EngWrongWordMapper wrongWordMapper()
        {
            return proxy(EngWrongWordMapper.class, (method, args) -> {
                if ("upsertWrongWord".equals(method))
                {
                    EngWrongWord item = (EngWrongWord) args[0]; lastWrongArticleId = item.getArticleId();
                    wrongWrites.incrementAndGet(); return 1;
                }
                return defaultValue(returnType(EngWrongWordMapper.class, method));
            });
        }

        private EngStudyRecordWordMapper recordWordMapper()
        {
            return proxy(EngStudyRecordWordMapper.class, (method, args) -> {
                if ("insertBatch".equals(method))
                {
                    @SuppressWarnings("unchecked") List<EngStudyRecordWord> items = (List<EngStudyRecordWord>) args[0];
                    details.addAll(items); detailWrites.incrementAndGet(); return items.size();
                }
                if ("selectByRecordAndUser".equals(method))
                    return details.stream().filter(item -> Objects.equals(item.getStudyRecordId(), args[0])).toList();
                return defaultValue(returnType(EngStudyRecordWordMapper.class, method));
            });
        }

        private EngCoinWalletMapper walletMapper()
        {
            return proxy(EngCoinWalletMapper.class, (method, args) -> {
                if ("selectCoinBalance".equals(method)) return balance;
                if ("increaseCoinBalance".equals(method)) { balance += (Long) args[1]; return 1; }
                return defaultValue(returnType(EngCoinWalletMapper.class, method));
            });
        }

        private EngUserWordProgressMapper wordProgressMapper()
        {
            return proxy(EngUserWordProgressMapper.class, (method, args) -> {
                if ("selectByUserAndWord".equals(method) || "selectForUpdate".equals(method)) return progress.get(args[1]);
                if ("ensureProgress".equals(method))
                {
                    EngUserWordProgress item = (EngUserWordProgress) args[0];
                    progress.computeIfAbsent(item.getWordId(), id -> progress(id, item.getFirstArticleId(), item.getFirstLevelNo(), 0));
                    return 1;
                }
                if ("updateProgress".equals(method)) { progress.put(((EngUserWordProgress) args[0]).getWordId(), (EngUserWordProgress) args[0]); return 1; }
                if ("selectLearnedWordIds".equals(method))
                {
                    @SuppressWarnings("unchecked") List<Long> ids = (List<Long>) args[1];
                    return ids.stream().filter(id -> progress.containsKey(id) && progress.get(id).getLearned() == 1).toList();
                }
                if ("selectReviewWords".equals(method))
                {
                    List<EngReviewWordVo> result = new ArrayList<>();
                    for (EngUserWordProgress item : progress.values()) if (item.getLearned() == 1)
                    {
                        EngReviewWordVo view = new EngReviewWordVo(); view.setWordId(item.getWordId());
                        EngWordVo word = wordById.get(item.getWordId());
                        view.setWordName(word.getWordName()); view.setAcceptation(word.getAcceptation());
                        view.setHighestStars(item.getHighestStars()); view.setLatestStars(item.getLatestStars());
                        view.setLatestTestTime(item.getLatestTestTime()); view.setSourceArticleNames("测试文章"); result.add(view);
                    }
                    return result;
                }
                return defaultValue(returnType(EngUserWordProgressMapper.class, method));
            });
        }

        private EngArticleLevelProgressMapper levelProgressMapper()
        {
            return proxy(EngArticleLevelProgressMapper.class, (method, args) -> {
                if ("selectByUserAndArticle".equals(method)) return new ArrayList<>(levelProgress.values());
                if ("countAnyProgress".equals(method)) return 0;
                if ("upsertBest".equals(method) || "upsertKnown".equals(method))
                {
                    EngArticleLevelProgress item = (EngArticleLevelProgress) args[0];
                    EngArticleLevelProgress stored = levelProgress.computeIfAbsent(item.getLevelNo(), ignored -> item);
                    stored.setBestScore(Math.max(intValue(stored.getBestScore()), intValue(item.getBestScore())));
                    stored.setHighestStars(Math.max(intValue(stored.getHighestStars()), intValue(item.getHighestStars())));
                    stored.setCompletedByKnownWords(Math.max(intValue(stored.getCompletedByKnownWords()),
                            intValue(item.getCompletedByKnownWords())));
                    lockOrder.add("levelProgress"); return 1;
                }
                return defaultValue(returnType(EngArticleLevelProgressMapper.class, method));
            });
        }

        private EngArticleWordRelMapper articleWordMapper()
        {
            return proxy(EngArticleWordRelMapper.class, (method, args) -> {
                if ("selectMaxLevelNo".equals(method)) return levelWords.keySet().stream().max(Integer::compareTo).orElse(0);
                if ("lockArticle".equals(method)) { lockOrder.add("articleLock"); return ARTICLE_ID; }
                if ("selectEngArticleWordRelList".equals(method))
                {
                    List<EngArticleWordRel> result = new ArrayList<>(); long id = 1L;
                    for (Map.Entry<Integer, List<String>> entry : levelWords.entrySet())
                        for (String name : entry.getValue())
                        {
                            EngArticleWordRel relation = new EngArticleWordRel(); relation.setId(id++);
                            relation.setArticleId(ARTICLE_ID); relation.setWordName(name);
                            relation.setLevelNo(entry.getKey()); relation.setCreateBy("tester"); result.add(relation);
                        }
                    return result;
                }
                if ("selectByArticleAndLevel".equals(method))
                {
                    Integer levelNo = (Integer) args[1];
                    return levelWords.getOrDefault(levelNo, List.of()).stream().map(name -> {
                        EngArticleWordRel relation = new EngArticleWordRel(); relation.setWordName(name);
                        relation.setLevelNo(levelNo); return relation;
                    }).toList();
                }
                return defaultValue(returnType(EngArticleWordRelMapper.class, method));
            });
        }
    }

    /** 仅供回归入口模拟带 TTL 的 Redis 值缓存。 */
    private static class MemoryRedisCache extends RedisCache
    {
        private final Map<String, Object> values = new HashMap<>();
        private final Map<String, Long> expiresAt = new HashMap<>();
        private final List<Integer> ttls = new ArrayList<>();
        private long nowSeconds;
        private int rejectedSaves;
        private int serializationRoundTrips;
        private boolean failRelease;

        MemoryRedisCache()
        {
            redisTemplate = new ScriptRedisTemplate(this);
        }

        @Override public <T> void setCacheObject(String key, T value, Integer timeout, java.util.concurrent.TimeUnit unit)
        { put(key, value, Math.toIntExact(unit.toSeconds(timeout))); }
        @SuppressWarnings("unchecked")
        @Override public <T> T getCacheObject(String key) { purge(key); return (T) values.get(key); }
        @Override public boolean deleteObject(String key) { expiresAt.remove(key); return values.remove(key) != null; }
        void removeResults() { new ArrayList<>(values.keySet()).stream()
                .filter(key -> !key.endsWith(":generation") && !key.endsWith(":attempts"))
                .forEach(this::deleteObject); }
        String resultPayload() { return values.entrySet().stream().filter(entry ->
                !entry.getKey().endsWith(":generation") && !entry.getKey().endsWith(":attempts"))
                .map(entry -> String.valueOf(entry.getValue())).findFirst().orElse(""); }
        void advanceSeconds(long seconds) { nowSeconds += seconds; new ArrayList<>(values.keySet()).forEach(this::purge); }
        int activeSize() { new ArrayList<>(values.keySet()).forEach(this::purge); return values.size(); }
        private void put(String key, Object value, int ttl)
        { values.put(key, serializedRoundTrip(value)); expiresAt.put(key, nowSeconds + ttl); ttls.add(ttl); }
        private void purge(String key)
        { if (expiresAt.getOrDefault(key, Long.MAX_VALUE) <= nowSeconds) deleteObject(key); }
        private Object serializedRoundTrip(Object value)
        {
            serializationRoundTrips++;
            String encoded = JSON.toJSONString(value, JSONWriter.Feature.WriteClassName);
            return JSON.parseObject(encoded, Object.class);
        }
    }

    /** 在内存中按两段 DefaultRedisScript 的键、参数和 TTL 语义执行。 */
    private static class ScriptRedisTemplate extends RedisTemplate<Object, Object>
    {
        private final MemoryRedisCache cache;
        ScriptRedisTemplate(MemoryRedisCache cache) { this.cache = cache; }

        @SuppressWarnings("unchecked")
        @Override
        public synchronized <T> T execute(RedisScript<T> script, List<Object> keys, Object... args)
        {
            String source = script.getScriptAsString();
            if (source.contains("redis.call('incr'"))
            {
                String generationKey = String.valueOf(keys.get(0)), resultKey = String.valueOf(keys.get(1));
                String attemptKey = String.valueOf(keys.get(2));
                String generation = String.valueOf(cache.serializedRoundTrip(args[0]));
                int ttl = ((Number) cache.serializedRoundTrip(args[1])).intValue();
                int maximum = ((Number) cache.serializedRoundTrip(args[2])).intValue();
                Number stored = cache.getCacheObject(attemptKey);
                int count = stored == null ? 0 : stored.intValue();
                if (count >= maximum) return (T) Long.valueOf(0L);
                count++;
                cache.put(attemptKey, count, ttl);
                cache.put(generationKey, generation, ttl); cache.deleteObject(resultKey);
                return (T) Long.valueOf(count);
            }
            if (source.contains("redis.call('decr'"))
            {
                if (cache.failRelease) throw new IllegalStateException("模拟释放失败");
                String attemptKey = String.valueOf(keys.get(0));
                int ttl = ((Number) cache.serializedRoundTrip(args[0])).intValue();
                Number stored = cache.getCacheObject(attemptKey);
                int count = stored == null ? 0 : stored.intValue();
                if (count <= 1)
                {
                    cache.deleteObject(attemptKey); return (T) Long.valueOf(0L);
                }
                cache.put(attemptKey, --count, ttl); return (T) Long.valueOf(count);
            }
            String generationKey = String.valueOf(keys.get(0)), resultKey = String.valueOf(keys.get(1));
            String generation = String.valueOf(cache.serializedRoundTrip(args[0]));
            String current = cache.getCacheObject(generationKey);
            if (!Objects.equals(current, generation))
            {
                cache.rejectedSaves++; return (T) Long.valueOf(0L);
            }
            String payload = String.valueOf(cache.serializedRoundTrip(args[1]));
            int ttl = ((Number) cache.serializedRoundTrip(args[2])).intValue();
            cache.put(resultKey, payload, ttl); return (T) Long.valueOf(1L);
        }
    }

    private static EngWordVo word(Long id, String name, String meaning)
    {
        EngWordVo word = new EngWordVo(); word.setId(id); word.setWordName(name); word.setAcceptation(meaning); return word;
    }

    private static EngWord toWord(EngWordVo source)
    {
        EngWord word = new EngWord(); word.setId(source.getId()); word.setWordName(source.getWordName());
        word.setAcceptation(source.getAcceptation()); return word;
    }

    private static void setTestLoginUser()
    {
        setTestLoginUser(USER_ID);
    }

    private static void setTestLoginUser(long userId)
    {
        SysUser user = new SysUser(); user.setUserId(userId); user.setUserName("tester" + userId);
        LoginUser loginUser = new LoginUser(); loginUser.setUserId(userId); loginUser.setUser(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation)
    {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> invocation.invoke(method.getName(), args));
    }

    private static Class<?> returnType(Class<?> type, String methodName)
    {
        for (var method : type.getMethods()) if (method.getName().equals(methodName)) return method.getReturnType();
        return Object.class;
    }

    private static Object defaultValue(Class<?> type)
    {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == long.class) return 0L;
        return 0;
    }

    private static int intValue(Integer value) { return value == null ? 0 : value; }

    private static void assertTrue(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void assertThrows(Runnable action, String message)
    { try { action.run(); } catch (ServiceException expected) { return; } throw new AssertionError(message); }
    private static void assertEquals(Object expected, Object actual, String message)
    { if (!Objects.equals(expected, actual)) throw new AssertionError(message + "，期望=" + expected + "，实际=" + actual); }

    @FunctionalInterface
    private interface Invocation { Object invoke(String method, Object[] args); }
}
