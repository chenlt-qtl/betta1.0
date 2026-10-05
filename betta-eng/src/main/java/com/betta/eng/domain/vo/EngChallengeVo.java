package com.betta.eng.domain.vo;

import java.util.List;
import lombok.Data;

/**
 * 文章闯关内容展示对象，包含文章元数据、历史进度和无答案题目。
 */
@Data
public class EngChallengeVo {
    private String attemptId;
    private String mode;
    /** 文章主键。 */
    private Long articleId;
    private Integer levelNo;
    /** 文章标题。 */
    private String title;
    /** 本次挑战选中的最多五个单词，与题目范围一致。 */
    private List<EngWordVo> words;
    /** 本次挑战题目集合。 */
    private List<EngChallengeQuestionVo> questions;
    /** 本次测试是否包含跟读评分。 */
    private Boolean pronunciationEnabled;
}
