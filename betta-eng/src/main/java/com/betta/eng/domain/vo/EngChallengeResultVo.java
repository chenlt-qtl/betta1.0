package com.betta.eng.domain.vo;

import java.util.List;
import lombok.Data;

/**
 * 闯关提交结果，返回计分、金币、通关状态和逐题复盘信息。
 */
@Data
public class EngChallengeResultVo {
    private String attemptId;
    private String mode;
    private Long articleId;
    private Integer levelNo;
    /** 百分制得分。 */
    private Integer score;
    /** 正确题数。 */
    private Integer correctCount;
    /** 总题数。 */
    private Integer totalCount;
    /** 跟读合格数。 */
    private Integer pronunciationPassedCount;
    /** 跟读题总数。 */
    private Integer pronunciationTotalCount;
    /** 本轮跟读平均总分，未启用跟读时为空。 */
    private Integer pronunciationAverageScore;
    /** 是否达到 80 分一星通关线。 */
    private Boolean passed;
    private Integer stars;
    private Long milestoneCoin;
    private Long reviewCoin;
    /** 本轮获得金币，为里程碑金币与复习金币之和。 */
    private Long coinReward;
    /** 本轮奖励入账后的当前金币余额。 */
    private Long coinBalance;
    /** 逐题判定结果。 */
    private List<ResultItem> results;
    private Boolean nextLevelUnlocked;
    /** 真正可进入的下一非空新词关，无下一关时为空。 */
    private Integer nextLevelNo;
    private List<EngChallengeWordResultVo> wordResults;

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
