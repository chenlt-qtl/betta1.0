package com.betta.eng.domain.vo;

import java.util.List;
import lombok.Data;

/** 腾讯云返回后收敛为前端所需的单词跟读结果。 */
@Data
public class EngPronunciationAssessmentVo
{
    private String questionId;
    private Integer score;
    private Double accuracy;
    private Double fluency;
    private Double completeness;
    private Boolean passed;
    private Boolean matched;
    /** 腾讯云匹配标记，0 表示匹配参考单词。 */
    private Integer matchTag;
    /** 当前测试内该单词已消耗的有效测评次数。 */
    private Integer attemptCount;
    /** 当前测试内该单词剩余的测评次数。 */
    private Integer remainingAttempts;
    private List<EngPronunciationPhoneVo> phones;
}
