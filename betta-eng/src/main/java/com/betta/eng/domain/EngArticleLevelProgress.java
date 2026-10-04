package com.betta.eng.domain;

import com.betta.common.core.domain.BaseEntity;
import lombok.Data;

/** 用户文章关卡的最好成绩与解锁进度。 */
@Data
public class EngArticleLevelProgress extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private Long articleId;
    private Integer levelNo;
    private Integer bestScore;
    private Integer highestStars;
    private Integer completedByKnownWords;
}
