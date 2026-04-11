package com.betta.robot.service.impl;

import com.alibaba.fastjson2.JSON;
import com.betta.common.utils.StringUtils;
import com.betta.framework.manager.AsyncManager;
import com.betta.robot.client.FeishuClient;
import com.betta.robot.client.WeChatClient;
import com.betta.robot.domain.MessageChannelConfig;
import com.betta.robot.domain.MessageRecord;
import com.betta.robot.dto.ActionResult;
import com.betta.robot.dto.CommandDTO;
import com.betta.robot.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.TimerTask;

/**
 * 消息处理编排实现：异步解析 -> 分发 -> 回复
 *
 * @author betta
 */
@Slf4j
@Service
public class MessageProcessServiceImpl implements IMessageProcessService {

    @Autowired
    private IMessageRecordService messageRecordService;
    @Autowired
    private IMessageChannelConfigService messageChannelConfigService;
    @Autowired
    private ILLMParseService llmParseService;
    @Autowired
    private ICommandDispatchService commandDispatchService;
    @Autowired
    private FeishuClient feishuClient;
    @Autowired
    private WeChatClient weChatClient;

    @Override
    public void processReceiveMessageAsync(Long recordId) {
        AsyncManager.me().execute(new TimerTask() {
            @Override
            public void run() {
                doProcess(recordId);
            }
        });
    }

    /**
     * 同步执行：加载记录 -> 解析 -> 分发 -> 回发 -> 更新记录
     *
     * @param recordId 消息记录 ID
     */
    private void doProcess(Long recordId) {
        MessageRecord record = messageRecordService.selectById(recordId);
        if (record == null || !"RECEIVE".equals(record.getDirection())) {
            return;
        }
        MessageChannelConfig config = null;
        if (record.getConfigId() != null) {
            config = messageChannelConfigService.selectById(record.getConfigId());
        }
        if (config == null) {
            config = messageChannelConfigService.selectOneEnabledByChannel(record.getChannelType());
        }
        String content = record.getContent();
        if (StringUtils.isBlank(content)) {
            updateRecordFail(record, "消息内容为空");
            return;
        }
        CommandDTO command = llmParseService.parse(content);
        record.setParsedIntent(command.getIntent());
        record.setParsedJson(JSON.toJSONString(command));
        ActionResult actionResult = commandDispatchService.dispatch(command);
        String replyText = actionResult.isSuccess() ? ("操作成功：" + actionResult.getMessage()) : ("操作失败：" + actionResult.getMessage());
        boolean sent = sendReply(config, record, replyText);
        record.setReplyContent(replyText);
        record.setStatus(sent ? "SUCCESS" : "FAIL");
        if (!sent) {
            record.setErrorMsg("回复发送失败");
        }
        messageRecordService.update(record);
    }

    /**
     * 根据渠道发送回复
     *
     * @param config    配置（可为 null）
     * @param record    记录（含 channelType, chatId, userId）
     * @param replyText 回复文案
     * @return 是否发送成功
     */
    private boolean sendReply(MessageChannelConfig config, MessageRecord record, String replyText) {
        if (config == null) {
            config = messageChannelConfigService.selectOneEnabledByChannel(record.getChannelType());
        }
        if (config == null) {
            log.warn("No config for channel: {}", record.getChannelType());
            return false;
        }
        String channel = record.getChannelType();
        if ("FEISHU".equals(channel)) {
            return feishuClient.sendText(config, record.getChatId(), replyText);
        }
        if ("WECHAT".equals(channel)) {
            return weChatClient.sendText(config, record.getUserId(), replyText);
        }
        return false;
    }

    private void updateRecordFail(MessageRecord record, String errorMsg) {
        record.setStatus("FAIL");
        record.setErrorMsg(errorMsg);
        messageRecordService.update(record);
    }
}
