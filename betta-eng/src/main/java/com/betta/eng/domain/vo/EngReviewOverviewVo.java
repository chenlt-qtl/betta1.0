package com.betta.eng.domain.vo;

import java.util.List;
import lombok.Data;

/** 当前用户全局复习概览。 */
@Data
public class EngReviewOverviewVo
{
    private Integer learnedWordCount;
    private Integer recommendedCount;
    private List<EngReviewWordVo> words;
}
