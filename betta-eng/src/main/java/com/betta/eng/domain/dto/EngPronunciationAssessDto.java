package com.betta.eng.domain.dto;

import org.springframework.web.multipart.MultipartFile;
import lombok.Data;

/** 单词跟读评分请求，由服务端校验测试范围与 WAV 音频。 */
@Data
public class EngPronunciationAssessDto
{
    private String attemptId;
    private String mode;
    private Long articleId;
    private Integer levelNo;
    private String questionId;
    private MultipartFile audio;
}
