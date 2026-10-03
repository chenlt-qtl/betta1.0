package com.betta.eng.service.impl;

import com.betta.common.exception.ServiceException;
import com.betta.eng.domain.EngArticleWordRel;
import com.betta.eng.domain.EngDailyTestWord;
import com.betta.eng.domain.EngIcibaSentence;
import com.betta.eng.domain.EngWord;
import com.betta.eng.domain.vo.EngWordVo;
import com.betta.eng.mapper.EngWordMapper;
import com.betta.eng.mapper.EngDailyTestWordMapper;
import com.betta.eng.service.IEngArticleWordRelService;
import com.betta.eng.service.IEngIcibaSentenceService;
import com.betta.eng.service.IEngSentenceService;
import com.betta.eng.service.IEngUserScoreService;
import com.betta.eng.utils.dict.DictUtils;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.time.LocalDate;
import java.util.Date;
import com.betta.common.core.domain.entity.SysUser;
import com.betta.common.core.domain.model.LoginUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** 单词数据库查询和正式词头关系处理的独立回归入口。 */
public class EngWordServiceImplTest
{
    /** 依次验证纯数据库查询、未收录异常、生词收藏和别名关系。 */
    public static void main(String[] args)
    {
        shouldReturnDatabaseWordWithoutWriting();
        shouldRejectMissingWordWithoutWriting();
        shouldRejectWordWithoutDefinitionWithoutWriting();
        shouldReturnOnlyWordBookRelationFromWordDetail();
        shouldNotTreatArticleRelationAsWordBookRelation();
        shouldUseCanonicalWordNameForArticleRelation();
        shouldKeepExistingWordBookRelationWithoutPenalty();
        shouldPenalizeDuplicateArticleRelation();
        shouldKeepExistingCanonicalRelationWhenInputUsesAlias();
        shouldDeduplicateCanonicalAndAliasDuringArticleSync();
        shouldSaveKnownBatchWordsAndReturnMissingWords();
        shouldReturnAllMissingBatchWordsWithoutWritingRelations();
        shouldApplyDuplicateArticlePenaltyOncePerCanonicalWord();
        shouldMixDailyReviewCategoriesInPriorityOrder();
        shouldRespectDailyNewWordQuota();
        shouldExcludeGloballyCompletedDailyWord();
        shouldStopAfterTenNewWordRoundsAndResetNextDay();
    }

    /** 连续十轮完成五十个新词后第十一轮为空，次日则按已有成绩进入学习期。 */
    private static void shouldStopAfterTenNewWordRoundsAndResetNextDay()
    {
        Date now = reviewNow();
        LocalDate firstDay = LocalDate.of(2026, 10, 3);
        LocalDate nextDay = firstDay.plusDays(1);
        List<EngWordVo> words = new ArrayList<>();
        for (long id = 1; id <= 55; id++)
        {
            words.add(reviewWord(id, id, "new" + id, 0, false, 0, now));
        }
        StatefulDailyMapper dailyMapper = new StatefulDailyMapper();
        EngWordServiceImpl service = reviewService(words, dailyMapper);
        for (int round = 0; round < 10; round++)
        {
            List<EngWordVo> selected = service.selectNextReviewWords(7L, 9L, firstDay, now);
            assertEquals(5, selected.size(), "前十轮应各返回五个新词");
            assertTrue(selected.stream().allMatch(word -> "NEW".equals(word.getReviewCategory())),
                    "首日十轮应全部为新词");
            dailyMapper.complete(firstDay, selected);
        }
        assertEquals(List.of(), service.selectNextReviewWords(7L, 9L, firstDay, now),
                "首日完成五十个新词后第十一轮不应再返回新词");

        for (int index = 0; index < 50; index++)
        {
            EngWordVo learned = words.get(index);
            learned.setScoreExists(true);
            learned.setBaseFamiliarity(1);
            learned.setLastReviewTime(now);
        }
        List<EngWordVo> nextDayWords = service.selectNextReviewWords(7L, 9L, nextDay,
                new Date(now.getTime() + 24L * 60L * 60L * 1000L));
        assertEquals(5, nextDayWords.size(), "次日完成集合和新词计数应按日期重置");
        assertTrue(nextDayWords.stream().allMatch(word -> "LEARNING".equals(word.getReviewCategory())),
                "前日新词产生成绩后次日应优先进入学习期");
    }

