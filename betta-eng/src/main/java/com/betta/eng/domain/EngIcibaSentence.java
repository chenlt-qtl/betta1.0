package com.betta.eng.domain;

import com.betta.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 词典例句实体，保存单词对应的英文例句与中文释义。
 */
@Data
public class EngIcibaSentence extends BaseEntity {
    private static final long serialVersionUID = 1L;
    /** 例句主键。 */
    private Long id;
    /** 单词主键。 */
    private Long wordId;
    /** 英文原句。 */
    private String orig;
    /** 中文释义。 */
    private String trans;
    /** 例句状态。 */
    private String status;
}
