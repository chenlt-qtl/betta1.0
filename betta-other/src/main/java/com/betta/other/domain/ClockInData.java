package com.betta.other.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import com.betta.common.annotation.Excel;
import com.betta.common.core.domain.BaseEntity;

/**
 * 打卡数据对象 clock_in_data
 * 
 * @author chenlt
 * @date 2024-07-01
 */
@Data
public class ClockInData extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long id;

    /** 打卡日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "打卡日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date time;

    private String timeStr;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String value;

}
