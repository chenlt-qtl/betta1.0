package com.betta.eng.service.impl;

import com.betta.common.core.domain.entity.SysUser;
import com.betta.common.core.domain.model.LoginUser;
import com.betta.common.exception.ServiceException;
import com.betta.eng.domain.EngArticle;
import com.betta.eng.domain.EngArticleWordRel;
import com.betta.eng.domain.EngSentence;
import com.betta.eng.domain.EngSentenceWordRel;
import com.betta.eng.domain.EngWord;
import com.betta.eng.domain.dojo.BatchAddSentences;
import com.betta.eng.domain.dto.EngSentenceWordUpdateDto;
import com.betta.eng.domain.vo.EngSentenceSegmentVo;
import com.betta.eng.domain.vo.EngSentenceWordOptionsVo;
import com.betta.eng.domain.vo.EngWordFormMatchVo;
import com.betta.eng.mapper.EngArticleMapper;
import com.betta.eng.mapper.EngArticleLevelProgressMapper;
import com.betta.eng.mapper.EngArticleWordRelMapper;
import com.betta.eng.mapper.EngSentenceMapper;
import com.betta.eng.mapper.EngSentenceWordRelMapper;
import com.betta.eng.mapper.EngWordMapper;
import com.betta.eng.service.IEngArticleWordRelService;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/** 句子与规范词关系业务的无数据库回归入口。 */
public class EngSentenceServiceImplTest
{
    /** 依次验证词形回显、伪造拦截、最后引用清理和用户隔离。 */
    public static void main(String[] args)
    {
        setTestLoginUser();
        try
        {
            shouldResolvePluralAndPreserveWholeWordBoundary();
            shouldRejectWordThatDoesNotOccurInSentence();
            shouldDeleteArticleWordOnlyAfterLastSentenceReference();
            shouldRejectSentenceOwnedByAnotherUser();
            shouldRejectUnownedArticleBeforeAnyWrite();
            shouldCleanRelationsWhenArticleWordIsDeleted();
            shouldAssignPermanentLevelsInInsertionOrder();
            shouldCanonicalizeAndDeduplicateRawRelationInsert();
            shouldCleanArticleWordWhenSentenceChangesOrIsDeleted();
            shouldResolvePrototypeCandidateAndRejectAmbiguity();
            shouldStopAfterMapperFailureAndExposeTransactionBoundary();
            shouldUseConstantMapperCallsWhenSavingMultipleWords();
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
    }

    /** 复数和大小写应回显同一规范词，pineapple 不得被拆成 apple。 */
    private static void shouldResolvePluralAndPreserveWholeWordBoundary()
    {
        Harness harness = new Harness(true, 1);
        EngSentenceWordOptionsVo options = harness.service.selectWordOptions(10L);
        List<EngSentenceSegmentVo> words = options.getSegments().stream()
                .filter(segment -> "WORD".equals(segment.getType())).toList();
        EngSentenceSegmentVo plural = segment(words, "apples");
        EngSentenceSegmentVo capitalized = segment(words, "Apple");
        EngSentenceSegmentVo substring = segment(words, "Pineapple");
        assertEquals(1L, plural.getWordId(), "复数别名应解析到规范词 apple");
        assertTrue(plural.isSelected(), "已保存的复数关系必须正确回显");
        assertEquals(1L, capitalized.getWordId(), "大小写差异应解析到同一规范词");
        assertTrue(capitalized.isSelected(), "同一规范词的多个出现位置应同步高亮");
        assertEquals(null, substring.getWordId(), "apple 不得命中 pineapple 子串");
        assertTrue(!substring.isSelectable(), "未收录的完整词应显示为不可选择");
    }

    /** 客户端提交句中不存在的规范词时不得产生任何写入。 */
    private static void shouldRejectWordThatDoesNotOccurInSentence()
    {
        Harness harness = new Harness(false, 0);
        EngSentenceWordUpdateDto request = new EngSentenceWordUpdateDto();
        request.setWordIds(List.of(2L));
        try
        {
            harness.service.updateWordOptions(10L, request);
            throw new AssertionError("伪造句子关系必须失败");
        }
        catch (ServiceException expected)
        {
            assertTrue(expected.getMessage().contains("未在句子中"), "应返回可理解的词形校验错误");
        }
        assertEquals(0, harness.relationWrites.get(), "伪造请求不得写句子关系");
        assertEquals(0, harness.articleDeletes.get(), "伪造请求不得删除文章词关系");
    }

    /** 取消当前句子选词时，仅最后一个引用会同步删除文章词关系。 */
    private static void shouldDeleteArticleWordOnlyAfterLastSentenceReference()
    {
        EngSentenceWordUpdateDto empty = new EngSentenceWordUpdateDto();

        Harness lastReference = new Harness(true, 0);
        lastReference.service.updateWordOptions(10L, empty);
        assertEquals(1, lastReference.relationDeletes.get(), "取消选词应删除当前句子关系");
        assertEquals(1, lastReference.articleDeletes.get(), "最后一个句子引用取消后应删除文章词");

        Harness anotherReference = new Harness(true, 1);
        anotherReference.service.updateWordOptions(10L, empty);
        assertEquals(1, anotherReference.relationDeletes.get(), "当前句子关系仍应删除");
        assertEquals(0, anotherReference.articleDeletes.get(), "其他句子仍引用时必须保留文章词");
    }

    /** 找不到当前用户句子时不得泄露或修改数据。 */
    private static void shouldRejectSentenceOwnedByAnotherUser()
    {
        Harness harness = new Harness(false, 0);
        harness.owned = false;
        try
        {
            harness.service.selectWordOptions(10L);
            throw new AssertionError("跨用户读取必须失败");
        }
        catch (ServiceException expected)
        {
            assertTrue(expected.getMessage().contains("无权操作"), "跨用户访问应返回统一权限错误");
        }
    }

    /** 新增、批量新增、修改和关系更新均须先确认目标文章属于当前用户。 */
    private static void shouldRejectUnownedArticleBeforeAnyWrite()
    {
        Harness harness = new Harness(true, 0);
        harness.articleOwned = false;

        EngSentence added = sentence();
        added.setId(null);
        assertServiceException(() -> harness.service.insertEngSentence(added), "新增句子必须校验文章归属");

        BatchAddSentences batch = new BatchAddSentences();
        batch.setArticleId(20L);
        batch.setSentenceStr("A sentence.");
        assertServiceException(() -> harness.service.insertEngSentenceBatch(batch), "批量新增必须校验文章归属");

        EngSentence updated = sentence();
        updated.setContent("Updated apples.");
        assertServiceException(() -> harness.service.updateEngSentence(updated), "修改句子必须校验目标文章归属");
        assertServiceException(() -> harness.service.updateWordOptions(10L, new EngSentenceWordUpdateDto()),
                "更新句子词关系必须校验文章归属");

        assertEquals(0, harness.sentenceWrites.get(), "文章归属校验失败后不得写句子");
        assertEquals(0, harness.relationWrites.get(), "文章归属校验失败后不得写句子关系");
        assertEquals(0, harness.relationDeletes.get(), "文章归属校验失败后不得删除句子关系");
        assertEquals(0, harness.articleDeletes.get(), "文章归属校验失败后不得删除文章词");
    }

    /** 从文章词表删除规范词时，应先清理该文章下全部句子关系。 */
    private static void shouldCleanRelationsWhenArticleWordIsDeleted()
    {
        List<String> operations = new ArrayList<>();
        EngArticleWordRel relation = new EngArticleWordRel();
        relation.setId(30L);
        relation.setArticleId(20L);
        relation.setWordName("apple");
        EngArticleWordRelMapper articleWordMapper = proxy(EngArticleWordRelMapper.class, (method, args) -> {
            if ("selectEngArticleWordRelById".equals(method)) return relation;
            if ("deleteEngArticleWordRelByIds".equals(method))
            {
                operations.add("article");
                return 1;
            }
            return defaultValue(methodReturnType(EngArticleWordRelMapper.class, method));
        });
        EngSentenceWordRelMapper sentenceWordMapper = proxy(EngSentenceWordRelMapper.class, (method, args) -> {
            if ("deleteByArticleAndWordNames".equals(method))
            {
                operations.add("sentence");
                assertEquals(List.of("apple"), args[1], "应按规范词名清理文章内句子关系");
                return 2;
            }
            return defaultValue(methodReturnType(EngSentenceWordRelMapper.class, method));
        });

        EngArticleWordRelServiceImpl service = new EngArticleWordRelServiceImpl(articleWordMapper,
                sentenceWordMapper, proxy(EngArticleLevelProgressMapper.class,
                        (method, args) -> defaultValue(methodReturnType(EngArticleLevelProgressMapper.class, method))),
                proxy(EngWordMapper.class,
                        (method, args) -> defaultValue(methodReturnType(EngWordMapper.class, method))));
        assertEquals(1, service.deleteEngArticleWordRelByIds(new Long[] {30L}), "文章词删除行数错误");
        assertEquals(List.of("sentence", "article"), operations, "必须先清句子关系再删文章词");
    }

    /** 文章词按新增顺序每五个固定为一关，不重排旧关卡。 */
    private static void shouldAssignPermanentLevelsInInsertionOrder()
    {
        List<EngArticleWordRel> stored = new ArrayList<>();
        AtomicInteger historyMax = new AtomicInteger();
        java.util.concurrent.atomic.AtomicBoolean frozen = new java.util.concurrent.atomic.AtomicBoolean();
        EngArticleWordRelMapper articleWordMapper = proxy(EngArticleWordRelMapper.class, (method, args) -> {
            if ("lockArticle".equals(method)) return 20L;
            if ("selectMaxLevelNo".equals(method))
                return stored.stream().map(EngArticleWordRel::getLevelNo).max(Integer::compareTo).orElse(0);
            if ("countByArticleAndLevel".equals(method))
                return (int) stored.stream().filter(item -> item.getLevelNo().equals(args[1])).count();
            if ("selectExistingWordNames".equals(method))
            {
                @SuppressWarnings("unchecked") List<String> names = (List<String>) args[1];
                return stored.stream().map(EngArticleWordRel::getWordName).filter(names::contains).toList();
            }
            if ("insertBatch".equals(method))
            {
                @SuppressWarnings("unchecked") List<EngArticleWordRel> relations = (List<EngArticleWordRel>) args[0];
                stored.addAll(relations);
                return relations.size();
            }
            return defaultValue(methodReturnType(EngArticleWordRelMapper.class, method));
        });
        EngWordMapper wordMapper = proxy(EngWordMapper.class, (method, args) -> {
            if ("selectByIds".equals(method))
            {
                @SuppressWarnings("unchecked") List<Long> ids = (List<Long>) args[0];
                return ids.stream().map(id -> word(id, "word" + id)).toList();
            }
            return defaultValue(methodReturnType(EngWordMapper.class, method));
        });
        EngArticleWordRelServiceImpl service = new EngArticleWordRelServiceImpl(articleWordMapper,
                proxy(EngSentenceWordRelMapper.class,
                        (method, args) -> defaultValue(methodReturnType(EngSentenceWordRelMapper.class, method))),
                proxy(EngArticleLevelProgressMapper.class, (method, args) -> {
                    if ("countAnyProgress".equals(method)) return frozen.get() ? 1 : 0;
                    if ("selectMaxLevelNoByArticle".equals(method)) return historyMax.get();
                    return defaultValue(methodReturnType(EngArticleLevelProgressMapper.class, method));
                }), wordMapper);

        service.insertMissingByWordIds(20L, List.of(1L, 2L, 2L, 3L, 4L, 5L, 6L));
        assertEquals(List.of(1, 1, 1, 1, 1, 2), stored.stream().map(EngArticleWordRel::getLevelNo).toList(),
                "应按添加顺序每五词分关");
        assertEquals(List.of("word1", "word2", "word3", "word4", "word5", "word6"),
                stored.stream().map(EngArticleWordRel::getWordName).toList(), "批量新增应保序并去重");
        service.insertMissingByWordIds(20L, List.of(6L));
        assertEquals(6, stored.size(), "已存在规范词必须幂等");

        historyMax.set(2);
        stored.clear();
        service.insertMissingByWordIds(20L, List.of(7L));
        assertEquals(3, stored.get(0).getLevelNo(), "删除末关全部词后必须越过历史高水位");
        historyMax.set(3);
        frozen.set(true);
        service.insertMissingByWordIds(20L, List.of(8L));
        assertEquals(4, stored.get(1).getLevelNo(), "未满但已产生进度的末关必须冻结");
    }

    /** 公共原始关系入口只允许唯一规范词落库，含 articleId=0 的重复请求均幂等。 */
    private static void shouldCanonicalizeAndDeduplicateRawRelationInsert()
    {
        List<EngArticleWordRel> stored = new ArrayList<>();
        EngArticleWordRelMapper mapper = proxy(EngArticleWordRelMapper.class, (method, args) -> {
            if ("lockArticle".equals(method)) return 20L;
            if ("selectMaxLevelNo".equals(method) || "countByArticleAndLevel".equals(method)) return 0;
            if ("selectExistingWordNames".equals(method))
            {
                @SuppressWarnings("unchecked") List<String> names = (List<String>) args[1];
                return stored.stream().filter(item -> item.getArticleId().equals(args[0]))
                        .map(EngArticleWordRel::getWordName).filter(names::contains).toList();
            }
            if ("insertEngArticleWordRel".equals(method)) { stored.add((EngArticleWordRel) args[0]); return 1; }
            return defaultValue(methodReturnType(EngArticleWordRelMapper.class, method));
        });
        EngWord color = word(9L, "color");
        EngWordMapper wordMapper = proxy(EngWordMapper.class, (method, args) -> {
            if ("selectEngWordByWordName".equals(method))
                return "colour".equals(args[0]) || "color".equals(args[0]) ? List.of(color) : List.of();
            return defaultValue(methodReturnType(EngWordMapper.class, method));
        });
        EngArticleWordRelServiceImpl service = new EngArticleWordRelServiceImpl(mapper,
                proxy(EngSentenceWordRelMapper.class,
                        (method, args) -> defaultValue(methodReturnType(EngSentenceWordRelMapper.class, method))),
                proxy(EngArticleLevelProgressMapper.class, (method, args) -> 0), wordMapper);
        EngArticleWordRel article = new EngArticleWordRel(); article.setArticleId(20L); article.setWordName("colour");
        assertEquals(1, service.insertEngArticleWordRel(article), "别名应解析后新增");
        assertEquals("color", stored.get(0).getWordName(), "原始别名不得落库");
        EngArticleWordRel duplicate = new EngArticleWordRel(); duplicate.setArticleId(20L); duplicate.setWordName("color");
        assertEquals(0, service.insertEngArticleWordRel(duplicate), "真实文章重复关系应幂等");
        EngArticleWordRel wordBook = new EngArticleWordRel(); wordBook.setArticleId(0L); wordBook.setWordName("colour");
        assertEquals(1, service.insertEngArticleWordRel(wordBook), "articleId=0 应兼容规范词关系");
        assertEquals(0, service.insertEngArticleWordRel(wordBook), "articleId=0 重复关系也应幂等");
        EngArticleWordRel invalid = new EngArticleWordRel(); invalid.setArticleId(20L); invalid.setWordName("unknown");
        try { service.insertEngArticleWordRel(invalid); throw new AssertionError("未收录文本不得落库"); }
        catch (ServiceException expected) { assertTrue(expected.getMessage().contains("未收录"), "应返回规范词校验错误"); }
    }

    /** 句子内容删除词形或删除句子时，按剩余引用决定是否保留文章词。 */
    private static void shouldCleanArticleWordWhenSentenceChangesOrIsDeleted()
    {
        Harness updateLast = new Harness(true, 0);
        EngSentence withoutApple = sentence();
        withoutApple.setContent("Only bananas remain.");
        updateLast.service.updateEngSentence(withoutApple);
        assertEquals(1, updateLast.relationDeletes.get(), "词形消失应删除句子关系");
        assertEquals(1, updateLast.articleDeletes.get(), "最后引用消失应删除文章词");

        Harness updateWithOther = new Harness(true, 1);
        updateWithOther.service.updateEngSentence(withoutApple);
        assertEquals(1, updateWithOther.relationDeletes.get(), "词形消失仍应删除当前句子关系");
        assertEquals(0, updateWithOther.articleDeletes.get(), "仍有其他引用时应保留文章词");

        Harness deleteLast = new Harness(true, 0);
        deleteLast.service.deleteEngSentenceByIds(new Long[] {10L});
        assertEquals(1, deleteLast.relationDeletes.get(), "删除句子应清理其单词关系");
        assertEquals(1, deleteLast.articleDeletes.get(), "删除最后引用句子应删除文章词");

        Harness deleteWithOther = new Harness(true, 1);
        deleteWithOther.service.deleteEngSentenceByIds(new Long[] {10L});
        assertEquals(0, deleteWithOther.articleDeletes.get(), "删除句子后仍有引用应保留文章词");

        Harness moved = new Harness(true, 0);
        EngSentence movedSentence = sentence();
        movedSentence.setArticleId(21L);
        moved.service.updateEngSentence(movedSentence);
        assertEquals(1, moved.articleBatchInserts.get(), "句子移动后必须经统一分关服务补充新文章词");
    }

    /** prototype 回退只有唯一候选时可选，多个规范词候选必须标记为歧义。 */
    private static void shouldResolvePrototypeCandidateAndRejectAmbiguity()
    {
        Harness harness = new Harness(false, 0);
        EngWord go = word(3L, "go");
        go.setPrototype("went");
        harness.sentence.setContent("They went home with apples.");
        harness.prototypeWords.add(go);
        harness.candidates.put("apples", List.of(harness.apple, harness.banana));

        List<EngSentenceSegmentVo> words = harness.service.selectWordOptions(10L).getSegments().stream()
                .filter(segment -> "WORD".equals(segment.getType())).toList();
        assertEquals(3L, segment(words, "went").getWordId(), "唯一 prototype 候选应解析到规范词");
        assertTrue(segment(words, "went").isSelectable(), "唯一 prototype 候选应允许选择");
        assertEquals(null, segment(words, "apples").getWordId(), "多个候选不得擅自选择规范词");
        assertTrue(!segment(words, "apples").isSelectable(), "歧义词形应禁止选择");
    }

    /** Mapper 写入失败后不得继续后续副作用，公开写入口必须保留事务边界。 */
    private static void shouldStopAfterMapperFailureAndExposeTransactionBoundary()
    {
        Harness harness = new Harness(false, 0);
        harness.failRelationInsert = true;
        harness.articleWordExists = false;
        EngSentenceWordUpdateDto request = new EngSentenceWordUpdateDto();
        request.setWordIds(List.of(1L));
        try
        {
            harness.service.updateWordOptions(10L, request);
            throw new AssertionError("关系 Mapper 异常必须向上抛出");
        }
        catch (IllegalStateException expected)
        {
            assertEquals("relation insert failed", expected.getMessage(), "应保留原始写入异常");
        }
        assertEquals(0, harness.articleInserts.get(), "句子关系写入失败后不得继续新增文章词");
        try
        {
            assertTrue(EngSentenceServiceImpl.class
                    .getMethod("updateWordOptions", Long.class, EngSentenceWordUpdateDto.class)
                    .isAnnotationPresent(Transactional.class), "关系替换入口必须由事务包裹");
            assertTrue(EngSentenceServiceImpl.class.getMethod("updateEngSentence", EngSentence.class)
                    .isAnnotationPresent(Transactional.class), "句子修改入口必须由事务包裹");
            assertTrue(EngSentenceServiceImpl.class.getMethod("deleteEngSentenceByIds", Long[].class)
                    .isAnnotationPresent(Transactional.class), "句子删除入口必须由事务包裹");
        }
        catch (ReflectiveOperationException exception)
        {
            throw new AssertionError("事务入口反射检查失败", exception);
        }
    }

    /** 多词句子的读取和保存均应使用固定次数的批量 Mapper 调用。 */
    private static void shouldUseConstantMapperCallsWhenSavingMultipleWords()
    {
        Harness harness = new Harness(false, 0);
        harness.sentence.setContent("Apple banana apples banana.");

        harness.service.selectWordOptions(10L);
        assertEquals(1, harness.wordBatchQueries.get(), "读取多词句子只能批量查询一次词形");

        harness.wordBatchQueries.set(0);
        EngSentenceWordUpdateDto request = new EngSentenceWordUpdateDto();
        request.setWordIds(List.of(1L, 2L));
        EngSentenceWordOptionsVo result = harness.service.updateWordOptions(10L, request);

        assertEquals(1, harness.wordBatchQueries.get(), "保存时不得重复解析或逐词查询");
        assertEquals(1, harness.relationBatchWrites.get(), "多个句子词关系应一次批量写入");
        assertEquals(1, harness.articleBatchInserts.get(), "多个文章词关系应一次批量补充");
        long selectedWords = result.getSegments().stream()
                .filter(segment -> "WORD".equals(segment.getType()) && segment.isSelected()).count();
        assertEquals(4L, selectedWords, "保存响应应复用解析结果并回显全部出现位置");
    }

    /** 单测用内存 Mapper 桩。 */
    private static class Harness
    {
        private final EngWord apple = word(1L, "apple");
        private final EngWord banana = word(2L, "banana");
        private final EngSentence sentence = sentence();
        private final List<EngSentenceWordRel> relations = new ArrayList<>();
        private final Map<String, List<EngWord>> candidates = new java.util.HashMap<>();
        private final List<EngWord> prototypeWords = new ArrayList<>();
        private final AtomicInteger relationWrites = new AtomicInteger();
        private final AtomicInteger relationDeletes = new AtomicInteger();
        private final AtomicInteger articleDeletes = new AtomicInteger();
        private final AtomicInteger articleInserts = new AtomicInteger();
        private final AtomicInteger sentenceWrites = new AtomicInteger();
        private final AtomicInteger wordBatchQueries = new AtomicInteger();
        private final AtomicInteger relationBatchWrites = new AtomicInteger();
        private final AtomicInteger articleBatchInserts = new AtomicInteger();
        private final int remainingReferenceCount;
        private boolean owned = true;
        private boolean articleOwned = true;
        private boolean failRelationInsert;
        private boolean articleWordExists = true;
        private final EngSentenceServiceImpl service;

        Harness(boolean withExistingRelation, int remainingReferenceCount)
        {
            this.remainingReferenceCount = remainingReferenceCount;
            if (withExistingRelation)
            {
                EngSentenceWordRel relation = new EngSentenceWordRel();
                relation.setId(100L);
                relation.setSentenceId(10L);
                relation.setArticleId(20L);
                relation.setWordId(1L);
                relation.setWordName("apple");
                relation.setMatchedText("apples");
                relations.add(relation);
            }
            candidates.put("apple", List.of(apple));
            candidates.put("apples", List.of(apple));
            candidates.put("banana", List.of(banana));
            service = new EngSentenceServiceImpl(sentenceMapper(), null, relationMapper(), wordMapper(),
                    articleWordMapper(), articleMapper(), articleWordRelService());
        }

        private IEngArticleWordRelService articleWordRelService()
        {
            return proxy(IEngArticleWordRelService.class, (method, args) -> {
                if ("insertMissingByWordIds".equals(method))
                {
                    articleBatchInserts.incrementAndGet();
                    if (articleWordExists) return 0;
                    @SuppressWarnings("unchecked")
                    List<Long> wordIds = (List<Long>) args[1];
                    articleInserts.addAndGet(wordIds.size());
                    return wordIds.size();
                }
                return defaultValue(methodReturnType(IEngArticleWordRelService.class, method));
            });
        }

        private EngSentenceMapper sentenceMapper()
        {
            return proxy(EngSentenceMapper.class, (method, args) -> {
                if ("selectEngSentenceById".equals(method))
                {
                    return owned ? sentence : null;
                }
                if ("updateEngSentence".equals(method) || "insertEngSentence".equals(method))
                {
                    sentenceWrites.incrementAndGet();
                    return 1;
                }
                if ("deleteEngSentenceByIds".equals(method))
                {
                    sentenceWrites.incrementAndGet();
                    return 1;
                }
                if ("countByArticleId".equals(method)) return 1L;
                return defaultValue(methodReturnType(EngSentenceMapper.class, method));
            });
        }

        private EngSentenceWordRelMapper relationMapper()
        {
            return proxy(EngSentenceWordRelMapper.class, (method, args) -> {
                if ("selectBySentenceId".equals(method))
                {
                    return new ArrayList<>(relations);
                }
                if ("deleteBySentenceAndWord".equals(method))
                {
                    relationDeletes.incrementAndGet();
                    relations.removeIf(relation -> relation.getWordId().equals(args[1]));
                    return 1;
                }
                if ("deleteBySentenceAndWordIds".equals(method))
                {
                    @SuppressWarnings("unchecked")
                    List<Long> wordIds = (List<Long>) args[1];
                    int before = relations.size();
                    relations.removeIf(relation -> wordIds.contains(relation.getWordId()));
                    int deleted = before - relations.size();
                    relationDeletes.addAndGet(deleted);
                    return deleted;
                }
                if ("deleteBySentenceIds".equals(method))
                {
                    relationDeletes.addAndGet(relations.size());
                    relations.clear();
                    return 1;
                }
                if ("countByArticleAndWord".equals(method))
                {
                    return remainingReferenceCount;
                }
                if ("insert".equals(method))
                {
                    if (failRelationInsert) throw new IllegalStateException("relation insert failed");
                    relationWrites.incrementAndGet();
                    return 1;
                }
                if ("upsertBatch".equals(method))
                {
                    if (failRelationInsert) throw new IllegalStateException("relation insert failed");
                    @SuppressWarnings("unchecked")
                    List<EngSentenceWordRel> batch = (List<EngSentenceWordRel>) args[0];
                    relationBatchWrites.incrementAndGet();
                    relationWrites.addAndGet(batch.size());
                    for (EngSentenceWordRel relation : batch)
                    {
                        relations.removeIf(existing -> existing.getWordId().equals(relation.getWordId()));
                        relations.add(relation);
                    }
                    return batch.size();
                }
                if ("updateMatchedText".equals(method))
                {
                    relationWrites.incrementAndGet();
                    return 1;
                }
                return defaultValue(methodReturnType(EngSentenceWordRelMapper.class, method));
            });
        }

        private EngWordMapper wordMapper()
        {
            return proxy(EngWordMapper.class, (method, args) -> {
                if ("selectWordFormMatches".equals(method))
                {
                    wordBatchQueries.incrementAndGet();
                    @SuppressWarnings("unchecked")
                    List<String> forms = (List<String>) args[0];
                    List<EngWordFormMatchVo> matches = new ArrayList<>();
                    for (String form : forms)
                    {
                        List<EngWord> words = candidates.get(form);
                        if (words == null)
                        {
                            words = prototypeWords.stream()
                                    .filter(word -> form.equals(word.getPrototype())).toList();
                        }
                        for (EngWord word : words)
                        {
                            EngWordFormMatchVo match = new EngWordFormMatchVo();
                            match.setLookupKey(form);
                            match.setWordId(word.getId());
                            match.setWordName(word.getWordName());
                            matches.add(match);
                        }
                    }
                    return matches;
                }
                if ("selectEngWordByWordName".equals(method))
                {
                    String form = (String) args[0];
                    if (candidates.containsKey(form)) return candidates.get(form);
                    return prototypeWords.stream().filter(word -> form.equals(word.getPrototype())).toList();
                }
                if ("selectEngWordById".equals(method))
                {
                    return Long.valueOf(1L).equals(args[0]) ? apple : banana;
                }
                return defaultValue(methodReturnType(EngWordMapper.class, method));
            });
        }

        private EngArticleWordRelMapper articleWordMapper()
        {
            return proxy(EngArticleWordRelMapper.class, (method, args) -> {
                if ("selectEngArticleWordRelList".equals(method))
                {
                    if (!articleWordExists) return List.of();
                    EngArticleWordRel relation = new EngArticleWordRel();
                    relation.setId(30L);
                    relation.setArticleId(20L);
                    relation.setWordName("apple");
                    return List.of(relation);
                }
                if ("deleteEngArticleWordRelByIds".equals(method))
                {
                    articleDeletes.incrementAndGet();
                    return 1;
                }
                if ("deleteUnusedByWordIds".equals(method))
                {
                    @SuppressWarnings("unchecked")
                    List<Long> wordIds = (List<Long>) args[1];
                    if (remainingReferenceCount == 0 && !wordIds.isEmpty())
                    {
                        articleDeletes.addAndGet(wordIds.size());
                        return wordIds.size();
                    }
                    return 0;
                }
                if ("insertEngArticleWordRel".equals(method))
                {
                    articleInserts.incrementAndGet();
                    return 1;
                }
                if ("insertMissingByWordIds".equals(method))
                {
                    articleBatchInserts.incrementAndGet();
                    if (articleWordExists) return 0;
                    @SuppressWarnings("unchecked")
                    List<Long> wordIds = (List<Long>) args[1];
                    articleInserts.addAndGet(wordIds.size());
                    return wordIds.size();
                }
                return defaultValue(methodReturnType(EngArticleWordRelMapper.class, method));
            });
        }

        private EngArticleMapper articleMapper()
        {
            return proxy(EngArticleMapper.class, (method, args) -> {
                if ("selectEngArticleById".equals(method) && articleOwned)
                {
                    EngArticle article = new EngArticle();
                    article.setId((Long) args[0]);
                    return article;
                }
                return defaultValue(methodReturnType(EngArticleMapper.class, method));
            });
        }
    }

    /** 通过方法名取得接口方法返回类型。 */
    private static Class<?> methodReturnType(Class<?> type, String methodName)
    {
        for (java.lang.reflect.Method method : type.getMethods())
        {
            if (method.getName().equals(methodName))
            {
                return method.getReturnType();
            }
        }
        return Object.class;
    }

    /** 创建接口代理。 */
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation)
    {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> invocation.invoke(method.getName(), args));
    }

    /** 动态代理调用处理器。 */
    @FunctionalInterface
    private interface Invocation
    {
        Object invoke(String method, Object[] args);
    }

    private static EngWord word(Long id, String name)
    {
        EngWord word = new EngWord();
        word.setId(id);
        word.setWordName(name);
        return word;
    }

    private static EngSentence sentence()
    {
        EngSentence sentence = new EngSentence();
        sentence.setId(10L);
        sentence.setArticleId(20L);
        sentence.setContent("Pineapple and apples; Apple pie.");
        return sentence;
    }

    private static EngSentenceSegmentVo segment(List<EngSentenceSegmentVo> segments, String text)
    {
        return segments.stream().filter(segment -> text.equals(segment.getText())).findFirst()
                .orElseThrow(() -> new AssertionError("缺少片段：" + text));
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

    private static Object defaultValue(Class<?> type)
    {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == long.class) return 0L;
        return 0;
    }

    private static void assertTrue(boolean value, String message)
    {
        if (!value) throw new AssertionError(message);
    }

    /** 断言操作因文章或句子归属校验失败。 */
    private static void assertServiceException(Runnable action, String message)
    {
        try
        {
            action.run();
            throw new AssertionError(message);
        }
        catch (ServiceException expected)
        {
            assertTrue(expected.getMessage().contains("无权操作"), message + "应返回统一权限错误");
        }
    }

    private static void assertEquals(Object expected, Object actual, String message)
    {
        if (!java.util.Objects.equals(expected, actual))
        {
            throw new AssertionError(message + "，期望=" + expected + "，实际=" + actual);
        }
    }
}
