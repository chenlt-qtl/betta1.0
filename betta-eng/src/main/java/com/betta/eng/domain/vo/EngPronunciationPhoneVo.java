package com.betta.eng.domain.vo;

import lombok.Data;

/** 跟读结果中的单个音素反馈。 */
@Data
public class EngPronunciationPhoneVo
{
    private String phone;
    private String referencePhone;
    private String referenceLetter;
    private Double accuracy;
    private Boolean matched;
    private Boolean stress;
    private Boolean detectedStress;
}
