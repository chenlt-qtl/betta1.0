package com.betta.eng.domain.vo;

import com.betta.eng.domain.EngStudyRecord;
import java.util.List;
import lombok.Data;

/**
 * 当前用户学习统计展示对象，汇总闯关次数、积分、进度和错词。
 */
@Data
public class EngStudySummaryVo {
    /** 历史累计积分。 */
    private Long totalScore;
    /** 闯关提交次数。 */
    private Long studyCount;
    /** 已通关文章数量。 */
    private Long completedArticleCount;
    /** 未掌握错词数量。 */
    private Long wrongWordCount;
    /** 已掌握错词数量。 */
    private Long masteredWrongWordCount;
    /** 最近十次学习记录。 */
    private List<EngStudyRecord> recentRecords;
}
