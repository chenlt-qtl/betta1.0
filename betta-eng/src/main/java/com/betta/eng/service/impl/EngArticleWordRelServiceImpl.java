package com.betta.eng.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.betta.common.utils.SecurityUtils;
import com.betta.eng.domain.EngArticleWordRel;
import com.betta.eng.mapper.EngArticleWordRelMapper;
import com.betta.eng.service.IEngArticleWordRelService;

/** 文章单词关系业务实现，负责用户隔离和关系审计信息。 */
@Service
public class EngArticleWordRelServiceImpl implements IEngArticleWordRelService
{
    private final EngArticleWordRelMapper mapper;

    /** 创建关系服务；mapper 负责关系数据库操作。 */
    public EngArticleWordRelServiceImpl(EngArticleWordRelMapper mapper)
    {
        this.mapper = mapper;
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
    public int insertEngArticleWordRel(EngArticleWordRel rel)
    {
        rel.setCreateBy(SecurityUtils.getUsername());
        return mapper.insertEngArticleWordRel(rel);
    }

    @Override
    public int deleteEngArticleWordRelByIds(Long[] ids)
    {
        return ids == null || ids.length == 0 ? 0
                : mapper.deleteEngArticleWordRelByIds(ids, SecurityUtils.getUsername());
    }

    @Override
    public int deleteByArticle(Long articleId)
    {
        return mapper.deleteByArticleId(articleId, SecurityUtils.getUsername());
    }
}
