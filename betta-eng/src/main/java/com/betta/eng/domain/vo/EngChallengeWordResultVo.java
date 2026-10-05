package com.betta.eng.domain.vo;

import lombok.Data;

/** 测试结算中的单词级结果。 */
@Data
public class EngChallengeWordResultVo
{
    private Long wordId;
    private String wordName;
    private Long sourceArticleId;
    private Integer correctCount;
    private Integer totalCount;
    private Boolean allCorrect;
    private Integer pronunciationScore;
    private Boolean pronunciationPassed;
    private Integer stars;
    private Integer highestStars;
    private Integer currentStars;
    private Long milestoneCoin;
    private Long reviewCoin;
}
