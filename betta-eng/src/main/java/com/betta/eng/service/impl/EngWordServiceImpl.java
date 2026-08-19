package com.betta.eng.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

/** 单词业务实现，负责去重、详情聚合以及文章关系的全量同步。 */
@Service
public class EngWordServiceImpl implements IEngWordService
{
    private final EngWordMapper mapper;
    private final IEngArticleWordRelService relService;
    private final IEngUserScoreService scoreService;
    private final IEngSentenceService sentenceService;
    private final IEngIcibaSentenceService icibaService;
    private final DictUtils dictUtils;

    /** 创建单词服务；参数依次负责单词、关系、成绩、句子、例句和词典访问。 */
    public EngWordServiceImpl(EngWordMapper mapper, IEngArticleWordRelService relService,
            IEngUserScoreService scoreService, IEngSentenceService sentenceService,
            IEngIcibaSentenceService icibaService, DictUtils dictUtils)
    {
        this.mapper = mapper;
        this.relService = relService;
        this.scoreService = scoreService;
        this.sentenceService = sentenceService;
        this.icibaService = icibaService;
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
        return mapper.selectWordListByArticleId(articleId);
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
    public EngWordVo getWordVo(String wordName)
    {
        EngWord word = getOrCreate(normalize(wordName));
        EngWordVo result = new EngWordVo();
        BeanUtils.copyProperties(word, result);
        EngIcibaSentence sentence = new EngIcibaSentence();
        sentence.setWordId(word.getId());
        result.setIcibaSentenceList(icibaService.selectEngIcibaSentenceList(sentence));
        result.setSentenceList(sentenceService.selectByWordTop10(word));
        EngArticleWordRel rel = new EngArticleWordRel();
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
            icibaService.deleteByWordId(id);
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
        Set<String> desired = new HashSet<>();
        for (String value : words)
        {
            if (StringUtils.isNotEmpty(value))
            {
                desired.add(normalize(value));
            }
        }
        List<Long> removeIds = new ArrayList<>();
        for (EngArticleWordRel rel : old)
        {
            if (!desired.remove(rel.getWordName().toLowerCase(Locale.ROOT)))
            {
                removeIds.add(rel.getId());
            }
        }
        if (!removeIds.isEmpty())
        {
            relService.deleteEngArticleWordRelByIds(removeIds.toArray(new Long[0]));
        }
        for (String value : desired)
        {
            addArticleWord(articleId, value);
        }
    }

    @Override
    public void addArticleWord(Long articleId, String wordName)
    {
        String normalized = normalize(wordName);
        getOrCreate(normalized);
        EngArticleWordRel condition = new EngArticleWordRel();
        condition.setArticleId(articleId);
        condition.setWordName(normalized);
        if (relService.selectEngArticleWordRelList(condition).isEmpty())
        {
            EngArticleWordRel rel = new EngArticleWordRel();
            rel.setArticleId(articleId);
            rel.setWordName(normalized);
            relService.insertEngArticleWordRel(rel);
        }
        else
        {
            scoreService.updateEngUserScore(normalized, -1);
        }
    }

    /** 将 value 规范化为小写非空单词。 */
    private String normalize(String value)
    {
        if (StringUtils.isEmpty(value) || StringUtils.isEmpty(value.trim()))
        {
            throw new ServiceException("单词不能为空");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /** 查询或通过词典创建 normalized 单词，并持久化词典例句。 */
    private EngWord getOrCreate(String normalized)
    {
        List<EngWord> list = mapper.selectEngWordByWordName(normalized);
        if (!list.isEmpty())
        {
            return list.get(0);
        }
        EngWordVo word = dictUtils.getWord(normalized);
        word.setCreateBy(SecurityUtils.getUsername());
        mapper.insertEngWord(word);
        if (word.getIcibaSentenceList() != null)
        {
            for (EngIcibaSentence sentence : word.getIcibaSentenceList())
            {
                sentence.setWordId(word.getId());
                icibaService.insertEngIcibaSentence(sentence);
            }
        }
        return word;
    }
}
