package com.betta.robot.service;

import com.betta.robot.dto.MessageProcessResult;

/**
 * API调度 Service 接口
 * 处理消息流程：关键词过滤 -> 大模型选择 -> 参数提取 -> API调用
 *
 * @author betta
 */
public interface IApiDispatchService {

    /**
     * 处理消息，选择合适的API配置并调用
     *
     * @param messageText 用户消息文本
     * @return 处理结果
     */
    MessageProcessResult processMessage(String messageText);
}
