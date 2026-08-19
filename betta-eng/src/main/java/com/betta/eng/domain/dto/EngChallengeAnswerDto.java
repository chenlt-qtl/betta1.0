package com.betta.eng.domain.dto;

import lombok.Data;

/**
 * 闯关单题答案请求，只携带题目标识与用户答案。
 */
@Data
public class EngChallengeAnswerDto {
    /** 服务端下发的题目标识。 */
    private String questionId;
    /** 用户选择或填写的答案。 */
    private String answer;
}
