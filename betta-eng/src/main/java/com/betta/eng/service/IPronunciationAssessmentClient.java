package com.betta.eng.service;

import com.betta.eng.domain.vo.EngPronunciationAssessmentVo;

/** 英文单词跟读评测客户端边界，便于隔离第三方服务和自动测试。 */
public interface IPronunciationAssessmentClient
{
    EngPronunciationAssessmentVo assess(String word, byte[] wavAudio);
}
