package com.betta.robot.enums;

/**
 * 消息渠道类型
 *
 * @author betta
 */
public enum ChannelTypeEnum {

    /** 飞书 */
    FEISHU("FEISHU", "飞书"),
    /** 企业微信 */
    WECHAT("WECHAT", "企业微信");

    private final String code;
    private final String label;

    ChannelTypeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
