package com.betta.message.domain;

import com.betta.common.annotation.Excel;
import com.betta.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 大模型配置
 *
 * @author betta
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MessageLlmConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @Excel(name = "主键", cellType = com.betta.common.annotation.Excel.ColumnType.NUMERIC)
    private Long id;

    /** 配置名称 */
    @Excel(name = "配置名称")
    private String configName;

    /** 提供商：OPENAI/ANTHROPIC/BAIDU/ALIYUN/LOCAL等 */
    @Excel(name = "提供商")
    private String provider;

    /** API Key */
    private String apiKey;

    /** API 端点（可选，本地部署时使用） */
    @Excel(name = "API端点")
    private String apiEndpoint;

    /** 模型名称 */
    @Excel(name = "模型名称")
    private String modelName;

    /** 温度参数（0-1） */
    @Excel(name = "温度")
    private BigDecimal temperature;

    /** 最大Token数 */
    @Excel(name = "最大Token")
    private Integer maxTokens;

    /** Top P 参数 */
    @Excel(name = "Top P")
    private BigDecimal topP;

    /** 超时时间（秒） */
    @Excel(name = "超时时间")
    private Integer timeout;

    /** 状态：0启用 1停用 */
    @Excel(name = "状态", readConverterExp = "0=启用,1=停用")
    private String status;
}
