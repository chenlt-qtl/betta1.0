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
    /** 当前访问环境是否支持启动跟读评分。 */
    private Boolean pronunciationEnabled;
    /** 当前关卡尚未学习的单词数。 */
    private Integer newWordCount;
    /** 当前关卡是否还有生词。 */
    private Boolean hasNewWords;
}
