package com.betta.eng.service.impl;

import com.betta.eng.domain.EngStudyRecord;
import com.betta.eng.domain.vo.EngStudyAdminUserVo;
import com.betta.eng.mapper.EngStudyAdminMapper;
import com.betta.eng.service.IEngStudyAdminService;
import java.util.List;
import org.springframework.stereotype.Service;

/** 管理员积分只读查询服务实现。 */
@Service
public class EngStudyAdminServiceImpl implements IEngStudyAdminService
{
    private final EngStudyAdminMapper mapper;

    /** 创建管理员积分查询服务。 */
    public EngStudyAdminServiceImpl(EngStudyAdminMapper mapper)
    {
        this.mapper = mapper;
    }

    @Override
    public List<EngStudyAdminUserVo> selectUserScoreList(EngStudyAdminUserVo condition)
    {
        return mapper.selectUserScoreList(condition);
    }

    @Override
    public List<EngStudyRecord> selectUserScoreHistory(Long userId)
    {
        return mapper.selectUserScoreHistory(userId);
    }
}
