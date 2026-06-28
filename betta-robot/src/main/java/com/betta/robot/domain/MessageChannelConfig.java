package com.betta.robot.domain;

import com.betta.common.annotation.Excel;
import com.betta.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息通道配置（飞书/微信，页面配置）
 *
 * @author betta
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MessageChannelConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @Excel(name = "主键", cellType = com.betta.common.annotation.Excel.ColumnType.NUMERIC)
    private Long id;

    /** 渠道：FEISHU/WECHAT */
    @Excel(name = "渠道类型")
    private String channelType;

    /** 渠道名称（展示用） */
    @Excel(name = "渠道名称")
    private String channelName;

    /** 应用ID */
    @Excel(name = "应用ID")
    private String appId;

    /** 应用密钥 */
    private String appSecret;

    /** 回调加密密钥 */
    private String encryptKey;

    /** 回调校验Token */
    private String verificationToken;

    /** 企业微信 AgentId */
    private String agentId;

    /** 企业微信 CorpId */
    private String corpId;

    /** 状态：0启用 1停用 */
    @Excel(name = "状态", readConverterExp = "0=启用,1=停用")
    private String status;
}
