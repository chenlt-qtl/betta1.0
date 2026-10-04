package com.betta.eng.domain.vo;

import lombok.Data;

/** 文章关卡地图中的单关状态。 */
@Data
public class EngArticleLevelVo
{
    private Integer levelNo;
    private Integer totalWordCount;
    private Integer newWordCount;
    private Integer learnedWordCount;
    private Boolean unlocked;
    private Boolean masteredByExistingWords;
    private Integer bestScore;
    private Integer highestStars;
}