    /** 每日队列应按学习期、到期复习和新词顺序混合补满五个。 */
    private static void shouldMixDailyReviewCategoriesInPriorityOrder()
    {
        Date now = reviewNow();
        List<EngWordVo> words = List.of(
                reviewWord(1L, 3L, "review", 5, true, 20, now),
                reviewWord(2L, 2L, "learning2", 2, true, 0, now),
                reviewWord(3L, 1L, "learning1", 1, true, 0, now),
                reviewWord(4L, 4L, "new1", 0, false, 0, now),
                reviewWord(5L, 5L, "new2", 0, false, 0, now),
                reviewWord(6L, 6L, "new3", 0, false, 0, now));
        List<EngWordVo> result = reviewService(words, 0, List.of())
                .selectNextReviewWords(7L, 9L, LocalDate.of(2026, 10, 3), now);
        assertEquals(List.of(3L, 2L, 1L, 4L, 5L), reviewWordIds(result), "每日队列优先级错误");
        assertEquals(List.of("LEARNING", "LEARNING", "REVIEW", "NEW", "NEW"),
                result.stream().map(EngWordVo::getReviewCategory).toList(), "每日队列分类错误");
    }

    /** 新词达到五十个后不得继续加入，仅剩一个配额时也只能补一个。 */
    private static void shouldRespectDailyNewWordQuota()
    {
        Date now = reviewNow();
        List<EngWordVo> words = List.of(
                reviewWord(1L, 1L, "learning", 1, true, 0, now),
                reviewWord(2L, 2L, "new1", 0, false, 0, now),
                reviewWord(3L, 3L, "new2", 0, false, 0, now));
        assertEquals(List.of(1L), reviewWordIds(reviewService(words, 50, List.of())
                .selectNextReviewWords(7L, 9L, LocalDate.of(2026, 10, 3), now)), "达到配额后不应返回新词");
        assertEquals(List.of(1L, 2L), reviewWordIds(reviewService(words, 49, List.of())
                .selectNextReviewWords(7L, 9L, LocalDate.of(2026, 10, 3), now)), "剩余配额必须准确限制新词数");
    }

    /** 同一单词在其他文章完成后当天全站不再出现。 */
    private static void shouldExcludeGloballyCompletedDailyWord()
    {
        Date now = reviewNow();
        List<EngWordVo> words = List.of(
                reviewWord(1L, 1L, "done", 1, true, 0, now),
                reviewWord(2L, 2L, "available", 1, true, 0, now));
        assertEquals(List.of(2L), reviewWordIds(reviewService(words, 0, List.of(1L))
                .selectNextReviewWords(7L, 99L, LocalDate.of(2026, 10, 3), now)), "已完成单词应全站日去重");
    }

    private static EngWordServiceImpl reviewService(List<EngWordVo> words, int newCount, List<Long> completed)
    {
        setTestLoginUser();
        EngWordMapper wordMapper = (EngWordMapper) Proxy.newProxyInstance(EngWordMapper.class.getClassLoader(),
                new Class<?>[] {EngWordMapper.class}, (proxy, method, args) ->
                        "selectWordListByArticleId".equals(method.getName()) ? words : null);
        EngDailyTestWordMapper dailyMapper = dailyTestWordMapper(newCount, completed);
        return new EngWordServiceImpl(wordMapper, null, null, null, null, new DictUtils(wordMapper), dailyMapper);
    }

    private static EngWordServiceImpl reviewService(List<EngWordVo> words, EngDailyTestWordMapper dailyMapper)
    {
        setTestLoginUser();
        EngWordMapper wordMapper = (EngWordMapper) Proxy.newProxyInstance(EngWordMapper.class.getClassLoader(),
                new Class<?>[] {EngWordMapper.class}, (proxy, method, args) ->
                        "selectWordListByArticleId".equals(method.getName()) ? words : null);
        return new EngWordServiceImpl(wordMapper, null, null, null, null, new DictUtils(wordMapper), dailyMapper);
    }

    /** 按日期保存已完成集合和新词计数，用于连续多轮队列测试。 */
    private static class StatefulDailyMapper implements EngDailyTestWordMapper
    {
        private final Map<LocalDate, Set<Long>> completedIds = new HashMap<>();
        private final Map<LocalDate, Integer> newCounts = new HashMap<>();

        private void complete(LocalDate studyDate, List<EngWordVo> words)
        {
            completedIds.computeIfAbsent(studyDate, key -> new HashSet<>())
                    .addAll(words.stream().map(EngWordVo::getId).toList());
            int newCount = (int) words.stream().filter(word -> "NEW".equals(word.getReviewCategory())).count();
            newCounts.merge(studyDate, newCount, Integer::sum);
        }

