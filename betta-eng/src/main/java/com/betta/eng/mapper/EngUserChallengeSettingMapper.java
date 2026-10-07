package com.betta.eng.mapper;

import com.betta.eng.domain.EngUserChallengeSetting;

/** 用户普通单词测试题型设置数据访问。 */
public interface EngUserChallengeSettingMapper
{
    /** 按用户查询题型设置。 */
    EngUserChallengeSetting selectByUserId(Long userId);

    /** 新增或更新当前用户设置，并递增设置版本。 */
    int upsert(EngUserChallengeSetting setting);
}
