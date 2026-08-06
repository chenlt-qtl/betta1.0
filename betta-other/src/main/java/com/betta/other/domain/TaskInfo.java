package com.betta.other.domain;

import lombok.Data;
import com.betta.common.annotation.Excel;
import com.betta.common.core.domain.BaseEntity;

/**
 * 任务对象 task_info
 * 
 * @author chenlt
 * @date 2025-01-10
 */
@Data
public class TaskInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long id;

    /** 类型 */
    @Excel(name = "类型")
    private Long type;

    /** 名称 */
    @Excel(name = "名称")
    private String content;

    /** 备注 */
    @Excel(name = "备注")
    private String comment;

    //任务状态
    private Long taskStatus;

}
