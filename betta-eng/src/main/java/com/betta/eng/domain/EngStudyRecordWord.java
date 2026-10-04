package com.betta.eng.domain;

import com.betta.common.core.domain.BaseEntity;
import lombok.Data;

/** 每次测试对应的规范词级结果明细。 */
@Data
public class EngStudyRecordWord extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long studyRecordId;
    private Long wordId;
    private Long sourceArticleId;
    private String studyMode;
    private Integer correctCount;
    private Integer totalCount;
    private Integer allCorrect;
    private Integer stars;
    private Long milestoneCoin;
    private Long reviewCoin;
    private String wordName;
    private String acceptation;
    private String sourceArticleTitle;
}
