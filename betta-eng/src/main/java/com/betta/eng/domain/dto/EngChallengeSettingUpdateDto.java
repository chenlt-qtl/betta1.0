package com.betta.eng.domain.dto;

import lombok.Data;

/** 当前用户更新普通单词测试题型设置的请求。 */
@Data
public class EngChallengeSettingUpdateDto
{
    private Boolean wordToMeaningEnabled;
    private Boolean meaningToWordEnabled;
    private Boolean sentenceClozeEnabled;
    private Boolean pronunciationEnabled;
}