        @Override
        public List<Long> selectCompletedWordIds(Long userId, LocalDate studyDate)
        {
            return new ArrayList<>(completedIds.getOrDefault(studyDate, Set.of()));
        }

        @Override
        public int countNewWords(Long userId, Long articleId, LocalDate studyDate)
        {
            return newCounts.getOrDefault(studyDate, 0);
        }

        @Override
        public int insertDailyTestWord(EngDailyTestWord dailyTestWord) { return 1; }

        @Override
        public int bindStudyRecord(Long userId, Long articleId, LocalDate studyDate,
                List<Long> wordIds, Long studyRecordId) { return wordIds.size(); }
    }

    private static EngDailyTestWordMapper dailyTestWordMapper(int newCount, List<Long> completed)
    {
        return new EngDailyTestWordMapper() {
            @Override
            public List<Long> selectCompletedWordIds(Long userId, LocalDate studyDate) { return completed; }
            @Override
            public int countNewWords(Long userId, Long articleId, LocalDate studyDate) { return newCount; }
            @Override
            public int insertDailyTestWord(EngDailyTestWord dailyTestWord) { return 1; }
            @Override
            public int bindStudyRecord(Long userId, Long articleId, LocalDate studyDate,
                    List<Long> wordIds, Long studyRecordId) { return wordIds.size(); }
        };
    }

    private static Date reviewNow() { return new Date(2_000L * 24L * 60L * 60L * 1000L); }

    private static EngWordVo reviewWord(Long id, Long relId, String name, int familiarity,
            boolean scoreExists, int elapsedDays, Date now)
    {
        EngWordVo word = new EngWordVo();
        word.setId(id);
        word.setRelId(relId);
        word.setWordName(name);
        word.setAcceptation("释义");
        word.setBaseFamiliarity(familiarity);
        word.setScoreExists(scoreExists);
        word.setLastReviewTime(new Date(now.getTime() - elapsedDays * 24L * 60L * 60L * 1000L));
        return word;
    }

    private static List<Long> reviewWordIds(List<EngWordVo> words)
    {
        return words.stream().map(EngWordVo::getId).toList();
    }

    private static void setTestLoginUser()
    {
        SysUser user = new SysUser();
        user.setUserId(7L);
        user.setUserName("tester");
        LoginUser loginUser = new LoginUser();
        loginUser.setUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
    }

    /** 查词应聚合数据库现有数据，不新增或刷新词条和例句。 */
    private static void shouldReturnDatabaseWordWithoutWriting()
    {
        Harness harness = new Harness(word("watermelon", "noun 西瓜", 7L));
        harness.sentences.add(sentence(7L, "Watermelon is sweet.", "西瓜很甜。"));

        EngWordVo result = harness.service.getWordVo(" Watermelon ");

        assertEquals(7L, result.getId(), "应返回数据库现有词条");
        assertEquals(1, result.getIcibaSentenceList().size(), "应返回数据库现有例句");
        assertEquals(0, harness.wordInsertCalls.get(), "查词不得新增单词");
        assertEquals(0, harness.wordUpdateCalls.get(), "查词不得更新单词");
        assertEquals(0, harness.sentenceInsertCalls.get(), "查词不得新增例句");
    }

    /** 未收录词应直接报错，且查询过程不得写数据库。 */
    private static void shouldRejectMissingWordWithoutWriting()
    {
        Harness harness = new Harness((EngWord) null);

        assertMissing(() -> harness.service.getWordVo("missing"), "missing");
        assertNoDictionaryWrites(harness);
    }

    /** 空释义或旧 JSON 释义不能作为可展示词典结果，也不得自动刷新。 */
    private static void shouldRejectWordWithoutDefinitionWithoutWriting()
    {
        Harness empty = new Harness(word("empty", null, 8L));
        assertMissing(() -> empty.service.getWordVo("empty"), "empty");
        assertNoDictionaryWrites(empty);

        Harness legacy = new Harness(word("legacy", "[{\"part\":\"noun\"}]", 9L));
        assertMissing(() -> legacy.service.getWordVo("legacy"), "legacy");
        assertNoDictionaryWrites(legacy);
    }

