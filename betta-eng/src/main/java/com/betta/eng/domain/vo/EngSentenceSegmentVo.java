package com.betta.eng.domain.vo;

import lombok.Data;

/** 句子按原文顺序拆分后的文本片段。 */
@Data
public class EngSentenceSegmentVo
{
    /** 原始文本。 */
    private String text;
    /** 片段类型：WORD 或 TEXT。 */
    private String type;
    /** 唯一匹配的规范词主键。 */
    private Long wordId;
    /** 唯一匹配的规范词文本。 */
    private String wordName;
    /** 当前句子是否已关联该规范词。 */
    private boolean selected;
    /** 是否能够建立无歧义关系。 */
    private boolean selectable;
}
