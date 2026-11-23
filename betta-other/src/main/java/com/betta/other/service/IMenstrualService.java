package com.betta.other.service;

import java.util.List;
import com.betta.other.domain.menstrual.MenstrualRecord;
import com.betta.other.domain.menstrual.MenstrualSummary;

/** 个人经期记录和统计服务。 */
public interface IMenstrualService
{
    /** 返回当前用户的历史记录。 */
    List<MenstrualRecord> listRecords();
    /** 保存当前用户的新记录。 */
    MenstrualRecord createRecord(MenstrualRecord record);
    /** 更新当前用户的记录。 */
    MenstrualRecord updateRecord(Long id, MenstrualRecord record);
    /** 删除当前用户的记录。 */
    void deleteRecord(Long id);
    /** 根据当前用户的历史记录计算统计。 */
    MenstrualSummary summary();
}