    /** 详情同时存在文章关系和生词关系时，只返回生词收藏关系主键。 */
    private static void shouldReturnOnlyWordBookRelationFromWordDetail()
    {
        Harness harness = new Harness(word("watermelon", "noun 西瓜", 10L));
        harness.relations.add(relation(31L, 5L, "watermelon"));
        harness.relations.add(relation(32L, 0L, "watermelon"));

        EngWordVo result = harness.service.getWordVo("watermelon");

        assertEquals(32L, result.getRelId(), "详情只能返回生词收藏关系主键");
    }

    /** 详情只有真实文章关系时，不得把文章关系误判为生词收藏。 */
    private static void shouldNotTreatArticleRelationAsWordBookRelation()
    {
        Harness harness = new Harness(word("watermelon", "noun 西瓜", 11L));
        harness.relations.add(relation(33L, 5L, "watermelon"));

        EngWordVo result = harness.service.getWordVo("watermelon");

        assertEquals(null, result.getRelId(), "文章关系不能作为生词收藏关系返回");
    }

    /** 通过别名加入文章时保存 Mapper 解析后的正式词头。 */
    private static void shouldUseCanonicalWordNameForArticleRelation()
    {
        Harness harness = new Harness(word("color", "noun 颜色", 10L));

        harness.service.addArticleWord(5L, "colour");

        assertEquals(1, harness.relations.size(), "应新增一条文章单词关系");
        assertEquals("color", harness.relations.get(0).getWordName(), "文章关系必须使用正式词头");
        assertNoDictionaryWrites(harness);
    }

    /** 重复添加已有生词收藏应幂等返回，不新增关系也不扣熟悉度。 */
    private static void shouldKeepExistingWordBookRelationWithoutPenalty()
    {
        Harness harness = new Harness(word("color", "noun 颜色", 13L));
        harness.relations.add(relation(34L, 0L, "color"));

        harness.service.addArticleWord(0L, "colour");

        assertEquals(1, harness.relations.size(), "重复收藏不得新增关系");
        assertEquals(0, harness.relationInsertCalls.get(), "重复收藏不得调用新增关系");
        assertEquals(0, harness.scoreUpdateCalls.get(), "重复收藏不得扣熟悉度");
    }

    /** 真实文章重复添加单词时，保留原有扣熟悉度规则。 */
    private static void shouldPenalizeDuplicateArticleRelation()
    {
        Harness harness = new Harness(word("color", "noun 颜色", 14L));
        harness.relations.add(relation(35L, 5L, "color"));

        harness.service.addArticleWord(5L, "colour");

        assertEquals(1, harness.relations.size(), "文章重复加词不得新增关系");
        assertEquals(0, harness.relationInsertCalls.get(), "文章重复加词不得调用新增关系");
        assertEquals(1, harness.scoreUpdateCalls.get(), "文章重复加词仍应扣熟悉度");
    }

    /** 已有正式词关系时，别名输入不得删除重建或触发重复加词扣分。 */
    private static void shouldKeepExistingCanonicalRelationWhenInputUsesAlias()
    {
        Harness harness = new Harness(word("color", "noun 颜色", 11L));
        harness.relations.add(relation(21L, 5L, "color"));

        harness.service.updateByArticle(List.of("colour"), 5L);

        assertEquals(1, harness.relations.size(), "别名输入应保留已有正式词关系");
        assertEquals(0, harness.relationInsertCalls.get(), "已有关系不得重复新增");
        assertEquals(0, harness.relationDeleteCalls.get(), "已有关系不得删除重建");
        assertEquals(0, harness.scoreUpdateCalls.get(), "同步文章关系不得触发扣分");
    }

    /** 正式词与别名同时输入时，只应新增一条正式词关系。 */
    private static void shouldDeduplicateCanonicalAndAliasDuringArticleSync()
    {
        Harness harness = new Harness(word("color", "noun 颜色", 12L));

        harness.service.updateByArticle(List.of("color", "colour"), 5L);

        assertEquals(1, harness.relations.size(), "正式词和别名应去重为一条关系");
        assertEquals("color", harness.relations.get(0).getWordName(), "新增关系应使用正式词头");
        assertEquals(1, harness.relationInsertCalls.get(), "仅应新增一次正式词关系");
        assertEquals(0, harness.scoreUpdateCalls.get(), "批量同步不得触发扣分");
    }

