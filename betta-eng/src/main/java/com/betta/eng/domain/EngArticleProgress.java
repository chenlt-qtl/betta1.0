package com.betta.eng.domain;

import com.betta.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 用户文章最好进度实体，同一用户与文章仅保留一条最好成绩。
 */
@Data
public class EngArticleProgress extends BaseEntity {
    private static final long serialVersionUID = 1L;
    /** 进度主键。 */
    private Long id;
    /** 登录用户主键。 */
    private Long userId;
    /** 文章主键。 */
    private Long articleId;
    /** 历史最好得分。 */
    private Integer bestScore;
    /** 最好成绩对应的正确题数。 */
    private Integer bestCorrectCount;
    /** 最好成绩对应的总题数。 */
    private Integer bestTotalCount;
    /** 是否已通关，零未通关、一已通关。 */
    private Integer completed;
}
