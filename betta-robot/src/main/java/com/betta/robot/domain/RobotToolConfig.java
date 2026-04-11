package com.betta.robot.domain;

import lombok.Data;
import com.betta.common.annotation.Excel;
import com.betta.common.core.domain.BaseEntity;

/**
 * 工具配置对象 robot_tool_config
 * 
 * @author betta
 * @date 2026-04-11
 */
@Data
public class RobotToolConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 配置名称 */
    @Excel(name = "配置名称")
    private String configName;

    /** 类名 */
    @Excel(name = "类名")
    private String className;

    /** 参数（JSON格式，如：{"name":"你好"}） */
    @Excel(name = "参数", readConverterExp = "J=SON格式，如：{\"name\":\"你好\"}")
    private String toolParams;

    /** 关键词（多个用逗号分隔） */
    @Excel(name = "关键词", readConverterExp = "多=个用逗号分隔")
    private String keywords;

    /** 优先级（数字越大越优先） */
    @Excel(name = "优先级", readConverterExp = "数=字越大越优先")
    private Long priority;

    /** 描述（用于大模型选择） */
    @Excel(name = "描述", readConverterExp = "用=于大模型选择")
    private String description;

    /** 提示词（用于提取参数） */
    @Excel(name = "提示词", readConverterExp = "用=于提取参数")
    private String prompt;

    /** DTO类名（用于参数反序列化） */
    @Excel(name = "DTO类名", readConverterExp = "用=于参数反序列化")
    private String dtoClass;

    /** 关联的大模型配置ID */
    @Excel(name = "关联的大模型配置ID")
    private Long llmConfigId;

    /** 状态：0启用 1停用 */
    @Excel(name = "状态：0启用 1停用")
    private String status;
}
