package com.betta.eng.mapper;

import com.betta.eng.domain.EngStudyRecord;
import java.util.List;

/** 学习记录数据访问接口。 */
public interface EngStudyRecordMapper {
    /** 查询用户最近十条记录；userId 为登录用户主键，返回学习记录集合。 */
    List<EngStudyRecord> selectRecentByUserId(Long userId);
    /** 汇总用户学习次数；userId 为登录用户主键，返回次数。 */
    long countByUserId(Long userId);
    /** 汇总用户积分；userId 为登录用户主键，返回总积分。 */
    long sumScoreByUserId(Long userId);
    /** 以用户和 attemptId 查询幂等结果。 */
    EngStudyRecord selectByUserAndAttempt(EngStudyRecord record);
    /** 在当前事务中锁定并读取已提交的幂等记录。 */
    EngStudyRecord selectByUserAndAttemptForUpdate(EngStudyRecord record);
    /** 幂等插入记录头，重复 attempt 返回零。 */
    int insertIgnoreEngStudyRecord(EngStudyRecord record);
    int updateOutcome(EngStudyRecord record);
}
