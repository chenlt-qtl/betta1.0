package com.betta.eng.mapper;

import java.util.List;
import com.betta.eng.domain.mall.MallExchangeRecord;

/** 商城兑换记录数据访问接口。 */
public interface MallExchangeRecordMapper
{
    /** 新增兑换记录并回填主键。 */
    int insertExchangeRecord(MallExchangeRecord record);

    /** 按条件查询兑换记录；用户主键为空时用于管理端查询全部记录。 */
    List<MallExchangeRecord> selectExchangeRecordList(MallExchangeRecord record);
}
