package com.betta.eng.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.betta.common.utils.SecurityUtils;
import com.betta.eng.domain.EngIcibaSentence;
import com.betta.eng.mapper.EngIcibaSentenceMapper;
import com.betta.eng.service.IEngIcibaSentenceService;

/** 词典例句业务实现，负责审计信息设置。 */
@Service
public class EngIcibaSentenceServiceImpl implements IEngIcibaSentenceService
{
    private final EngIcibaSentenceMapper mapper;

    /** 创建例句服务；mapper 负责例句数据库操作。 */
    public EngIcibaSentenceServiceImpl(EngIcibaSentenceMapper mapper)
    {
        this.mapper = mapper;
    }

    @Override
    public List<EngIcibaSentence> selectEngIcibaSentenceList(EngIcibaSentence sentence)
    {
        return mapper.selectEngIcibaSentenceList(sentence);
    }

    @Override
    public int insertEngIcibaSentence(EngIcibaSentence sentence)
    {
        sentence.setCreateBy(SecurityUtils.getUsername());
        return mapper.insertEngIcibaSentence(sentence);
    }

    @Override
    public void deleteByWordId(Long wordId)
    {
        mapper.deleteByWordId(wordId);
    }
}
