package com.betta.eng.domain;

import java.time.LocalDate;
import lombok.Data;

/** 用户每日已完成测试单词，负责跨文章去重和文章新词配额统计。 */
@Data
public class EngDailyTestWord {
    /** 主键。 */
    private Long id;
    /** 用户主键。 */
    private Long userId;
    /** 本次学习来源文章主键。 */
    private Long articleId;
    /** 单词主键。 */
    private Long wordId;
    /** 对应的学习记录主键；登记占位后回填。 */
    private Long studyRecordId;
    /** 完成日期。 */
    private LocalDate studyDate;
    /** 队列分类：LEARNING、REVIEW 或 NEW。 */
    private String category;
    /** 创建者。 */
    private String createBy;
}