    /** 批量新增应保存已收录词，并按规范化输入返回去重后的未收录词。 */
    private static void shouldSaveKnownBatchWordsAndReturnMissingWords()
    {
        EngWord apple = word("apple", "noun 苹果", 15L);
        Harness harness = new Harness(Map.of("apple", apple));

        List<String> inputs = new ArrayList<>(List.of(" Apple ", "MISSING", "missing", " "));
        inputs.add(null);
        List<String> missingWords = harness.service.addArticleWords(5L, inputs);

        assertEquals(List.of("missing"), missingWords, "应返回规范化、去重后的未收录输入");
        assertEquals(1, harness.relations.size(), "已收录词应正常加入文章");
        assertEquals("apple", harness.relations.get(0).getWordName(), "文章关系应保存正式词头");
        assertEquals(1, harness.relationInsertCalls.get(), "部分未收录不应影响有效词写入");
    }

    /** 全部输入均未收录时，应返回完整去重结果且不写入任何文章关系。 */
    private static void shouldReturnAllMissingBatchWordsWithoutWritingRelations()
    {
        Harness harness = new Harness(Map.of());

        List<String> missingWords = harness.service.addArticleWords(5L,
                List.of(" MISSING ", "missing", "UNKNOWN", "unknown"));

        assertEquals(List.of("missing", "unknown"), missingWords, "应返回全部规范化、去重后的未收录输入");
        assertEquals(0, harness.relations.size(), "全部未收录时不得建立文章关系");
        assertEquals(0, harness.relationInsertCalls.get(), "全部未收录时不得调用新增关系");
    }

    /** 正式词与别名混合重复输入时，每个正式词头最多触发一次原有扣分规则。 */
    private static void shouldApplyDuplicateArticlePenaltyOncePerCanonicalWord()
    {
        EngWord color = word("color", "noun 颜色", 16L);
        Map<String, EngWord> dictionary = new LinkedHashMap<>();
        dictionary.put("color", color);
        dictionary.put("colour", color);
        Harness harness = new Harness(dictionary);
        harness.relations.add(relation(36L, 5L, "color"));

        List<String> missingWords = harness.service.addArticleWords(5L,
                List.of("color", "COLOUR", "colour"));

        assertEquals(List.of(), missingWords, "已收录正式词和别名不应报告未收录");
        assertEquals(1, harness.relations.size(), "正式词和别名不得重复建立关系");
        assertEquals(1, harness.scoreUpdateCalls.get(), "同一正式词头在批次内只能扣一次熟悉度");
    }

    private static void assertMissing(Runnable action, String wordName)
    {
        try
        {
            action.run();
            throw new AssertionError("未收录词应抛出业务异常：" + wordName);
        }
        catch (ServiceException exception)
        {
            assertEquals("词典未查询到单词：" + wordName, exception.getMessage(), "异常信息应包含查询词");
        }
    }

    private static void assertNoDictionaryWrites(Harness harness)
    {
        assertEquals(0, harness.wordInsertCalls.get(), "查词不得新增单词");
        assertEquals(0, harness.wordUpdateCalls.get(), "查词不得更新单词");
        assertEquals(0, harness.sentenceInsertCalls.get(), "查词不得新增例句");
    }

    private static EngWord word(String wordName, String acceptation, Long id)
    {
        EngWord word = new EngWord();
        word.setId(id);
        word.setWordName(wordName);
        word.setAcceptation(acceptation);
        word.setDictionarySource("oald10");
        return word;
    }

    private static EngIcibaSentence sentence(Long wordId, String orig, String trans)
    {
        EngIcibaSentence sentence = new EngIcibaSentence();
        sentence.setWordId(wordId);
        sentence.setOrig(orig);
        sentence.setTrans(trans);
        return sentence;
    }

    private static EngArticleWordRel relation(Long id, Long articleId, String wordName)
    {
        EngArticleWordRel relation = new EngArticleWordRel();
        relation.setId(id);
        relation.setArticleId(articleId);
        relation.setWordName(wordName);
        return relation;
    }

    private static void assertEquals(Object expected, Object actual, String message)
    {
        if (expected == null ? actual != null : !expected.equals(actual))
        {
            throw new AssertionError(message + "，期望=" + expected + "，实际=" + actual);
        }
    }

    private static void assertTrue(boolean value, String message)
    {
        if (!value)
        {
            throw new AssertionError(message);
        }
    }

    /** 为单词、例句和文章关系提供内存桩。 */
    private static final class Harness
    {
        private final Function<String, EngWord> wordLookup;
        private final List<EngIcibaSentence> sentences = new ArrayList<>();
        private final List<EngArticleWordRel> relations = new ArrayList<>();
        private final AtomicInteger wordInsertCalls = new AtomicInteger();
        private final AtomicInteger wordUpdateCalls = new AtomicInteger();
        private final AtomicInteger sentenceInsertCalls = new AtomicInteger();
        private final AtomicInteger relationInsertCalls = new AtomicInteger();
        private final AtomicInteger relationDeleteCalls = new AtomicInteger();
        private final AtomicInteger scoreUpdateCalls = new AtomicInteger();
        private final EngWordServiceImpl service;

