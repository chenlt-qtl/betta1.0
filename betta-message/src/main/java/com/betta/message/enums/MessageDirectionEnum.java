package com.betta.message.enums;

/**
 * 消息方向
 *
 * @author betta
 */
public enum MessageDirectionEnum {

    RECEIVE("RECEIVE", "接收"),
    SEND("SEND", "发送");

    private final String code;
    private final String label;

    MessageDirectionEnum(String code, String label) {
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
