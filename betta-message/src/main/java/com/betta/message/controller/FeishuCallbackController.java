package com.betta.message.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.betta.common.utils.StringUtils;
import com.betta.message.domain.MessageChannelConfig;
import com.betta.message.domain.MessageRecord;
import com.betta.message.enums.ChannelTypeEnum;
import com.betta.message.enums.MessageDirectionEnum;
import com.betta.message.service.IMessageChannelConfigService;
import com.betta.message.service.IMessageProcessService;
import com.betta.message.service.IMessageRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * 飞书事件订阅回调：url_verification + 接收消息后解析并回复
 *
 * @author betta
 */
@Slf4j
@RestController
@RequestMapping("/message/feishu")
public class FeishuCallbackController {

    @Autowired
    private IMessageChannelConfigService messageChannelConfigService;
    @Autowired
    private IMessageRecordService messageRecordService;
    @Autowired
    private IMessageProcessService messageProcessService;

    /**
     * 飞书配置的回调 URL 填：https://你的域名/message/feishu/callback
     *
     * @param body 飞书 POST 的 body（可能含 challenge 或 event）
     * @return 若为 url_verification 返回 {"challenge":"xxx"}，否则返回 200 空或 OK
     */
    @PostMapping("/callback")
    public Object callback(@RequestBody String body) {
        log.info("飞书Callback:{}", body);
        if (StringUtils.isBlank(body)) {
            return new JSONObject();
        }
        JSONObject root = JSON.parseObject(body);
        if (root.containsKey("challenge")) {
            JSONObject res = new JSONObject();
            res.put("challenge", root.getString("challenge"));
            return res;
        }
        JSONObject header = root.getJSONObject("header");
        JSONObject event = root.getJSONObject("event");
        if (header == null || event == null) {
            return new JSONObject();
        }
        String eventType = header.getString("event_type");
        if (!"im.message.receive_v1".equals(eventType)) {
            return new JSONObject();
        }
        JSONObject message = event.getJSONObject("message");
        JSONObject sender = event.getJSONObject("sender");
        if (message == null) {
            return new JSONObject();
        }
        String chatId = message.getString("chat_id");
        String messageId = message.getString("message_id");
        String contentStr = message.getString("content");
        String userId = null;
        if (sender != null && sender.containsKey("sender_id")) {
            JSONObject senderId = sender.getJSONObject("sender_id");
            if (senderId != null) {
                userId = senderId.getString("open_id");
                if (StringUtils.isEmpty(userId)) {
                    userId = senderId.getString("user_id");
                }
            }
        }
        String text = parseFeishuContent(contentStr);
        MessageChannelConfig config = messageChannelConfigService.selectOneEnabledByChannel(ChannelTypeEnum.FEISHU.getCode());
        Long configId = config != null ? config.getId() : null;
        MessageRecord record = new MessageRecord();
        record.setConfigId(configId);
        record.setChannelType(ChannelTypeEnum.FEISHU.getCode());
        record.setDirection(MessageDirectionEnum.RECEIVE.getCode());
        record.setContent(text);
        record.setChatId(chatId);
        record.setUserId(userId);
        record.setMessageId(messageId);
        record.setStatus("PENDING");
        record.setCreateTime(new Date());
        messageRecordService.insert(record);
        messageProcessService.processReceiveMessageAsync(record.getId());
        return new JSONObject();
    }

    /**
     * 解析飞书 message.content：通常为 {"text":"用户输入"} 或 {"json":"..."}
     *
     * @param contentStr JSON 字符串
     * @return 纯文本
     */
    private String parseFeishuContent(String contentStr) {
        if (StringUtils.isBlank(contentStr)) {
            return "";
        }
        try {
            JSONObject content = JSON.parseObject(contentStr);
            if (content.containsKey("text")) {
                return content.getString("text");
            }
            return contentStr;
        } catch (Exception e) {
            return contentStr;
        }
    }
}
