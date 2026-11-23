package com.betta.other.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.betta.other.domain.menstrual.MenstrualRecord;

/** 经期记录数据访问，查询和修改均限制所属用户。 */
public interface MenstrualMapper
{
    List<MenstrualRecord> selectRecords(@Param("userId") Long userId);
    MenstrualRecord selectRecord(@Param("userId") Long userId, @Param("id") Long id);
    int insertRecord(MenstrualRecord record);
    int updateRecord(MenstrualRecord record);
    int deleteRecord(@Param("userId") Long userId, @Param("id") Long id);
}
