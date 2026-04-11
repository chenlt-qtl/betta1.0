package com.betta.robot.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.betta.robot.client.JsonHttpClient;
import com.betta.robot.domain.MessageLlmConfig;
import com.betta.robot.domain.RobotToolConfig;
import com.betta.robot.dto.CommandDTO;
import com.betta.robot.mapper.MessageLlmConfigMapper;
import com.betta.robot.mapper.RobotToolConfigMapper;
import com.betta.robot.service.IApiDispatchService;
import com.betta.robot.service.ILLMParseService;
import com.betta.robot.tools.ITool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ApiDispatchServiceImpl implements IApiDispatchService {

    @Autowired
    private RobotToolConfigMapper robotToolConfigMapper;

    @Autowired
    private MessageLlmConfigMapper messageLlmConfigMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ILLMParseService llmParseService;

    @Override
    public String processMessage(String messageText, String chatId, String userId, String channelType) {
        if (StringUtils.isBlank(messageText)) {
            return "消息为空";
        }

        // 1. 首先使用预设的正则规则进行匹配
        CommandDTO parsedCommand = llmParseService.parse(messageText);
        if (parsedCommand != null && !"unknown".equals(parsedCommand.getIntent())) {
            // 正则匹配成功，直接执行工具
            return executeToolWithCommandDTO(parsedCommand);
        }

        // 2. 正则不匹配，进行智能路由
        return intelligentRoute(messageText);
    }

    /**
     * 智能路由：使用大模型选择最合适的工具
     *
     * @param messageText 用户消息
     * @return 执行结果或大模型回答
     */
    private String intelligentRoute(String messageText) {
        // 获取所有启用的工具配置，按优先级从大到小排序
        RobotToolConfig query = new RobotToolConfig();
        query.setStatus("0");
        List<RobotToolConfig> allConfigs = robotToolConfigMapper.selectRobotToolConfigList(query);

        if (allConfigs == null || allConfigs.isEmpty()) {
            // 没有工具配置，直接用大模型回答
            return askLLM(messageText);
        }

        // 按优先级从大到小排序
        allConfigs.sort((a, b) -> Long.compare(
                b.getPriority() != null ? b.getPriority() : 0,
                a.getPriority() != null ? a.getPriority() : 0
        ));

        // 匹配关键字，筛选候选工具
        List<RobotToolConfig> candidateConfigs = allConfigs.stream()
                .filter(config -> matchesKeywords(messageText, config))
                .collect(Collectors.toList());

        if (candidateConfigs.isEmpty()) {
            // 没有匹配的关键字，直接用大模型回答
            return askLLM(messageText);
        }

        // 有多个候选工具，让大模型选择
        RobotToolConfig selectedConfig = selectBestConfigByLLM(candidateConfigs, messageText);
        if (selectedConfig == null) {
            // 大模型认为都不匹配，直接用大模型回答
            return askLLM(messageText);
        }

        // 大模型选择了某个工具，执行该工具
        return executeTool(selectedConfig, messageText);
    }

    /**
     * 用大模型选择最合适的工具
     *
     * @param candidateConfigs 候选工具列表
     * @param messageText      用户消息
     * @return 选择的工具配置，如果不匹配则返回null
     */
    private RobotToolConfig selectBestConfigByLLM(List<RobotToolConfig> candidateConfigs, String messageText) {
        // 获取ID为1的大模型配置
        MessageLlmConfig llmConfig = messageLlmConfigMapper.selectById(1L);
        if (llmConfig == null || StringUtils.isBlank(llmConfig.getApiKey())) {
            log.warn("未找到ID为1的大模型配置或API Key为空，使用第一个候选工具");
            return candidateConfigs.get(0);
        }

        // 构造提示词
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个智能助手，需要根据用户的消息选择最合适的工具。\n\n");
        prompt.append("用户消息：\"").append(messageText).append("\"\n\n");
        prompt.append("可选工具列表：\n");

        for (int i = 0; i < candidateConfigs.size(); i++) {
            RobotToolConfig config = candidateConfigs.get(i);
            prompt.append(i + 1).append(". 工具ID：").append(config.getId());
            prompt.append("，工具名称：").append(config.getConfigName());
            if (StringUtils.isNotBlank(config.getDescription())) {
                prompt.append("，描述：").append(config.getDescription());
            }
            prompt.append("\n");
        }

        prompt.append("\n请从以上工具中选择最合适的一个，只返回工具ID（纯数字）。");
        prompt.append("如果以上工具都不适合处理用户消息，请回复\"不匹配\"。");

        try {
            String llmResponse = callLlm(llmConfig, prompt.toString());
            if (StringUtils.isBlank(llmResponse)) {
                log.warn("大模型返回空响应，使用第一个候选工具");
                return candidateConfigs.get(0);
            }

            llmResponse = llmResponse.trim();

            // 检查是否回复"不匹配"
            if (llmResponse.contains("不匹配")) {
                log.info("大模型认为没有合适的工具");
                return null;
            }

            // 尝试解析工具ID
            try {
                Long selectedId = Long.parseLong(llmResponse);
                for (RobotToolConfig config : candidateConfigs) {
                    if (config.getId().equals(selectedId)) {
                        log.info("大模型选择了工具：{}", config.getConfigName());
                        return config;
                    }
                }
            } catch (NumberFormatException e) {
                log.warn("大模型返回的不是数字格式：{}，使用第一个候选工具", llmResponse);
            }

            // 如果无法解析，尝试匹配工具名称
            for (RobotToolConfig config : candidateConfigs) {
                if (llmResponse.contains(config.getConfigName())) {
                    log.info("大模型选择了工具（名称匹配）：{}", config.getConfigName());
                    return config;
                }
            }

            log.warn("无法从大模型响应中解析出工具ID：{}，使用第一个候选工具", llmResponse);
            return candidateConfigs.get(0);
        } catch (Exception e) {
            log.error("调用大模型选择工具失败，使用第一个候选工具", e);
            return candidateConfigs.get(0);
        }
    }

    /**
     * 用大模型直接回答用户问题
     *
     * @param messageText 用户消息
     * @return 大模型回答
     */
    private String askLLM(String messageText) {
        // 获取ID为1的大模型配置
        MessageLlmConfig llmConfig = messageLlmConfigMapper.selectById(1L);
        if (llmConfig == null || StringUtils.isBlank(llmConfig.getApiKey())) {
            return "未配置大模型，无法回答您的问题";
        }

        try {
            String response = callLlm(llmConfig, messageText);
            if (StringUtils.isNotBlank(response)) {
                return response;
            }
            return "大模型未返回有效回答";
        } catch (Exception e) {
            log.error("调用大模型回答问题失败", e);
            return "抱歉，调用大模型失败：" + e.getMessage();
        }
    }

    /**
     * 执行工具（使用CommandDTO）
     *
     * @param commandDTO 命令DTO
     * @return 执行结果
     */
    private String executeToolWithCommandDTO(CommandDTO commandDTO) {
        try {
            // 根据intent找到对应的工具
            RobotToolConfig query = new RobotToolConfig();
            query.setStatus("0");
            List<RobotToolConfig> allConfigs = robotToolConfigMapper.selectRobotToolConfigList(query);

            if (allConfigs == null || allConfigs.isEmpty()) {
                return "未配置工具";
            }

            // 根据intent匹配工具（这里需要根据业务逻辑实现）
            // 暂时返回未知命令
            return "未知命令：" + commandDTO.getIntent();
        } catch (Exception e) {
            log.error("执行工具失败", e);
            return "执行工具失败：" + e.getMessage();
        }
    }

    private boolean matchesKeywords(String messageText, RobotToolConfig config) {
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

    private String executeTool(RobotToolConfig config, String messageText) {
        if (config.getLlmConfigId() == null) {
            return callToolWithSimpleParams(config, messageText);
        }

        MessageLlmConfig llmConfig = messageLlmConfigMapper.selectById(config.getLlmConfigId());
        if (llmConfig == null) {
            return callToolWithSimpleParams(config, messageText);
        }

        String extractedParams = extractParamsByLlm(llmConfig, config.getPrompt(), messageText);
        if (extractedParams == null) {
            return "参数提取失败";
        }

        return callTool(config, extractedParams);
    }

    private String callToolWithSimpleParams(RobotToolConfig config, String messageText) {
        String toolParams = config.getToolParams();
        if (StringUtils.isBlank(toolParams)) {
            return callTool(config, "{}");
        }

        try {
            JSONObject params = JSON.parseObject(toolParams);
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String value = String.valueOf(entry.getValue());
                if (value.startsWith("${") && value.endsWith("}")) {
                    String paramName = value.substring(2, value.length() - 1);
                    String extractedValue = extractSimpleParam(messageText, paramName);
                    params.put(paramName, extractedValue);
                }
            }
            return callTool(config, params.toJSONString());
        } catch (Exception e) {
            log.error("解析工具参数失败", e);
            return callTool(config, toolParams);
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

    private String callTool(RobotToolConfig config, String params) {
        try {
            String className = config.getClassName();
            if (StringUtils.isBlank(className)) {
                return "工具类名未配置";
            }

            // 获取Bean
            Object bean = applicationContext.getBean(Class.forName(className));

            // 验证是否实现了ITool接口
            if (!(bean instanceof ITool)) {
                return "工具类未实现ITool接口";
            }

            // 创建CommandDTO，根据配置的dtoClass反序列化为具体的DTO子类
            CommandDTO commandDTO;
            String dtoClassName = config.getDtoClass();
            if (StringUtils.isNotBlank(dtoClassName)) {
                // 将JSON反序列化为具体的DTO子类
                commandDTO = (CommandDTO) JSON.parseObject(params, Class.forName(dtoClassName));
            } else {
                // 没有配置DTO类名，使用默认的CommandDTO，将params作为intent字段
                commandDTO = new CommandDTO();
                commandDTO.setIntent(params);
            }

            // 调用execute方法
            ((ITool) bean).execute(commandDTO);

            return "执行成功";
        } catch (ClassNotFoundException e) {
            log.error("工具类或DTO类不存在", e);
            return "工具类或DTO类不存在: " + e.getMessage();
        } catch (Exception e) {
            log.error("工具调用失败", e);
            return "工具调用失败: " + e.getMessage();
        }
    }

    /**
     * 调用大模型API
     *
     * @param llmConfig LLM配置
     * @param prompt    提示词
     * @return 大模型回复内容
     */
    private String callLlm(MessageLlmConfig llmConfig, String prompt) {
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

            // 发送请求
            String response = JsonHttpClient.postJson(endpoint, requestBody, apiKey);

            if (StringUtils.isBlank(response)) {
                log.error("LLM返回空响应");
                return null;
            }

            // 解析响应，从OpenAI格式的响应中提取内容
            JSONObject jsonResponse = JSON.parseObject(response);
            if (jsonResponse.containsKey("choices")) {
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject choice = choices.getJSONObject(0);
                    if (choice.containsKey("message")) {
                        JSONObject messageObj = choice.getJSONObject("message");
                        if (messageObj.containsKey("content")) {
                            return messageObj.getString("content");
                        }
                    }
                }
            }

            log.error("LLM响应格式不正确: {}", response);
            return null;
        } catch (Exception e) {
            log.error("调用LLM失败", e);
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
