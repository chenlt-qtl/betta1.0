package com.betta.eng.service.impl;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.betta.common.exception.ServiceException;
import com.betta.common.utils.SecurityUtils;
import com.betta.common.utils.StringUtils;
import com.betta.eng.domain.EngArticleWordRel;
import com.betta.eng.domain.EngIcibaSentence;
import com.betta.eng.domain.EngWord;
import com.betta.eng.domain.vo.EngWordVo;
import com.betta.eng.mapper.EngWordMapper;
import com.betta.eng.service.IEngArticleWordRelService;
import com.betta.eng.service.IEngIcibaSentenceService;
import com.betta.eng.service.IEngSentenceService;
import com.betta.eng.service.IEngUserScoreService;
import com.betta.eng.service.IEngWordService;
import com.betta.eng.utils.dict.DictUtils;
import com.betta.eng.utils.FamiliarityCalculator;

/** 单词业务实现，负责去重、详情聚合以及文章关系的全量同步。 */
@Service
public class EngWordServiceImpl implements IEngWordService
{
    private final EngWordMapper mapper;
    private final IEngArticleWordRelService relService;
    private final IEngUserScoreService scoreService;
    private final IEngSentenceService sentenceService;
    private final IEngIcibaSentenceService dictionarySentenceService;
    private final DictUtils dictUtils;

    /** 创建单词服务；参数依次负责单词、关系、成绩、句子、例句和词典访问。 */
    public EngWordServiceImpl(EngWordMapper mapper, IEngArticleWordRelService relService,
            IEngUserScoreService scoreService, IEngSentenceService sentenceService,
            IEngIcibaSentenceService dictionarySentenceService, DictUtils dictUtils)
    {
        this.mapper = mapper;
        this.relService = relService;
        this.scoreService = scoreService;
        this.sentenceService = sentenceService;
        this.dictionarySentenceService = dictionarySentenceService;
        this.dictUtils = dictUtils;
    }

    @Override
    public EngWord selectEngWordById(Long id)
    {
        return mapper.selectEngWordById(id);
    }

    @Override
    public List<EngWordVo> selectEngWordList(EngWord word)
    {
        return mapper.selectEngWordList(word);
    }

    @Override
    public List<EngWordVo> selectWordListByArticle(Long articleId)
    {
        List<EngWordVo> result = mapper.selectWordListByArticleId(articleId, SecurityUtils.getUsername());
        applyEffectiveFamiliarity(result);
        return result;
    }


    @Override
    public void updateFamiliarity(String wordName, int delta)
    {
        scoreService.updateEngUserScore(wordName, delta);
    }

    /** 将文章单词携带的基础值换算为查询时刻的有效熟悉度。 */
    private void applyEffectiveFamiliarity(List<EngWordVo> words)
    {
        Date now = new Date();
        for (EngWordVo word : words)
        {
            word.setFamiliarity(FamiliarityCalculator.effective(word.getBaseFamiliarity(),
                    word.getLastReviewTime(), now));
        }
    }

    @Override
    public int addEngWord(EngWord word)
    {
        String normalized = normalize(word == null ? null : word.getWordName());
        if (!mapper.selectEngWordByWordName(normalized).isEmpty())
        {
            throw new ServiceException("单词 " + normalized + " 已存在");
        }
        word.setWordName(normalized);
        word.setCreateBy(SecurityUtils.getUsername());
        return mapper.insertEngWord(word);
    }

    @Override
    @Transactional
    public EngWordVo getWordVo(String wordName)
    {
        EngWord word = getOrCreate(normalize(wordName));
        EngWordVo result = new EngWordVo();
        BeanUtils.copyProperties(word, result);
        EngIcibaSentence sentence = new EngIcibaSentence();
        sentence.setWordId(word.getId());
        result.setIcibaSentenceList(dictionarySentenceService.selectEngIcibaSentenceList(sentence));
        result.setSentenceList(sentenceService.selectByWordTop10(word));
        EngArticleWordRel rel = new EngArticleWordRel();
        // articleId=0 专用于当前用户生词收藏，文章关系不能作为收藏状态返回。
        rel.setArticleId(0L);
        rel.setWordName(word.getWordName());
        List<EngArticleWordRel> rels = relService.selectEngArticleWordRelList(rel);
        if (!rels.isEmpty())
        {
            result.setRelId(rels.get(0).getId());
        }
        return result;
    }

    @Override
    public EngWord getWordFromApi(String wordName)
    {
        return dictUtils.getWord(normalize(wordName));
    }

