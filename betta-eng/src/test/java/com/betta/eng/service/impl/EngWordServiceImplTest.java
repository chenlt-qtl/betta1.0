package com.betta.eng.service.impl;

import com.betta.common.exception.ServiceException;
import com.betta.eng.domain.EngArticleWordRel;
import com.betta.eng.domain.EngIcibaSentence;
import com.betta.eng.domain.EngWord;
import com.betta.eng.domain.vo.EngWordVo;
import com.betta.eng.mapper.EngWordMapper;
import com.betta.eng.service.IEngArticleWordRelService;
import com.betta.eng.service.IEngIcibaSentenceService;
import com.betta.eng.service.IEngSentenceService;
import com.betta.eng.service.IEngUserScoreService;
import com.betta.eng.utils.dict.DictUtils;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** 单词数据库查询和正式词头关系处理的独立回归入口。 */
public class EngWordServiceImplTest
{
    /** 依次验证纯数据库查询、未收录异常和别名关系。 */
    public static void main(String[] args)
    {
        shouldReturnDatabaseWordWithoutWriting();
        shouldRejectMissingWordWithoutWriting();
        shouldRejectWordWithoutDefinitionWithoutWriting();
        shouldUseCanonicalWordNameForArticleRelation();
        shouldKeepExistingCanonicalRelationWhenInputUsesAlias();
        shouldDeduplicateCanonicalAndAliasDuringArticleSync();
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
        Harness harness = new Harness(null);

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

    /** 通过别名加入文章时保存 Mapper 解析后的正式词头。 */
    private static void shouldUseCanonicalWordNameForArticleRelation()
    {
        Harness harness = new Harness(word("color", "noun 颜色", 10L));

        harness.service.addArticleWord(5L, "colour");

        assertEquals(1, harness.relations.size(), "应新增一条文章单词关系");
        assertEquals("color", harness.relations.get(0).getWordName(), "文章关系必须使用正式词头");
        assertNoDictionaryWrites(harness);
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

    /** 为单词、例句和文章关系提供内存桩。 */
    private static final class Harness
    {
        private final EngWord storedWord;
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
            this.storedWord = storedWord;
            EngWordMapper wordMapper = wordMapper();
            service = new EngWordServiceImpl(wordMapper, articleWordRelService(), userScoreService(),
                    sentenceService(), dictionarySentenceService(), new DictUtils(wordMapper));
        }

        private EngWordMapper wordMapper()
        {
            return (EngWordMapper) Proxy.newProxyInstance(EngWordMapper.class.getClassLoader(),
                    new Class<?>[] { EngWordMapper.class }, (proxy, method, args) -> switch (method.getName())
                    {
                        case "selectEngWordByWordName" -> storedWord == null ? List.of() : List.of(storedWord);
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
