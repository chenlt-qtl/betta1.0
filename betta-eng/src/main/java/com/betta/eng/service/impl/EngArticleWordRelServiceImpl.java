package com.betta.eng.service.impl;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.betta.common.exception.ServiceException;
import com.betta.common.utils.SecurityUtils;
import com.betta.eng.domain.EngArticleWordRel;
import com.betta.eng.domain.EngWord;
import com.betta.eng.mapper.EngArticleWordRelMapper;
import com.betta.eng.mapper.EngArticleLevelProgressMapper;
import com.betta.eng.mapper.EngSentenceWordRelMapper;
import com.betta.eng.mapper.EngWordMapper;
import com.betta.eng.service.IEngArticleWordRelService;

/** 文章单词关系业务实现，负责用户隔离和关系审计信息。 */
@Service
public class EngArticleWordRelServiceImpl implements IEngArticleWordRelService
{
    private final EngArticleWordRelMapper mapper;
    private final EngSentenceWordRelMapper sentenceWordRelMapper;
    private final EngArticleLevelProgressMapper levelProgressMapper;
    private final EngWordMapper wordMapper;

    /** 创建关系服务；mapper 负责关系数据库操作。 */
    public EngArticleWordRelServiceImpl(EngArticleWordRelMapper mapper,
            EngSentenceWordRelMapper sentenceWordRelMapper,
            EngArticleLevelProgressMapper levelProgressMapper, EngWordMapper wordMapper)
    {
        this.mapper = mapper;
        this.sentenceWordRelMapper = sentenceWordRelMapper;
        this.levelProgressMapper = levelProgressMapper;
        this.wordMapper = wordMapper;
    }

    @Override
    public EngArticleWordRel selectEngArticleWordRelById(Long id)
    {
        return mapper.selectEngArticleWordRelById(id, SecurityUtils.getUsername());
    }

    @Override
    public List<EngArticleWordRel> selectEngArticleWordRelList(EngArticleWordRel rel)
    {
        rel.setCreateBy(SecurityUtils.getUsername());
        return mapper.selectEngArticleWordRelList(rel);
    }

    @Override
    @Transactional
    public int insertEngArticleWordRel(EngArticleWordRel rel)
    {
        if (rel == null || rel.getArticleId() == null || rel.getWordName() == null
                || rel.getWordName().isBlank())
            throw new ServiceException("文章和单词不能为空");
        String username = SecurityUtils.getUsername();
        rel.setCreateBy(username);
        if (rel.getArticleId() != null && rel.getArticleId() > 0)
        {
            if (mapper.lockArticle(rel.getArticleId(), username) == null)
                throw new ServiceException("文章不存在或无权操作");
        }
        EngWord word = resolveCanonicalWord(rel.getWordName());
        rel.setWordName(word.getWordName());
        if (!mapper.selectExistingWordNames(rel.getArticleId(), List.of(rel.getWordName()), username).isEmpty())
            return 0;
        if (rel.getArticleId() > 0)
        {
            rel.setLevelNo(nextLevelNo(rel.getArticleId()));
        }
        return mapper.insertEngArticleWordRel(rel);
    }

    @Override
    @Transactional
    public int deleteEngArticleWordRelByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            return 0;
        }
        String username = SecurityUtils.getUsername();
        List<EngArticleWordRel> relations = Arrays.stream(ids)
                .map(id -> mapper.selectEngArticleWordRelById(id, username))
                .filter(Objects::nonNull).toList();
        for (var entry : relations.stream().collect(Collectors.groupingBy(
                EngArticleWordRel::getArticleId)).entrySet())
        {
            List<String> wordNames = entry.getValue().stream().map(EngArticleWordRel::getWordName)
                    .filter(Objects::nonNull).distinct().toList();
            if (!wordNames.isEmpty())
            {
                sentenceWordRelMapper.deleteByArticleAndWordNames(entry.getKey(), wordNames, username);
            }
        }
        return mapper.deleteEngArticleWordRelByIds(ids, username);
    }

    @Override
    @Transactional
    public int deleteByArticle(Long articleId)
    {
        String username = SecurityUtils.getUsername();
        sentenceWordRelMapper.deleteByArticle(articleId, username);
        return mapper.deleteByArticleId(articleId, username);
    }

    @Override
    @Transactional
    public int insertMissingByWordIds(Long articleId, List<Long> wordIds)
    {
        if (articleId == null || wordIds == null || wordIds.isEmpty()) return 0;
        String username = SecurityUtils.getUsername();
        if (articleId > 0 && mapper.lockArticle(articleId, username) == null)
            throw new ServiceException("文章不存在或无权操作");
        List<Long> orderedIds = new ArrayList<>(new LinkedHashSet<>(wordIds));
        Map<Long, EngWord> words = wordMapper.selectByIds(orderedIds).stream()
                .collect(Collectors.toMap(EngWord::getId, item -> item));
        List<String> orderedNames = orderedIds.stream().map(words::get).filter(Objects::nonNull)
                .map(EngWord::getWordName).distinct().toList();
        if (orderedNames.isEmpty()) return 0;
        Set<String> existing = new HashSet<>(mapper.selectExistingWordNames(
                articleId, orderedNames, username));
        LevelAllocation allocation = articleId > 0 ? currentAllocation(articleId) : new LevelAllocation(0, 0);
        int levelNo = allocation.levelNo();
        int levelSize = allocation.levelSize();
        List<EngArticleWordRel> additions = new ArrayList<>();
        for (String wordName : orderedNames)
        {
            if (existing.contains(wordName)) continue;
            if (levelSize >= 5) { levelNo++; levelSize = 0; }
            EngArticleWordRel relation = new EngArticleWordRel();
            relation.setArticleId(articleId);
            relation.setWordName(wordName);
            relation.setLevelNo(articleId > 0 ? levelNo : null);
            relation.setCreateBy(username);
            additions.add(relation);
            if (articleId > 0) levelSize++;
        }
        return additions.isEmpty() ? 0 : mapper.insertBatch(additions);
    }

    /** 仅允许未满且从未产生关卡进度的最后一关继续追加。 */
    private int nextLevelNo(Long articleId)
    {
        return currentAllocation(articleId).levelNo();
    }

    /** 结合现存关系与历史进度高水位，防止已删关卡号被复用。 */
    private LevelAllocation currentAllocation(Long articleId)
    {
        int relationMax = value(mapper.selectMaxLevelNo(articleId));
        int progressMax = value(levelProgressMapper.selectMaxLevelNoByArticle(articleId));
        if (progressMax > relationMax) return new LevelAllocation(progressMax + 1, 0);
        if (relationMax == 0) return new LevelAllocation(progressMax + 1, 0);
        int levelSize = mapper.countByArticleAndLevel(articleId, relationMax);
        if (levelSize >= 5 || levelProgressMapper.countAnyProgress(articleId, relationMax) > 0)
            return new LevelAllocation(Math.max(relationMax, progressMax) + 1, 0);
        return new LevelAllocation(relationMax, levelSize);
    }

    /** 将公共入口的词形解析为唯一规范词，禁止原始文本落库。 */
    private EngWord resolveCanonicalWord(String input)
    {
        List<EngWord> matches = wordMapper.selectEngWordByWordName(
                input.trim().toLowerCase(Locale.ROOT));
        if (matches == null || matches.size() != 1)
            throw new ServiceException(matches == null || matches.isEmpty() ? "单词未收录" : "单词词形存在歧义");
        return matches.get(0);
    }

    private int value(Integer number) { return number == null ? 0 : number; }
    private record LevelAllocation(int levelNo, int levelSize) {}
}
