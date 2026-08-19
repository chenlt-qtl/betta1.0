package com.betta.eng.domain.dto;

import lombok.Data;

/**
 * 文章闯关单题即时判题请求，携带文章、题目和用户当前答案。
 */
@Data
public class EngChallengeCheckDto {
    /** 文章主键，用于限定题目必须属于当前文章。 */
    private Long articleId;
    /** 服务端下发的题目标识。 */
    private String questionId;
    /** 用户当前选择或填写的答案。 */
    private String answer;
}
