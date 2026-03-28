package com.betta.message.domain;

import com.betta.common.annotation.Excel;
import com.betta.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API配置
 *
 * @author betta
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MessageApiConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @Excel(name = "主键", cellType = com.betta.common.annotation.Excel.ColumnType.NUMERIC)
    private Long id;

    /** 配置名称 */
    @Excel(name = "配置名称")
    private String configName;

    /** API地址 */
    @Excel(name = "API地址")
    private String apiUrl;

    /** API参数（JSON格式） */
    @Excel(name = "API参数")
    private String apiParams;

    /** 关键词（多个用逗号分隔） */
    @Excel(name = "关键词")
    private String keywords;

    /** 优先级（数字越大越优先） */
    @Excel(name = "优先级")
    private Integer priority;

    /** 描述（用于大模型选择） */
    @Excel(name = "描述")
    private String description;

    /** 提示词（用于提取参数） */
    @Excel(name = "提示词")
    private String prompt;

    /** 关联的大模型配置ID */
    @Excel(name = "大模型配置ID")
    private Long llmConfigId;

    /** 状态：0启用 1停用 */
    @Excel(name = "状态", readConverterExp = "0=启用,1=停用")
    private String status;
}
