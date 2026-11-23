package com.betta.other.domain.menstrual;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/** 单次经期记录；结束日期为空时仅用于展示和统计的七天估算。 */
@Data
public class MenstrualRecord
{
    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private String note;
    @JsonIgnore
    private Long userId;
}
