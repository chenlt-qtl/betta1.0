package com.betta.eng.service.impl;

import com.betta.common.core.domain.entity.SysUser;
import com.betta.common.core.domain.model.LoginUser;
import com.betta.eng.domain.*;
import com.betta.eng.domain.dto.*;
import com.betta.eng.domain.vo.*;
import com.betta.eng.mapper.*;
import com.betta.eng.service.*;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/** 新词关卡、全局复习、金币与幂等规则的无数据库回归入口。 */
public class EngStudyServiceImplTest
{
    private static final long USER_ID = 7L;
    private static final long ARTICLE_ID = 10L;

    public static void main(String[] args) throws Exception
    {
        setTestLoginUser();
        try
        {
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
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
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

    /** 本关所有词都已在其他文章学过时自动掌握，不伪造星级。 */
    private static void shouldAutoMasterLevelWhenEveryWordWasLearnedElsewhere()
    {
        Harness harness = new Harness();
        harness.progress.put(1L, progress(1L, 98L, 1, 1));
        harness.progress.put(2L, progress(2L, 99L, 1, 1));
        EngArticleLevelVo level = harness.service.getArticleLevels(ARTICLE_ID).getLevels().get(0);
        assertTrue(level.getMasteredByExistingWords(), "全部词在其他文章已学时应自动掌握");
        assertEquals(0, level.getHighestStars(), "自动掌握不得伪造星级");
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

    /** 下一关导航必须跳过已自动掌握关，指向下一个有真实新词的关卡。 */
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
        assertEquals(3, result.getNextLevelNo(), "应跳过第2关自动掌握词");
        assertTrue(result.getNextLevelUnlocked(), "存在下一有效新词关时应允许继续");
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
        assertTrue(EngStudyServiceImpl.class.getMethod("getChallenge", String.class, Long.class, Integer.class, List.class)
                .isAnnotationPresent(Transactional.class), "自动掌握关卡写入必须处于事务边界");
    }

    private static EngChallengeSubmitDto request(String attemptId, String mode, Long articleId, Integer levelNo)
    {
        EngChallengeSubmitDto request = new EngChallengeSubmitDto(); request.setAttemptId(attemptId);
        request.setMode(mode); request.setArticleId(articleId); request.setLevelNo(levelNo); return request;
    }

    private static List<EngChallengeAnswerDto> perfectAppleAnswers()
    {
        return List.of(answer("WORD_TO_CN:1", "苹果"), answer("CN_TO_WORD:1", "apple"),
                answer("SENTENCE_CHOICE:1", "apples"), answer("SENTENCE_FILL:1", "appl"));
    }

    private static EngChallengeAnswerDto answer(String questionId, String value)
    {
        EngChallengeAnswerDto answer = new EngChallengeAnswerDto(); answer.setQuestionId(questionId);
        answer.setAnswer(value); return answer;
    }

    private static EngUserWordProgress progress(Long wordId, Long firstArticleId, Integer firstLevelNo, int learned)
    {
        EngUserWordProgress progress = new EngUserWordProgress(); progress.setId(wordId); progress.setUserId(USER_ID);
        progress.setWordId(wordId); progress.setLearned(learned); progress.setHighestStars(0); progress.setLatestStars(0);
        progress.setRewardedStars(0); progress.setFirstArticleId(firstArticleId); progress.setFirstLevelNo(firstLevelNo);
        return progress;
    }

    /** 用内存状态模拟 Mapper，验证同一服务实例上的完整业务副作用。 */
    private static class Harness
    {
        private final Map<Long, EngUserWordProgress> progress = new LinkedHashMap<>();
        private final Map<Integer, EngArticleLevelProgress> levelProgress = new LinkedHashMap<>();
        private final Map<String, EngStudyRecord> records = new HashMap<>();
        private final List<EngStudyRecordWord> details = new ArrayList<>();
        private final AtomicInteger detailWrites = new AtomicInteger();
        private final AtomicInteger currentAttemptReads = new AtomicInteger();
        private final AtomicInteger wrongWrites = new AtomicInteger();
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
        private final Map<Integer, List<String>> levelWords = new LinkedHashMap<>();
        private final EngStudyServiceImpl service;

        Harness()
        {
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
                if ("selectWordListByArticle".equals(method)) return List.of(apple, cat, dog);
                if ("selectEngWordById".equals(method))
                    return Long.valueOf(1L).equals(args[0]) ? toWord(apple)
                            : Long.valueOf(2L).equals(args[0]) ? toWord(cat) : toWord(dog);
                if ("updateFamiliarity".equals(method)) { familiarityDelta += (Integer) args[1]; return 1; }
                return defaultValue(returnType(IEngWordService.class, method));
            });
            service = new EngStudyServiceImpl(articleService, sentenceService,
                    proxy(IEngIcibaSentenceService.class, (method, args) -> List.of()), wordService,
                    recordMapper(), recordWordMapper(), walletMapper(),
                    wrongWordMapper(),
                    wordProgressMapper(), levelProgressMapper(), articleWordMapper());
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
                        view.setWordName(item.getWordId() == 1L ? "apple" : "cat");
                        view.setAcceptation(item.getWordId() == 1L ? "苹果" : "猫");
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
        SysUser user = new SysUser(); user.setUserId(USER_ID); user.setUserName("tester");
        LoginUser loginUser = new LoginUser(); loginUser.setUser(user);
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
    private static void assertEquals(Object expected, Object actual, String message)
    { if (!Objects.equals(expected, actual)) throw new AssertionError(message + "，期望=" + expected + "，实际=" + actual); }

    @FunctionalInterface
    private interface Invocation { Object invoke(String method, Object[] args); }
}
