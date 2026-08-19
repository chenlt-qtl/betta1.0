package com.betta.eng.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.betta.common.exception.ServiceException;
import com.betta.common.utils.SecurityUtils;
import com.betta.eng.domain.EngArticleGroup;
import com.betta.eng.mapper.EngArticleGroupMapper;
import com.betta.eng.service.IEngArticleGroupService;

/** 文章分组业务实现，负责审计字段和删除前业务校验。 */
@Service
public class EngArticleGroupServiceImpl implements IEngArticleGroupService
{
    private final EngArticleGroupMapper mapper;

    /** 创建分组服务；mapper 负责分组数据库操作。 */
    public EngArticleGroupServiceImpl(EngArticleGroupMapper mapper)
    {
        this.mapper = mapper;
    }

    @Override
    public EngArticleGroup selectEngArticleGroupById(Long id)
    {
        return mapper.selectEngArticleGroupById(id);
    }

    @Override
    public List<EngArticleGroup> selectEngArticleGroupList(EngArticleGroup group)
    {
        return mapper.selectEngArticleGroupList(group);
    }

    @Override
    public int insertEngArticleGroup(EngArticleGroup group)
    {
        group.setCreateBy(SecurityUtils.getUsername());
        return mapper.insertEngArticleGroup(group);
    }

    @Override
    public int updateEngArticleGroup(EngArticleGroup group)
    {
        group.setUpdateBy(SecurityUtils.getUsername());
        return mapper.updateEngArticleGroup(group);
    }

    @Override
    public int deleteEngArticleGroupByIds(Long[] ids)
    {
        for (Long id : ids)
        {
            if (mapper.countArticleByGroupId(id) > 0)
            {
                throw new ServiceException("分组下存在文章，不能删除");
            }
        }
        return mapper.deleteEngArticleGroupByIds(ids);
    }
}
