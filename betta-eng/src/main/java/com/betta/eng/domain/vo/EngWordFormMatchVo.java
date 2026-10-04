package com.betta.eng.domain.vo;

import lombok.Data;

/** 句子词形批量解析结果，将规范化词形映射到候选规范词。 */
@Data
public class EngWordFormMatchVo
{
    /** 规范化后的句中词形。 */
    private String lookupKey;
    /** 候选规范词主键。 */
    private Long wordId;
    /** 候选规范词文本。 */
    private String wordName;
}
