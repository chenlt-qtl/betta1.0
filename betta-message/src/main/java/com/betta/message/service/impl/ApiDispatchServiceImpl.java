package com.betta.message.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.betta.message.domain.MessageApiConfig;
import com.betta.message.domain.MessageLlmConfig;
import com.betta.message.mapper.MessageApiConfigMapper;
import com.betta.message.mapper.MessageLlmConfigMapper;
import com.betta.message.service.IApiDispatchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ApiDispatchServiceImpl implements IApiDispatchService {

    @Autowired
    private MessageApiConfigMapper messageApiConfigMapper;

    @Autowired
    private MessageLlmConfigMapper messageLlmConfigMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Override
    public String processMessage(String messageText, String chatId, String userId, String channelType) {
        if (StringUtils.isBlank(messageText)) {
            return "消息为空";
        }

        MessageApiConfig query = new MessageApiConfig();
        query.setStatus("0");
        List<MessageApiConfig> allConfigs = messageApiConfigMapper.selectList(query);

        if (allConfigs == null || allConfigs.isEmpty()) {
            return "未配置API";
        }

        List<MessageApiConfig> matchedConfigs = allConfigs.stream()
                .filter(config -> matchesKeywords(messageText, config))
                .sorted((a, b) -> Integer.compare(
                        b.getPriority() != null ? b.getPriority() : 0,
                        a.getPriority() != null ? a.getPriority() : 0
                ))
                .collect(Collectors.toList());

        if (matchedConfigs.isEmpty()) {
            return "无匹配的API配置";
        }

        if (matchedConfigs.size() == 1) {
            return executeApi(matchedConfigs.get(0), messageText);
        }

        MessageApiConfig selectedConfig = selectBestConfigByLlm(matchedConfigs, messageText);
        if (selectedConfig == null) {
            return "无法选择合适的API配置";
        }

        return executeApi(selectedConfig, messageText);
    }

    private boolean matchesKeywords(String messageText, MessageApiConfig config) {
        String keywords = config.getKeywords();
        if (StringUtils.isBlank(keywords)) {
            return true;
        }

        String[] keywordArray = keywords.split(",");
        for (String keyword : keywordArray) {
            if (StringUtils.isNotBlank(keyword) && messageText.contains(keyword.trim())) {
                return true;
            }
        }
        return false;
    }

    private MessageApiConfig selectBestConfigByLlm(List<MessageApiConfig> configs, String messageText) {
        if (configs.isEmpty()) {
            return null;
        }

        MessageApiConfig firstConfig = configs.get(0);
        if (firstConfig.getLlmConfigId() == null) {
            return firstConfig;
        }

        MessageLlmConfig llmConfig = messageLlmConfigMapper.selectById(firstConfig.getLlmConfigId());
        if (llmConfig == null || StringUtils.isBlank(llmConfig.getApiKey())) {
            return firstConfig;
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("根据用户消息：\"").append(messageText).append("\"\n\n");
        prompt.append("从以下API配置中选择最合适的一个，只返回配置名称：\n");

        for (int i = 0; i < configs.size(); i++) {
            MessageApiConfig config = configs.get(i);
            prompt.append(i + 1).append(". ");
            if (StringUtils.isNotBlank(config.getDescription())) {
                prompt.append(config.getDescription());
            } else {
                prompt.append(config.getConfigName());
            }
            prompt.append("\n");
        }

        prompt.append("\n只返回配置名称，不要其他文字。");

        try {
            String llmResponse = callLlm(llmConfig, prompt.toString());
            if (StringUtils.isNotBlank(llmResponse)) {
                for (MessageApiConfig config : configs) {
                    if (llmResponse.contains(config.getConfigName())) {
                        return config;
                    }
                }
            }
        } catch (Exception e) {
            log.error("LLM选择配置失败，使用默认选择", e);
        }

        return configs.get(0);
    }

    private String executeApi(MessageApiConfig config, String messageText) {
        if (config.getLlmConfigId() == null) {
            return callApiWithSimpleParams(config, messageText);
        }

        MessageLlmConfig llmConfig = messageLlmConfigMapper.selectById(config.getLlmConfigId());
        if (llmConfig == null) {
            return callApiWithSimpleParams(config, messageText);
        }

        String extractedParams = extractParamsByLlm(llmConfig, config.getPrompt(), messageText);
        if (extractedParams == null) {
            return "参数提取失败";
        }

        return callApi(config, extractedParams);
    }

    private String callApiWithSimpleParams(MessageApiConfig config, String messageText) {
        String apiParams = config.getApiParams();
        if (StringUtils.isBlank(apiParams)) {
            return callApi(config, "{}");
        }

        try {
            JSONObject params = JSON.parseObject(apiParams);
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String value = String.valueOf(entry.getValue());
                if (value.startsWith("${") && value.endsWith("}")) {
                    String paramName = value.substring(2, value.length() - 1);
                    String extractedValue = extractSimpleParam(messageText, paramName);
                    params.put(paramName, extractedValue);
                }
            }
            return callApi(config, params.toJSONString());
        } catch (Exception e) {
            log.error("解析API参数失败", e);
            return callApi(config, apiParams);
        }
    }

    private String extractSimpleParam(String messageText, String paramName) {
        return messageText.replaceAll(".*?" + paramName + "[是为：:]*\\s*", "").trim();
    }

    private String extractParamsByLlm(MessageLlmConfig llmConfig, String prompt, String messageText) {
        if (StringUtils.isBlank(prompt)) {
            return "{}";
        }

        String fullPrompt = prompt + "\n\n用户消息：" + messageText + "\n\n请从消息中提取参数，输出JSON格式。";

        try {
            String llmResponse = callLlm(llmConfig, fullPrompt);
            llmResponse = llmResponse.trim();
            if (llmResponse.startsWith("```json")) {
                llmResponse = llmResponse.substring(7);
            }
            if (llmResponse.startsWith("```")) {
                llmResponse = llmResponse.substring(3);
            }
            if (llmResponse.endsWith("```")) {
                llmResponse = llmResponse.substring(0, llmResponse.length() - 3);
            }
            llmResponse = llmResponse.trim();

            JSON.parseObject(llmResponse);
            return llmResponse;
        } catch (Exception e) {
            log.error("LLM提取参数失败", e);
            return null;
        }
    }

    private String callLlm(MessageLlmConfig config, String prompt) throws Exception {
        String apiUrl = config.getApiEndpoint();
        if (StringUtils.isBlank(apiUrl)) {
            apiUrl = "https://api.openai.com/v1/chat/completions";
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", config.getModelName());
        requestBody.put("temperature", config.getTemperature() != null ? config.getTemperature().doubleValue() : 0.7);
        requestBody.put("max_tokens", config.getMaxTokens() != null ? config.getMaxTokens() : 2000);

        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        requestBody.put("messages", new JSONObject[]{message});

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toJSONString()))
                .timeout(Duration.ofSeconds(config.getTimeout() != null ? config.getTimeout() : 30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject responseJson = JSON.parseObject(response.body());
            var choices = responseJson.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                return choices.getJSONObject(0).getJSONObject("message").getString("content");
            }
        }

        log.error("LLM调用失败: {}", response.body());
        return null;
    }

    private String callApi(MessageApiConfig config, String params) {
        try {
            String apiUrl = config.getApiUrl();
            if (StringUtils.isBlank(apiUrl)) {
                return "API地址未配置";
            }

            String filledUrl = fillUrlParams(apiUrl, params);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(filledUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(params))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return "调用成功: " + response.body();
            } else {
                return "调用失败: " + response.statusCode() + " - " + response.body();
            }
        } catch (Exception e) {
            log.error("API调用失败", e);
            return "调用失败: " + e.getMessage();
        }
    }

    private String fillUrlParams(String url, String params) {
        try {
            JSONObject paramJson = JSON.parseObject(params);
            for (Map.Entry<String, Object> entry : paramJson.entrySet()) {
                url = url.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }
        } catch (Exception e) {
            log.warn("填充URL参数失败", e);
        }
        return url;
    }
}
