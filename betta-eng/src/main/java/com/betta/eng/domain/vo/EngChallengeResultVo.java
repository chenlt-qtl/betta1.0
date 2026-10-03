package com.betta.eng.domain.vo;

import java.util.List;
import lombok.Data;

/**
 * 闯关提交结果，返回计分、金币、通关状态和逐题复盘信息。
 */
@Data
public class EngChallengeResultVo {
    /** 百分制得分。 */
    private Integer score;
    /** 正确题数。 */
    private Integer correctCount;
    /** 总题数。 */
    private Integer totalCount;
    /** 是否达到六十分通关线。 */
    private Boolean passed;
    /** 本轮获得金币，包含基础金币和最高适用成绩档位奖励。 */
    private Long coinReward;
    /** 本轮奖励入账后的当前金币余额。 */
    private Long coinBalance;
    /** 逐题判定结果。 */
    private List<ResultItem> results;

    /**
     * 单题判定结果，仅在提交后返回，避免挑战获取阶段泄露正确答案。
     */
    @Data
    public static class ResultItem {
        /** 题目标识。 */
        private String questionId;
        /** 是否回答正确。 */
        private Boolean correct;
        /** 正确答案。 */
        private String correctAnswer;
    }
}
