package com.betta.eng.domain;

import com.betta.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 用户单词熟悉度实体，保留原测试能力的成绩数据。
 */
@Data
public class EngUserScore extends BaseEntity {
    private static final long serialVersionUID = 1L;
    /** 成绩主键。 */
    private Long id;
    /** 用户名。 */
    private String user;
    /** 单词文本。 */
    private String wordName;
    /** 熟悉度增量或累计值。 */
    private Integer familiarity;
    /** 成绩状态。 */
    private String status;
}
