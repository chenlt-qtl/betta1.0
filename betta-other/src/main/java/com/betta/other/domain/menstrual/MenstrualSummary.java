package com.betta.other.domain.menstrual;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 基于当前用户全部历史记录计算的统计。 */
@Data
public class MenstrualSummary
{
    private int recordCount;
    private Long averageCycleDays;
    private BigDecimal averagePeriodDays;
    private LocalDate predictedStartDate;
}
