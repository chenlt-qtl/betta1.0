package com.betta.eng.domain.vo;

import com.betta.eng.domain.EngUserScore;
import lombok.Data;

/**
 * 用户单词测试展示对象，聚合单词、文章关系和示例句子。
 */
@Data
public class EngUserScoreVo extends EngUserScore {
    private static final long serialVersionUID = 1L;
    /** 文章单词关系主键。 */
    private Long relId;
    /** 单词主键。 */
    private Long wordId;
    /** 文章主键。 */
    private Long articleId;
    /** 原型词。 */
    private String prototype;
    /** 示例句子。 */
    private String sentence;
    /** 示例句子释义。 */
    private String sentenceAcceptation;
    /** 单词音频。 */
    private String phMp3;
    /** 音标。 */
    private String phonetics;
    /** 单词释义。 */
    private String acceptation;
    /** 词形变化。 */
    private String exchange;
    /** 词性信息。 */
    private String parts;
}
