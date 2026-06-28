package com.betta.robot.domain;

import com.betta.common.annotation.Excel;
import com.betta.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 大模型调用记录
 *
 * @author betta
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LlmCallRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @Excel(name = "主键", cellType = com.betta.common.annotation.Excel.ColumnType.NUMERIC)
    private Long id;

    /** 大模型配置ID */
    @Excel(name = "大模型配置ID")
    private Long llmConfigId;

    /** 模型名称 */
    @Excel(name = "模型名称")
    private String modelName;

    /** 提供商：OPENAI/ANTHROPIC/BAIDU/ALIYUN/LOCAL等 */
    @Excel(name = "提供商")
    private String provider;

    /** 请求内容（JSON格式） */
    @Excel(name = "提示词")
    private String prompt;

    /** 响应内容（JSON格式） */
    @Excel(name = "响应内容")
    private String responseContent;

    /** 状态：PENDING/SUCCESS/FAIL */
    @Excel(name = "状态", readConverterExp = "PENDING=处理中,SUCCESS=成功,FAIL=失败")
    private String status;

    /** 失败原因 */
    @Excel(name = "失败原因")
    private String errorMsg;

    /** 耗时（毫秒） */
    @Excel(name = "耗时(毫秒)")
    private Integer duration;
}
