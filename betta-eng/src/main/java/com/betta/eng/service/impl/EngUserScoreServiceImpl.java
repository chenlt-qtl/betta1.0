package com.betta.eng.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.betta.common.exception.ServiceException;
import com.betta.common.utils.SecurityUtils;
import com.betta.common.utils.StringUtils;
import com.betta.eng.domain.EngSentence;
import com.betta.eng.domain.EngUserScore;
import com.betta.eng.domain.vo.EngUserScoreVo;
import com.betta.eng.mapper.EngUserScoreMapper;
import com.betta.eng.service.IEngSentenceService;
import com.betta.eng.service.IEngUserScoreService;

/** 用户成绩业务实现，负责用户隔离、例句组装和熟悉度增量累计。 */
@Service
public class EngUserScoreServiceImpl implements IEngUserScoreService
{
    private final EngUserScoreMapper mapper;
    private final IEngSentenceService sentenceService;

    /** 创建成绩服务；参数分别负责成绩数据访问和例句聚合。 */
    public EngUserScoreServiceImpl(EngUserScoreMapper mapper, IEngSentenceService sentenceService)
    {
        this.mapper = mapper;
        this.sentenceService = sentenceService;
    }

    @Override
    public List<EngUserScore> selectEngUserScoreList(EngUserScore score)
    {
        score.setUser(SecurityUtils.getUsername());
        return mapper.selectEngUserScoreList(score);
    }

    @Override
    public List<EngUserScoreVo> selectEngUserScoreVoList(EngUserScore score)
    {
        return mapper.selectEngUserScoreVo(score, SecurityUtils.getUsername());
    }

    @Override
    public List<EngUserScoreVo> selectEngUserScoreVoList(EngUserScore score, boolean withSentence)
    {
        List<EngUserScoreVo> result = selectEngUserScoreVoList(score);
        if (!withSentence)
        {
            return result;
        }
        for (EngUserScoreVo item : result)
        {
            EngSentence condition = new EngSentence();
            condition.setArticleId(item.getArticleId());
            condition.setContent(item.getWordName());
            List<EngSentence> sentences = sentenceService.selectEngSentenceList(condition);
            if (!sentences.isEmpty())
            {
                item.setSentence(sentences.get(0).getContent());
                item.setSentenceAcceptation(sentences.get(0).getAcceptation());
            }
        }
        return result;
    }

    @Override
    public int insertEngUserScore(EngUserScore score)
    {
        validate(score);
        String username = SecurityUtils.getUsername();
        score.setUser(username);
        score.setCreateBy(username);
        return mapper.insertEngUserScore(score);
    }

    @Override
    public int updateEngUserScore(EngUserScore score)
    {
        validate(score);
        String username = SecurityUtils.getUsername();
        score.setUser(username);
        score.setUpdateBy(username);
        return mapper.updateEngUserScore(score);
    }

    @Override
    @Transactional
    public void batchUpdate(List<EngUserScore> scores)
    {
        if (scores == null)
        {
            throw new ServiceException("成绩列表不能为空");
        }
        for (EngUserScore score : scores)
        {
            updateEngUserScore(score.getWordName(), score.getFamiliarity() == null ? 0 : score.getFamiliarity());
        }
    }

    @Override
    public int deleteEngUserScoreByIds(Long[] ids)
    {
        return mapper.deleteEngUserScoreByIds(ids, SecurityUtils.getUsername());
    }

    @Override
    public void updateEngUserScore(String wordName, int delta)
    {
        if (StringUtils.isEmpty(wordName))
        {
            throw new ServiceException("单词不能为空");
        }
        String username = SecurityUtils.getUsername();
        EngUserScore score = mapper.getByWordName(wordName, username);
        if (score == null)
        {
            score = new EngUserScore();
            score.setWordName(wordName);
            score.setFamiliarity(delta);
            insertEngUserScore(score);
            return;
        }
        score.setFamiliarity((score.getFamiliarity() == null ? 0 : score.getFamiliarity()) + delta);
        score.setUser(username);
        score.setUpdateBy(username);
        mapper.updateEngUserScore(score);
    }

    /** 校验 score 必填单词文本。 */
    private void validate(EngUserScore score)
    {
        if (score == null || StringUtils.isEmpty(score.getWordName()))
        {
            throw new ServiceException("单词不能为空");
        }
    }
}
