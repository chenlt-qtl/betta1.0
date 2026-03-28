package com.betta.message.service;

/**
 * 消息处理编排：收到消息后异步执行「解析 -> 分发 -> 回复」
 *
 * @author betta
 */
public interface IMessageProcessService {

    /**
     * 异步处理一条接收消息（解析、执行命令、回发）
     *
     * @param recordId 消息记录 ID（direction=RECEIVE）
     */
    void processReceiveMessageAsync(Long recordId);
}
