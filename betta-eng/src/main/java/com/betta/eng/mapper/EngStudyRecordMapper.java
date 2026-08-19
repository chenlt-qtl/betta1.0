package com.betta.eng.mapper;

import com.betta.eng.domain.EngStudyRecord;
import java.util.List;

/** 学习记录数据访问接口。 */
public interface EngStudyRecordMapper {
    /** 新增学习记录；record 为本次闯关结果，返回影响行数。 */
    int insertEngStudyRecord(EngStudyRecord record);
    /** 查询用户最近十条记录；userId 为登录用户主键，返回学习记录集合。 */
    List<EngStudyRecord> selectRecentByUserId(Long userId);
    /** 汇总用户学习次数；userId 为登录用户主键，返回次数。 */
    long countByUserId(Long userId);
    /** 汇总用户积分；userId 为登录用户主键，返回总积分。 */
    long sumScoreByUserId(Long userId);
    /** 查询用户指定文章最后学习时间；记录中仅返回 createTime。 */
    EngStudyRecord selectLatestByUserAndArticle(EngStudyRecord record);
}
