package com.betta.eng.domain.dto;

import java.util.List;
import lombok.Data;

/**
 * 文章闯关提交请求，包含文章主键和完整答案列表。
 */
@Data
public class EngChallengeSubmitDto {
    /** 文章主键。 */
    private Long articleId;
    /** 本次提交的题目答案。 */
    private List<EngChallengeAnswerDto> answers;
}
