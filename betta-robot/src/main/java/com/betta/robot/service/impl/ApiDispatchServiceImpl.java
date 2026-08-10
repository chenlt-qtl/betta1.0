package com.betta.robot.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.betta.robot.domain.MessageLlmConfig;
import com.betta.robot.domain.RobotToolConfig;
import com.betta.robot.dto.ActionResult;
import com.betta.robot.dto.CommandDTO;
import com.betta.robot.dto.MessageProcessResult;
import com.betta.robot.mapper.MessageLlmConfigMapper;
import com.betta.robot.mapper.RobotToolConfigMapper;
import com.betta.robot.service.IApiDispatchService;
import com.betta.robot.tools.ITool;
import com.betta.robot.utils.LlmUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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

    @Autowired
    private LlmUtils llmUtils;

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

                    // 正则已完成匹配和参数提取，直接调用工具，避免对参数 JSON 重复执行正则匹配。
                    String result = callTool(config, paramsJson);
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
                //过滤className不为空的 提示词不为空 问题包含关键字
                .filter(config -> StringUtils.isNotBlank( config.getClassName()) &&
                        matchesKeywords(messageText, config) &&
                        StringUtils.isNotBlank(config.getPrompt()))
                .collect(Collectors.toList());

        if (candidateConfigs.isEmpty()) {
            // 没有匹配的关键字，直接用大模型回答
            String result = askLLM(messageText);
            return new MessageProcessResult("LLM_DIRECT", null, result, null);
        }

        // 让大模型选择工具（可能返回多个）
        List<ToolWithQuestion> toolWithQuestions = selectBestConfigByLLM(candidateConfigs, messageText);
        if (toolWithQuestions.isEmpty()) {
            // 大模型认为都不匹配，直接用大模型回答
            String result = askLLM(messageText);
            return new MessageProcessResult("LLM_DIRECT", null, result, null);
        }

        // 循环执行每个工具
        JSONArray results = new JSONArray();
        boolean hasError = false;

        for (ToolWithQuestion toolWithQuestion : toolWithQuestions) {
            try {
                String result = executeTool(toolWithQuestion.config, toolWithQuestion.question);

                JSONObject resultItem = new JSONObject();
                resultItem.put("toolName", toolWithQuestion.config.getConfigName());
                resultItem.put("question", toolWithQuestion.question);
                resultItem.put("result", result);
                resultItem.put("success", true);
                results.add(resultItem);
            } catch (Exception e) {
                log.error("执行工具失败：{}，子问题：{}", toolWithQuestion.config.getConfigName(), toolWithQuestion.question, e);
                hasError = true;

                JSONObject resultItem = new JSONObject();
                resultItem.put("toolName", toolWithQuestion.config.getConfigName());
                resultItem.put("question", toolWithQuestion.question);
                resultItem.put("result", "执行失败：" + e.getMessage());
                resultItem.put("success", false);
                results.add(resultItem);
            }
        }

        // 返回所有执行结果
        JSONObject finalResult = new JSONObject();
        finalResult.put("status", hasError ? "PARTIAL_SUCCESS" : "SUCCESS");
        finalResult.put("results", results);

        return new MessageProcessResult("LLM_MULTI_ROUTE", "多工具执行", finalResult.toJSONString(), null);
    }

    /**
     * 大模型拆解问题并选择工具
     *
     * @param candidateConfigs 候选工具列表
     * @param messageText      用户消息
     * @return 带子问题的工具配置列表
     */
    private List<ToolWithQuestion> selectBestConfigByLLM(List<RobotToolConfig> candidateConfigs, String messageText) {
        // 1. 查询"问题识别"配置
        RobotToolConfig query = new RobotToolConfig();
        query.setConfigName("问题识别");
        query.setStatus("0");
        List<RobotToolConfig> problemRecognitionConfigs = robotToolConfigMapper.selectRobotToolConfigList(query);

        if (problemRecognitionConfigs == null || problemRecognitionConfigs.isEmpty()) {
            log.warn("未找到'问题识别'配置");
            return Collections.emptyList();
        }

        RobotToolConfig problemRecognitionConfig = problemRecognitionConfigs.get(0);

        // 2. 获取提示词和大模型配置
        String prompt = problemRecognitionConfig.getPrompt();
        if (StringUtils.isBlank(prompt)) {
            log.warn("'问题识别'配置的提示词为空");
            return Collections.emptyList();
        }

        Long llmConfigId = problemRecognitionConfig.getLlmConfigId();
        if (llmConfigId == null) {
            log.warn("'问题识别'配置的大模型配置ID为空");
            return Collections.emptyList();
        }

        MessageLlmConfig llmConfig = messageLlmConfigMapper.selectById(llmConfigId);
        if (llmConfig == null || StringUtils.isBlank(llmConfig.getApiKey())) {
            log.warn("'问题识别'配置的大模型配置不存在或API Key为空");
            return Collections.emptyList();
        }

        // 3. 构造 toolArray 参数（排除"问题识别"配置本身）
        JSONArray toolArray = new JSONArray();
        for (RobotToolConfig config : candidateConfigs) {
            JSONObject tool = new JSONObject();
            tool.put("id", config.getId());
            tool.put("description", StringUtils.isNotBlank(config.getDescription()) ? config.getDescription() : config.getConfigName());
            toolArray.add(tool);
        }

        if (toolArray.isEmpty()) {
            log.warn("没有可用的工具，直接用大模型回答");
            return Collections.emptyList();
        }

        // 4. 替换提示词中的占位符
        String fullPrompt = prompt.replace("{{question}}", messageText);
        fullPrompt = fullPrompt.replace("{{toolArray}}", toolArray.toJSONString());

        try {
            String llmResponse = llmUtils.callWithRecord(llmConfig, fullPrompt);
            if (StringUtils.isBlank(llmResponse)) {
                log.warn("大模型返回空响应");
                return Collections.emptyList();
            }

            llmResponse = llmResponse.trim();

            // 检查是否返回空数组
            if ("[]".equals(llmResponse)) {
                log.info("大模型返回空数组，认为没有合适的工具");
                return Collections.emptyList();
            }

            // 解析大模型返回的结果
            try {
                JSONObject jsonResponse = JSON.parseObject(llmResponse);
                JSONArray resultArray = jsonResponse.getJSONArray("result");

                if (resultArray == null || resultArray.isEmpty()) {
                    log.info("大模型返回的result数组为空");
                    return Collections.emptyList();
                }

                List<ToolWithQuestion> toolList = new ArrayList<>();
                for (int i = 0; i < resultArray.size(); i++) {
                    JSONObject resultItem = resultArray.getJSONObject(i);
                    if (resultItem.containsKey("id")) {
                        Long selectedId = resultItem.getLong("id");
                        String question = resultItem.getString("question");

                        for (RobotToolConfig config : candidateConfigs) {
                            if (config.getId().equals(selectedId)) {
                                log.info("大模型选择了工具：{}，子问题：{}", config.getConfigName(), question);
                                toolList.add(new ToolWithQuestion(config, question));
                                break;
                            }
                        }
                    }
                }

                return toolList;
            } catch (Exception e) {
                log.error("解析大模型响应失败", e);
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("调用大模型选择工具失败", e);
            return Collections.emptyList();
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
            String response = llmUtils.callWithRecord(llmConfig, messageText);
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
        }else{
            log.info("参数提取成功：{}", extractedParams);
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
            String llmResponse = llmUtils.callWithRecord(llmConfig, fullPrompt);
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

    /**
     * 加载配置指定的工具 Bean，并使用合并后的 Map 参数执行工具。
     *
     * @param config 工具配置，提供工具类名和基础参数 JSON
     * @param params 动态参数 JSON，来源可以是正则捕获、大模型提取或简单参数提取
     * @return 工具返回的用户提示；调用失败时返回明确错误信息
     */
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

            // toolParams 是完整基础参数，正则或大模型提取的动态值覆盖其中同名占位值。
            Map<String, Object> mergedParams = mergeToolParams(config.getToolParams(), params);
            ActionResult actionResult = ((ITool) bean).execute(mergedParams);
            if (actionResult == null) {
                return "执行成功";
            }
            if (StringUtils.isNotBlank(actionResult.getMessage())) {
                return actionResult.getMessage();
            }
            return actionResult.isSuccess() ? "执行成功" : "执行失败";
        } catch (ClassNotFoundException e) {
            log.error("工具类不存在", e);
            return "工具类不存在: " + e.getMessage();
        } catch (Exception e) {
            log.error("工具调用失败", e);
            return "工具调用失败: " + e.getMessage();
        }
    }

    /**
     * 合并工具基础参数和动态参数，并保证动态值覆盖同名基础值。
     *
     * @param toolParams 基础参数 JSON，空值按空 Map 处理
     * @param dynamicParams 动态参数 JSON，空值按空 Map 处理
     * @return 合并后的独立参数 Map，保留 JSON 中数字、布尔值等原始类型
     */
    private Map<String, Object> mergeToolParams(String toolParams, String dynamicParams) {
        Map<String, Object> mergedParams = new LinkedHashMap<>();
        mergedParams.putAll(parseParamJson(toolParams));
        // 后写入动态参数，使正则捕获值或大模型提取值覆盖同名固定占位值。
        mergedParams.putAll(parseParamJson(dynamicParams));
        return mergedParams;
    }

    /**
     * 将参数 JSON 解析为 Map。
     *
     * @param paramsJson 参数 JSON，允许为空
     * @return 非空参数 Map；空 JSON 或 JSON null 返回空 Map
     */
    private Map<String, Object> parseParamJson(String paramsJson) {
        if (StringUtils.isBlank(paramsJson)) {
            return Collections.emptyMap();
        }
        JSONObject params = JSON.parseObject(paramsJson);
        return params == null ? Collections.emptyMap() : params;
    }

    /**
     * 带子问题的工具配置
     */
    private static class ToolWithQuestion {
        RobotToolConfig config;
        String question;

        ToolWithQuestion(RobotToolConfig config, String question) {
            this.config = config;
            this.question = question;
        }
    }

}
