package com.betta.message.domain;

import com.betta.common.annotation.Excel;
import com.betta.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息记录（收发）
 *
 * @author betta
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MessageRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "主键", cellType = com.betta.common.annotation.Excel.ColumnType.NUMERIC)
    private Long id;

    private Long configId;
    @Excel(name = "渠道")
    private String channelType;
    @Excel(name = "方向", readConverterExp = "RECEIVE=接收,SEND=发送")
    private String direction;
    @Excel(name = "内容")
    private String content;
    private String replyContent;
    private String chatId;
    private String userId;
    private String messageId;
    private String parsedIntent;
    private String parsedJson;
    @Excel(name = "状态")
    private String status;
    private String errorMsg;
}
