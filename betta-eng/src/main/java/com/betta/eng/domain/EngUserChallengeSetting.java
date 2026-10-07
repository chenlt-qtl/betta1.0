package com.betta.eng.domain;

import com.betta.common.core.domain.BaseEntity;
import lombok.Data;

/** 用户独立的普通单词测试题型偏好。 */
@Data
public class EngUserChallengeSetting extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private Boolean wordToMeaningEnabled;
    private Boolean meaningToWordEnabled;
    private Boolean sentenceClozeEnabled;
    private Boolean pronunciationEnabled;
    /** 每次保存递增，用于拒绝设置变更前生成的普通测试。 */
    private Long settingVersion;
}
