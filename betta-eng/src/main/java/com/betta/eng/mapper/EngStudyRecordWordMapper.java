package com.betta.eng.mapper;

import com.betta.eng.domain.EngStudyRecordWord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 学习记录规范词明细数据访问。 */
public interface EngStudyRecordWordMapper
{
    /** 批量写入一次测试的规范词明细。 */
    int insertBatch(@Param("items") List<EngStudyRecordWord> items);
    /** 仅查询归属指定用户的测试词明细。 */
    List<EngStudyRecordWord> selectByRecordAndUser(@Param("recordId") Long recordId,
            @Param("userId") Long userId);
}