    @Override
    public int updateEngWord(EngWord word)
    {
        if (word == null || word.getId() == null)
        {
            throw new ServiceException("单词主键不能为空");
        }
        word.setWordName(normalize(word.getWordName()));
        word.setUpdateBy(SecurityUtils.getUsername());
        return mapper.updateEngWord(word);
    }

    @Override
    @Transactional
    public void deleteEngWordByIds(Long[] ids)
    {
        for (Long id : ids)
        {
            if (mapper.countStudyRecordWordRefs(id) > 0)
                throw new ServiceException("单词已被历史测试记录引用，不能删除");
            dictionarySentenceService.deleteByWordId(id);
            mapper.deleteEngWordById(id);
        }
    }

    @Override
    public List<EngWord> selectNewList(EngWord word)
    {
        return mapper.selectRelList(word, SecurityUtils.getUsername());
    }

    @Override
    @Transactional
    public void updateByArticle(List<String> words, Long articleId)
    {
        if (articleId == null || words == null)
        {
            throw new ServiceException("文章和单词列表不能为空");
        }
        EngArticleWordRel condition = new EngArticleWordRel();
        condition.setArticleId(articleId);
        List<EngArticleWordRel> old = relService.selectEngArticleWordRelList(condition);
        Map<String, Long> desired = new LinkedHashMap<>();
        for (String value : words)
        {
            if (StringUtils.isNotEmpty(value))
            {
                EngWord resolvedWord = getOrCreate(normalize(value));
                desired.putIfAbsent(normalize(resolvedWord.getWordName()), resolvedWord.getId());
            }
        }
        List<Long> removeIds = new ArrayList<>();
        for (EngArticleWordRel rel : old)
        {
            String oldWordName = StringUtils.isEmpty(rel.getWordName()) ? null : normalize(rel.getWordName());
            if (oldWordName == null || desired.remove(oldWordName) == null)
            {
                removeIds.add(rel.getId());
            }
        }
        if (!removeIds.isEmpty())
        {
            relService.deleteEngArticleWordRelByIds(removeIds.toArray(new Long[0]));
        }
        relService.insertMissingByWordIds(articleId, new ArrayList<>(desired.values()));
    }

    @Override
    @Transactional
    public void addArticleWord(Long articleId, String wordName)
    {
        String normalized = normalize(wordName);
        EngWord resolvedWord = getOrCreate(normalized);
        relService.insertMissingByWordIds(articleId, List.of(resolvedWord.getId()));
    }

    @Override
    @Transactional
    public List<String> addArticleWords(Long articleId, List<String> words)
    {
        if (articleId == null || words == null)
        {
            throw new ServiceException("文章和单词列表不能为空");
        }
        Set<String> normalizedInputs = new LinkedHashSet<>();
        for (String word : words)
        {
            if (StringUtils.isNotEmpty(word) && StringUtils.isNotEmpty(word.trim()))
            {
                normalizedInputs.add(normalize(word));
            }
        }
        if (normalizedInputs.isEmpty())
        {
            throw new ServiceException("单词列表不能为空");
        }

        List<String> missingWords = new ArrayList<>();
        Map<String, Long> canonicalWords = new LinkedHashMap<>();
        for (String normalizedInput : normalizedInputs)
        {
            try
            {
                EngWord resolvedWord = getOrCreate(normalizedInput);
                String canonicalWordName = normalize(resolvedWord.getWordName());
                canonicalWords.putIfAbsent(canonicalWordName, resolvedWord.getId());
            }
            catch (ServiceException exception)
            {
                // 未收录属于可跳过的业务结果；其他业务异常仍由事务统一回滚。
                if (!isMissingDictionaryWord(exception, normalizedInput))
                {
                    throw exception;
                }
                missingWords.add(normalizedInput);
            }
        }
        relService.insertMissingByWordIds(articleId, new ArrayList<>(canonicalWords.values()));
        return missingWords;
    }

    /** 判断本地词典查询异常是否仅表示 normalizedInput 未收录。 */
    private boolean isMissingDictionaryWord(ServiceException exception, String normalizedInput)
    {
        return ("词典未查询到单词：" + normalizedInput).equals(exception.getMessage());
    }

    /** 将 value 规范化为小写非空单词。 */
    private String normalize(String value)
    {
        if (StringUtils.isEmpty(value) || StringUtils.isEmpty(value.trim()))
        {
            throw new ServiceException("单词不能为空");
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC).trim().toLowerCase(Locale.ROOT);
    }

    /** 仅从本地数据库查询 normalized 对应的正式词条或别名。 */
    private EngWord getOrCreate(String normalized)
    {
        return dictUtils.getWord(normalized);
    }
}
