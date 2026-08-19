package com.betta.eng.domain;

import com.betta.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 用户错词实体，按用户、文章和单词累计错误次数。
 */
@Data
public class EngWrongWord extends BaseEntity {
    private static final long serialVersionUID = 1L;
    /** 错词记录主键。 */
    private Long id;
    /** 登录用户主键。 */
    private Long userId;
    /** 来源文章主键。 */
    private Long articleId;
    /** 单词主键。 */
    private Long wordId;
    /** 错误累计次数。 */
    private Integer wrongCount;
    /** 掌握状态，零未掌握、一已掌握。 */
    private Integer mastered;
    /** 文章标题，仅用于联表查询展示。 */
    private String articleTitle;
    /** 单词文本，仅用于联表查询展示。 */
    private String wordName;
    /** 单词释义，仅用于联表查询展示。 */
    private String acceptation;
}
