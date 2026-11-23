package com.betta.other.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import com.betta.common.exception.ServiceException;
import com.betta.common.utils.SecurityUtils;
import com.betta.other.domain.menstrual.MenstrualRecord;
import com.betta.other.domain.menstrual.MenstrualSummary;
import com.betta.other.mapper.MenstrualMapper;
import com.betta.other.service.IMenstrualService;

/** 经期记录只使用登录态中的用户标识。 */
@Service
public class MenstrualServiceImpl implements IMenstrualService
{
    private final MenstrualMapper mapper;

    /** 创建经期记录服务。 */
    public MenstrualServiceImpl(MenstrualMapper mapper)
    {
        this.mapper = mapper;
    }

    @Override
    public List<MenstrualRecord> listRecords()
    {
        return mapper.selectRecords(SecurityUtils.getUserId());
    }

    @Override
    public MenstrualRecord createRecord(MenstrualRecord record)
    {
        validate(record);
        record.setId(null);
        record.setUserId(SecurityUtils.getUserId());
        try
        {
            mapper.insertRecord(record);
        }
        catch (DuplicateKeyException e)
        {
            throw new ServiceException("该开始日期已有记录");
        }
        return record;
    }

    @Override
    public MenstrualRecord updateRecord(Long id, MenstrualRecord record)
    {
        validate(record);
        Long userId = SecurityUtils.getUserId();
        if (mapper.selectRecord(userId, id) == null)
        {
            throw new ServiceException("记录不存在");
        }
        record.setId(id);
        record.setUserId(userId);
        try
        {
            if (mapper.updateRecord(record) == 0 && mapper.selectRecord(userId, id) == null)
            {
                throw new ServiceException("记录不存在");
            }
        }
        catch (DuplicateKeyException e)
        {
            throw new ServiceException("该开始日期已有记录");
        }
        return record;
    }

    @Override
    public void deleteRecord(Long id)
    {
        if (mapper.deleteRecord(SecurityUtils.getUserId(), id) == 0)
        {
            throw new ServiceException("记录不存在");
        }
    }

    @Override
    public MenstrualSummary summary()
    {
        List<MenstrualRecord> records = listRecords();
        MenstrualSummary summary = new MenstrualSummary();
        summary.setRecordCount(records.size());
        if (records.isEmpty())
        {
            return summary;
        }
        long totalDays = 0;
        for (MenstrualRecord record : records)
        {
            // 结束日期缺失只影响统计，不回写估算日期。
            totalDays += record.getEndDate() == null ? 7 : ChronoUnit.DAYS.between(record.getStartDate(), record.getEndDate()) + 1;
        }
        summary.setAveragePeriodDays(BigDecimal.valueOf(totalDays).divide(BigDecimal.valueOf(records.size()), 1, RoundingMode.HALF_UP));
        int count = Math.min(3, records.size() - 1);
        if (count > 0)
        {
            long totalCycle = 0;
            for (int i = 0; i < count; i++)
            {
                totalCycle += ChronoUnit.DAYS.between(records.get(i + 1).getStartDate(), records.get(i).getStartDate());
            }
            long average = Math.round((double) totalCycle / count);
            summary.setAverageCycleDays(average);
            summary.setPredictedStartDate(records.get(0).getStartDate().plusDays(average));
        }
        return summary;
    }

    private void validate(MenstrualRecord record)
    {
        if (record == null || record.getStartDate() == null)
        {
            throw new ServiceException("请选择开始日期");
        }
        if (record.getEndDate() != null && record.getEndDate().isBefore(record.getStartDate()))
        {
            throw new ServiceException("结束日期不能早于开始日期");
        }
        if (record.getNote() == null)
        {
            record.setNote("");
        }
        if (record.getNote().length() > 500)
        {
            throw new ServiceException("备注不能超过500个字符");
        }
    }
}
