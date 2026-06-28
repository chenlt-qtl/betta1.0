package com.betta.robot.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.betta.robot.client.JsonHttpClient;
import com.betta.robot.domain.MessageLlmConfig;
import com.betta.robot.service.ILlmCallRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class LlmUtils {

    @Autowired
    private ILlmCallRecordService llmCallRecordService;

    /**
     * 调用大模型API并记录调用日志
     *
     * @param llmConfig 大模型配置
     * @param prompt    提示词
     * @return 大模型回复内容
     */
    public String callWithRecord(MessageLlmConfig llmConfig, String prompt) {
        long startTime = System.currentTimeMillis();
        Long recordId = null;

        try {
            String endpoint = resolveEndpoint(llmConfig);
            String apiKey = llmConfig.getApiKey();

            // 构造OpenAI格式的请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", llmConfig.getModelName());

            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            requestBody.put("messages", messages);

            // 添加可选参数
            if (llmConfig.getTemperature() != null && llmConfig.getTemperature().compareTo(BigDecimal.ZERO) >= 0) {
                requestBody.put("temperature", llmConfig.getTemperature().doubleValue());
            }
            if (llmConfig.getMaxTokens() != null && llmConfig.getMaxTokens() > 0) {
                requestBody.put("max_tokens", llmConfig.getMaxTokens());
            }
            if (llmConfig.getTopP() != null && llmConfig.getTopP().compareTo(BigDecimal.ZERO) > 0) {
                requestBody.put("top_p", llmConfig.getTopP().doubleValue());
            }

            // 记录请求参数
            log.info("发送到大模型的请求参数：{}", requestBody.toJSONString());

            // 创建数据库记录
            recordId = llmCallRecordService.createRecord(
                llmConfig.getId(),
                llmConfig.getModelName(),
                llmConfig.getProvider(), prompt
            );

            // 发送请求
            String response = JsonHttpClient.postJson(endpoint, requestBody, apiKey);

            if (StringUtils.isBlank(response)) {
                log.error("LLM返回空响应");

                // 更新记录为失败
                if (recordId != null) {
                    llmCallRecordService.updateFail(recordId, "LLM返回空响应", (int) (System.currentTimeMillis() - startTime));
                }
                return null;
            }

            // 记录大模型返回的原始响应
            log.info("大模型返回的原始结果：{}", response);

            // 解析响应，从OpenAI格式的响应中提取内容
            JSONObject jsonResponse = JSONObject.parseObject(response);
            if (jsonResponse.containsKey("choices")) {
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject choice = choices.getJSONObject(0);
                    if (choice.containsKey("message")) {
                        JSONObject messageObj = choice.getJSONObject("message");
                        if (messageObj.containsKey("content")) {
                            String content = messageObj.getString("content");
                            // 更新记录
                            if (recordId != null) {
                                llmCallRecordService.updateSuccess(recordId, content, (int) (System.currentTimeMillis() - startTime));
                            }
                            return content;
                        }
                    }
                }
            }

            llmCallRecordService.updateFail(recordId,"LLM响应格式不正确", (int) (System.currentTimeMillis() - startTime));
            log.error("LLM响应格式不正确: {}", response);
            return null;
        } catch (Exception e) {
            log.error("调用LLM失败", e);

            // 更新记录为失败
            if (recordId != null) {
                llmCallRecordService.updateFail(recordId, e.getMessage(), (int) (System.currentTimeMillis() - startTime));
            }
            return null;
        }
    }

    /**
     * 根据提供商解析API端点
     *
     * @param llmConfig LLM配置
     * @return API端点URL
     */
    private String resolveEndpoint(MessageLlmConfig llmConfig) {
        // 如果配置了自定义端点，优先使用
        if (StringUtils.isNotBlank(llmConfig.getApiEndpoint())) {
            return llmConfig.getApiEndpoint();
        }

        // 根据provider类型返回默认端点
        String provider = llmConfig.getProvider();
        if (StringUtils.isBlank(provider)) {
            provider = "OPENAI"; // 默认使用OpenAI
        }

        switch (provider.toUpperCase()) {
            case "ALIYUN":
                return "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
            case "OPENAI":
                return "https://api.openai.com/v1/chat/completions";
            case "ANTHROPIC":
                // Anthropic暂不支持标准OpenAI格式，需要特殊处理
                return "https://api.anthropic.com/v1/messages";
            case "BAIDU":
                // 百度文心一言需要特殊适配
                return "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions";
            default:
                // 默认使用OpenAI端点
                return "https://api.openai.com/v1/chat/completions";
        }
    }
}
