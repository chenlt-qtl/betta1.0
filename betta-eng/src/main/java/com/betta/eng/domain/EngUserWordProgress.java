package com.betta.eng.domain;

import com.betta.common.core.domain.BaseEntity;
import java.util.Date;
import lombok.Data;

/** 用户对规范词的全局学习与星级进度。 */
@Data
public class EngUserWordProgress extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private Long wordId;
    private Integer learned;
    private Integer highestStars;
    private Integer latestStars;
    private Date latestTestTime;
    private Integer rewardedStars;
    private Long firstArticleId;
    private Integer firstLevelNo;
}
