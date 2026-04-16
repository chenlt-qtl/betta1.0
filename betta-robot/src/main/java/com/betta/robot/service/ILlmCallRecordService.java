package com.betta.robot.service;

import com.alibaba.fastjson2.JSONObject;

/**
 * 大模型调用记录服务接口
 *
 * @author betta
 */
public interface ILlmCallRecordService {

    /**
     * 创建调用记录
     *
     * @param llmConfigId   大模型配置ID
     * @param modelName     模型名称
     * @param provider      提供商
     * @param prompt 提示词
     * @return 记录ID
     */
    Long createRecord(Long llmConfigId, String modelName, String provider, String prompt);

    /**
     * 更新记录为成功状态
     *
     * @param recordId       记录ID
     * @param responseContent 响应内容（JSON格式）
     * @param duration       耗时（毫秒）
     */
    void updateSuccess(Long recordId, String responseContent, int duration);

    /**
     * 更新记录为失败状态
     *
     * @param recordId 记录ID
     * @param errorMsg 错误信息
     * @param duration 耗时（毫秒）
     */
    void updateFail(Long recordId, String errorMsg, int duration);
}
