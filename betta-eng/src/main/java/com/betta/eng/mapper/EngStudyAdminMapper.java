package com.betta.eng.mapper;

import com.betta.eng.domain.EngStudyRecord;
import com.betta.eng.domain.vo.EngStudyAdminUserVo;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 管理员积分查询数据访问接口。 */
public interface EngStudyAdminMapper
{
    /** 按 condition 中的用户名和昵称筛选未删除用户积分汇总。 */
    List<EngStudyAdminUserVo> selectUserScoreList(EngStudyAdminUserVo condition);

    /** 查询 userId 对应用户的完整积分历史。 */
    List<EngStudyRecord> selectUserScoreHistory(@Param("userId") Long userId);
}
