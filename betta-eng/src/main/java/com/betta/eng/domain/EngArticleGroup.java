package com.betta.eng.domain;

import com.betta.common.annotation.Excel;
import com.betta.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 英语文章分组实体，用于对文章进行层级归类。
 */
@Data
public class EngArticleGroup extends BaseEntity {
    private static final long serialVersionUID = 1L;
    /** 分组主键。 */
    private Long id;
    /** 分组名称。 */
    @Excel(name = "名称")
    private String name;
    /** 分组备注。 */
    @Excel(name = "手工注释")
    private String comment;
    /** 分组状态。 */
    private String status;
}
