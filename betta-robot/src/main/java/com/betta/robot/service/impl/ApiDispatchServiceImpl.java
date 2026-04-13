package com.betta.robot.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.betta.robot.client.JsonHttpClient;
import com.betta.robot.domain.MessageLlmConfig;
import com.betta.robot.domain.RobotToolConfig;
import com.betta.robot.dto.CommandDTO;
import com.betta.robot.dto.MessageProcessResult;
import com.betta.robot.mapper.MessageLlmConfigMapper;
import com.betta.robot.mapper.RobotToolConfigMapper;
import com.betta.robot.service.IApiDispatchService;
import com.betta.robot.tools.ITool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    @Override
    public MessageProcessResult processMessage(String messageText) {
        long startTime = System.currentTimeMillis();
        if (StringUtils.isBlank(messageText)) {
            return new MessageProcessResult("ERROR", null, "消息为空", 0.0);
        }

        // 1. 首先尝试使用配置的正则进行匹配
        MessageProcessResult regexMatchResult = tryMatchByRegex(messageText);
        if (regexMatchResult != null) {
            regexMatchResult.setDuration((System.currentTimeMillis() - startTime) / 1000.0);
            return regexMatchResult;
        }

        // 2. 正则不匹配，进行智能路由
        MessageProcessResult result = intelligentRoute(messageText);
        result.setDuration((System.currentTimeMillis() - startTime) / 1000.0);
        return result;
    }

    /**
     * 尝试使用配置的正则进行匹配
     *
     * @param messageText 用户消息
     * @return 执行结果，如果不匹配则返回null
     */
    private MessageProcessResult tryMatchByRegex(String messageText) {
        // 获取所有启用且配置了正则的工具
        RobotToolConfig query = new RobotToolConfig();
        query.setStatus("0");
        List<RobotToolConfig> allConfigs = robotToolConfigMapper.selectRobotToolConfigList(query);

        if (allConfigs == null || allConfigs.isEmpty()) {
            return null;
        }

        // 筛选出配置了正则表达式的工具
        List<RobotToolConfig> regexConfigs = allConfigs.stream()
                .filter(config -> StringUtils.isNotBlank(config.getRegexPattern()))
                .collect(Collectors.toList());

        if (regexConfigs.isEmpty()) {
            return null;
        }

        // 按优先级从大到小排序
        regexConfigs.sort((a, b) -> Long.compare(
                b.getPriority() != null ? b.getPriority() : 0,
                a.getPriority() != null ? a.getPriority() : 0
        ));

        // 依次尝试匹配
        for (RobotToolConfig config : regexConfigs) {
            try {
                Pattern pattern = Pattern.compile(config.getRegexPattern());
                Matcher matcher = pattern.matcher(messageText);

                if (matcher.matches()) {
                    log.info("消息匹配到正则配置：{}", config.getConfigName());

                    // 提取参数
                    String paramsJson = extractParamsFromMatcher(matcher, config.getRegexParamMap());
                    if (paramsJson == null) {
                        log.error("提取参数失败");
                        continue;
                    }

                    // 执行工具
                    String result = executeTool(config, paramsJson);
                    return new MessageProcessResult("REGEX", config.getConfigName(), result, null);
                }
            } catch (Exception e) {
                log.error("正则匹配失败，配置：{}，正则：{}", config.getConfigName(), config.getRegexPattern(), e);
            }
        }

        return null;
    }

    /**
     * 从正则匹配结果中提取参数
     *
     * @param matcher 正则匹配器
     * @param regexParamMap 参数映射JSON
     * @return 参数JSON字符串
     */
    private String extractParamsFromMatcher(Matcher matcher, String regexParamMap) {
        if (StringUtils.isBlank(regexParamMap)) {
            return "{}";
        }

        try {
            JSONObject paramMapping = JSON.parseObject(regexParamMap);
            JSONObject params = new JSONObject();

            // 遍历映射关系
            for (String key : paramMapping.keySet()) {
                String groupName = paramMapping.getString(key);
                try {
                    int groupIndex = Integer.parseInt(key);
                    if (groupIndex <= matcher.groupCount()) {
                        String groupValue = matcher.group(groupIndex);
                        if (groupValue != null) {
                            params.put(groupName, groupValue);
                        }
                    }
                } catch (NumberFormatException e) {
                    log.warn("捕获组编号不是有效数字：{}", key);
                }
            }

            return params.toJSONString();
        } catch (Exception e) {
            log.error("解析参数映射JSON失败", e);
            return null;
        }
    }

    /**
     * 智能路由：使用大模型选择最合适的工具
     *
     * @param messageText 用户消息
     * @return 执行结果或大模型回答
     */
    private MessageProcessResult intelligentRoute(String messageText) {
        // 检查"问题识别"配置是否存在
        RobotToolConfig problemRecognitionQuery = new RobotToolConfig();
        problemRecognitionQuery.setConfigName("问题识别");
        problemRecognitionQuery.setStatus("0");
        List<RobotToolConfig> problemRecognitionConfigs = robotToolConfigMapper.selectRobotToolConfigList(problemRecognitionQuery);

        if (problemRecognitionConfigs == null || problemRecognitionConfigs.isEmpty()) {
            log.error("未找到'问题识别'配置");
            return new MessageProcessResult("ERROR", null, "未找到'问题识别'配置，请先配置", null);
        }

        // 获取所有启用的工具配置（包括没有正则的）
        RobotToolConfig query = new RobotToolConfig();
        query.setStatus("0");
        List<RobotToolConfig> allConfigs = robotToolConfigMapper.selectRobotToolConfigList(query);

        if (allConfigs == null || allConfigs.isEmpty()) {
            // 没有工具配置，直接用大模型回答
            String result = askLLM(messageText);
            return new MessageProcessResult("LLM_DIRECT", null, result, null);
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
            String result = askLLM(messageText);
            return new MessageProcessResult("LLM_DIRECT", null, result, null);
        }

        // 有多个候选工具，让大模型选择
        RobotToolConfig selectedConfig = selectBestConfigByLLM(candidateConfigs, messageText);
        if (selectedConfig == null) {
            // 大模型认为都不匹配，直接用大模型回答
            String result = askLLM(messageText);
            return new MessageProcessResult("LLM_DIRECT", null, result, null);
        }

        // 大模型选择了某个工具，执行该工具
        String result = executeTool(selectedConfig, messageText);
        return new MessageProcessResult("LLM_ROUTE", selectedConfig.getConfigName(), result, null);
    }

    /**
     * 用大模型选择最合适的工具
     *
     * @param candidateConfigs 候选工具列表
     * @param messageText      用户消息
     * @return 选择的工具配置，如果不匹配则返回null
     */
    private RobotToolConfig selectBestConfigByLLM(List<RobotToolConfig> candidateConfigs, String messageText) {
        // 1. 查询"问题识别"配置
        RobotToolConfig query = new RobotToolConfig();
        query.setConfigName("问题识别");
        query.setStatus("0");
        List<RobotToolConfig> problemRecognitionConfigs = robotToolConfigMapper.selectRobotToolConfigList(query);

        if (problemRecognitionConfigs == null || problemRecognitionConfigs.isEmpty()) {
            log.warn("未找到'问题识别'配置，使用第一个候选工具");
            return candidateConfigs.get(0);
        }

        RobotToolConfig problemRecognitionConfig = problemRecognitionConfigs.get(0);

        // 2. 获取提示词和大模型配置
        String prompt = problemRecognitionConfig.getPrompt();
        if (StringUtils.isBlank(prompt)) {
            log.warn("'问题识别'配置的提示词为空，使用第一个候选工具");
            return candidateConfigs.get(0);
        }

        Long llmConfigId = problemRecognitionConfig.getLlmConfigId();
        if (llmConfigId == null) {
            log.warn("'问题识别'配置的大模型配置ID为空，使用第一个候选工具");
            return candidateConfigs.get(0);
        }

        MessageLlmConfig llmConfig = messageLlmConfigMapper.selectById(llmConfigId);
        if (llmConfig == null || StringUtils.isBlank(llmConfig.getApiKey())) {
            log.warn("'问题识别'配置的大模型配置不存在或API Key为空，使用第一个候选工具");
            return candidateConfigs.get(0);
        }

        // 3. 构造 toolArray 参数（排除"问题识别"配置本身）
        JSONArray toolArray = new JSONArray();
        for (RobotToolConfig config : candidateConfigs) {
            // 跳过"问题识别"配置（因为它是用来选择工具的，不是实际工具）
            if ("问题识别".equals(config.getConfigName())) {
                continue;
            }

            JSONObject tool = new JSONObject();
            tool.put("id", config.getId());
            tool.put("description", StringUtils.isNotBlank(config.getDescription()) ? config.getDescription() : config.getConfigName());
            toolArray.add(tool);
        }

        if (toolArray.isEmpty()) {
            log.warn("没有可用的工具，直接用大模型回答");
            return null;
        }

        // 4. 替换提示词中的占位符
        String fullPrompt = prompt.replace("{{question}}", messageText);
        fullPrompt = fullPrompt.replace("{{toolArray}}", toolArray.toJSONString());

        try {
            String llmResponse = callLlm(llmConfig, fullPrompt);
            if (StringUtils.isBlank(llmResponse)) {
                log.warn("大模型返回空响应，使用第一个候选工具");
                return candidateConfigs.get(0);
            }

            llmResponse = llmResponse.trim();

            // 5. 解析大模型返回的工具ID
            try {
                // 尝试直接解析为JSON
                JSONObject jsonResponse = JSON.parseObject(llmResponse);
                
                // 优先处理 result 数组格式
                if (jsonResponse.containsKey("result")) {
                    JSONArray resultArray = jsonResponse.getJSONArray("result");
                    if (resultArray == null || resultArray.isEmpty()) {
                        log.info("大模型返回的result数组为空，认为没有合适的工具");
                        return null;
                    }
                    JSONObject firstResult = resultArray.getJSONObject(0);
                    if (firstResult.containsKey("id")) {
                        Long selectedId = firstResult.getLong("id");
                        for (RobotToolConfig config : candidateConfigs) {
                            if (config.getId().equals(selectedId)) {
                                log.info("大模型选择了工具：{}", config.getConfigName());
                                return config;
                            }
                        }
                    }
                }
                
                // 处理直接返回 id 字段的格式
                if (jsonResponse.containsKey("id")) {
                    Long selectedId = jsonResponse.getLong("id");
                    for (RobotToolConfig config : candidateConfigs) {
                        if (config.getId().equals(selectedId)) {
                            log.info("大模型选择了工具：{}", config.getConfigName());
                            return config;
                        }
                    }
                } else if (jsonResponse.containsKey("toolId")) {
                    Long selectedId = jsonResponse.getLong("toolId");
                    for (RobotToolConfig config : candidateConfigs) {
                        if (config.getId().equals(selectedId)) {
                            log.info("大模型选择了工具：{}", config.getConfigName());
                            return config;
                        }
                    }
                } else if (jsonResponse.containsKey("selectedToolId")) {
                    Long selectedId = jsonResponse.getLong("selectedToolId");
                    for (RobotToolConfig config : candidateConfigs) {
                        if (config.getId().equals(selectedId)) {
                            log.info("大模型选择了工具：{}", config.getConfigName());
                            return config;
                        }
                    }
                }
            } catch (Exception e) {
                // JSON解析失败，尝试纯数字解析
            }

            // 检查是否回复"不匹配"
            if (llmResponse.contains("不匹配")) {
                log.info("大模型认为没有合适的工具");
                return null;
            }

            // 尝试解析工具ID（纯数字）
            try {
                Long selectedId = Long.parseLong(llmResponse.replaceAll("[^0-9]", ""));
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
        // 1. 如果配置了正则表达式，先尝试正则匹配
        if (StringUtils.isNotBlank(config.getRegexPattern())) {
            try {
                Pattern pattern = Pattern.compile(config.getRegexPattern());
                Matcher matcher = pattern.matcher(messageText);

                if (matcher.matches()) {
                    log.info("工具 {} 正则匹配成功", config.getConfigName());
                    // 提取参数
                    String paramsJson = extractParamsFromMatcher(matcher, config.getRegexParamMap());
                    if (paramsJson == null) {
                        paramsJson = "{}";
                    }
                    return callTool(config, paramsJson);
                } else {
                    log.info("工具 {} 正则匹配失败，继续使用大模型提取参数", config.getConfigName());
                }
            } catch (Exception e) {
                log.error("工具 {} 正则匹配失败：{}", config.getConfigName(), e.getMessage());
            }
        }

        // 2. 正则不匹配或没有配置正则，使用大模型提取参数
        if (config.getLlmConfigId() == null || StringUtils.isBlank(config.getPrompt())) {
            // 没有配置大模型或提示词，使用简单参数处理
            return callToolWithSimpleParams(config, messageText);
        }

        MessageLlmConfig llmConfig = messageLlmConfigMapper.selectById(config.getLlmConfigId());
        if (llmConfig == null) {
            return callToolWithSimpleParams(config, messageText);
        }

        String extractedParams = extractParamsByLlm(llmConfig, config.getPrompt(), config.getToolParams(), messageText);
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

    private String extractParamsByLlm(MessageLlmConfig llmConfig, String prompt, String toolParams, String messageText) {
        if (StringUtils.isBlank(prompt)) {
            return "{}";
        }

        // 替换提示词中的占位符
        String fullPrompt = prompt.replace("{{userQuestion}}", messageText);
        fullPrompt = fullPrompt.replace("{{params}}", toolParams);

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
