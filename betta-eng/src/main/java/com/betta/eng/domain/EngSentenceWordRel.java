package com.betta.eng.domain;

import com.betta.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 句子与规范词关系，matchedText 保留句中实际出现的大小写和词形。
 */
@Data
public class EngSentenceWordRel extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    /** 关系主键。 */
    private Long id;
    /** 句子主键。 */
    private Long sentenceId;
    /** 规范词主键。 */
    private Long wordId;
    /** 句中实际命中的词形。 */
    private String matchedText;
    /** 关联句子的文章主键，仅用于关系查询结果。 */
    private Long articleId;
    /** 关联句子的英文内容，仅用于关系查询结果。 */
    private String sentenceContent;
    /** 关联句子的中文释义，仅用于关系查询结果。 */
    private String sentenceAcceptation;
    /** 规范词文本，仅用于关系查询结果。 */
    private String wordName;
}
