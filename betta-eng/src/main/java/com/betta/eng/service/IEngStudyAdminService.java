package com.betta.eng.service;

import com.betta.eng.domain.EngStudyRecord;
import com.betta.eng.domain.vo.EngStudyAdminUserVo;
import java.util.List;

/** 管理员积分只读查询服务。 */
public interface IEngStudyAdminService
{
    /** 按 condition 查询全员积分汇总。 */
    List<EngStudyAdminUserVo> selectUserScoreList(EngStudyAdminUserVo condition);

    /** 查询 userId 对应用户的完整积分历史。 */
    List<EngStudyRecord> selectUserScoreHistory(Long userId);
}
