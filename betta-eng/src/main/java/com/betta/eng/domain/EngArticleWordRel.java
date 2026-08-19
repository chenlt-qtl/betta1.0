package com.betta.eng.domain;

import com.betta.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 文章与单词关系实体，使用单词文本兼容既有基础表结构。
 */
@Data
public class EngArticleWordRel extends BaseEntity {
    private static final long serialVersionUID = 1L;
    /** 关系主键。 */
    private Long id;
    /** 文章主键，零表示用户从查询页加入的生词。 */
    private Long articleId;
    /** 关联单词文本。 */
    private String wordName;
    /** 关系状态。 */
    private String status;
}
