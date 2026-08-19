package com.betta.eng.domain;

import com.betta.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 用户闯关学习记录实体，每次合法提交均保留一条历史记录。
 */
@Data
public class EngStudyRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;
    /** 学习记录主键。 */
    private Long id;
    /** 登录用户主键。 */
    private Long userId;
    /** 文章主键。 */
    private Long articleId;
    /** 本次得分。 */
    private Integer score;
    /** 正确题数。 */
    private Integer correctCount;
    /** 总题数。 */
    private Integer totalCount;
    /** 是否通关，零未通关、一已通关。 */
    private Integer passed;
    /** 文章标题，仅用于联表查询展示。 */
    private String articleTitle;
}