        private Harness(EngWord storedWord)
        {
            this(ignored -> storedWord);
        }

        private Harness(Map<String, EngWord> storedWords)
        {
            this(storedWords::get);
        }

        private Harness(Function<String, EngWord> wordLookup)
        {
            this.wordLookup = wordLookup;
            EngWordMapper wordMapper = wordMapper();
            service = new EngWordServiceImpl(wordMapper, articleWordRelService(), userScoreService(),
                    sentenceService(), dictionarySentenceService(), new DictUtils(wordMapper),
                    dailyTestWordMapper(0, List.of()));
        }

        private EngWordMapper wordMapper()
        {
            return (EngWordMapper) Proxy.newProxyInstance(EngWordMapper.class.getClassLoader(),
                    new Class<?>[] { EngWordMapper.class }, (proxy, method, args) -> switch (method.getName())
                    {
                        case "selectEngWordByWordName" -> {
                            EngWord storedWord = wordLookup.apply((String) args[0]);
                            yield storedWord == null ? List.of() : List.of(storedWord);
                        }
                        case "insertEngWord" -> wordInsertCalls.incrementAndGet();
                        case "updateEngWord" -> wordUpdateCalls.incrementAndGet();
                        default -> defaultValue(method.getReturnType());
                    });
        }

        private IEngIcibaSentenceService dictionarySentenceService()
        {
            return (IEngIcibaSentenceService) Proxy.newProxyInstance(
                    IEngIcibaSentenceService.class.getClassLoader(),
                    new Class<?>[] { IEngIcibaSentenceService.class }, (proxy, method, args) -> {
                        if ("selectEngIcibaSentenceList".equals(method.getName()))
                        {
                            return new ArrayList<>(sentences);
                        }
                        if ("insertEngIcibaSentence".equals(method.getName()))
                        {
                            sentenceInsertCalls.incrementAndGet();
                            return 1;
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private IEngArticleWordRelService articleWordRelService()
        {
            return (IEngArticleWordRelService) Proxy.newProxyInstance(
                    IEngArticleWordRelService.class.getClassLoader(),
                    new Class<?>[] { IEngArticleWordRelService.class }, (proxy, method, args) -> {
                        if ("selectEngArticleWordRelList".equals(method.getName()))
                        {
                            EngArticleWordRel condition = (EngArticleWordRel) args[0];
                            return relations.stream()
                                    .filter(rel -> condition.getArticleId() == null
                                            || condition.getArticleId().equals(rel.getArticleId()))
                                    .filter(rel -> condition.getWordName() == null
                                            || condition.getWordName().equals(rel.getWordName()))
                                    .toList();
                        }
                        if ("insertEngArticleWordRel".equals(method.getName()))
                        {
                            EngArticleWordRel rel = (EngArticleWordRel) args[0];
                            rel.setId((long) relations.size() + 1);
                            relations.add(rel);
                            relationInsertCalls.incrementAndGet();
                            return 1;
                        }
                        if ("deleteEngArticleWordRelByIds".equals(method.getName()))
                        {
                            Long[] ids = (Long[]) args[0];
                            relations.removeIf(rel -> List.of(ids).contains(rel.getId()));
                            relationDeleteCalls.incrementAndGet();
                            return ids.length;
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private IEngUserScoreService userScoreService()
        {
            return (IEngUserScoreService) Proxy.newProxyInstance(IEngUserScoreService.class.getClassLoader(),
                    new Class<?>[] { IEngUserScoreService.class }, (proxy, method, args) -> {
                        if ("updateEngUserScore".equals(method.getName()))
                        {
                            scoreUpdateCalls.incrementAndGet();
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private IEngSentenceService sentenceService()
        {
            return (IEngSentenceService) Proxy.newProxyInstance(IEngSentenceService.class.getClassLoader(),
                    new Class<?>[] { IEngSentenceService.class }, (proxy, method, args) -> {
                        if ("selectByWordTop10".equals(method.getName()))
                        {
                            return List.of();
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private Object defaultValue(Class<?> type)
        {
            if (type == int.class)
            {
                return 0;
            }
            if (type == boolean.class)
            {
                return false;
            }
            return null;
        }
    }
}
