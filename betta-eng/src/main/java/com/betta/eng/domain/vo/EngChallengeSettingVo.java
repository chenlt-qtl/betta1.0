package com.betta.eng.domain.vo;

import lombok.Data;

/** 当前用户的普通单词测试题型设置。 */
@Data
public class EngChallengeSettingVo
{
    private Boolean wordToMeaningEnabled;
    private Boolean meaningToWordEnabled;
    private Boolean sentenceClozeEnabled;
    private Boolean pronunciationEnabled;
}
