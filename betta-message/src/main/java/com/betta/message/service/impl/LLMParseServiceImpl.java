package com.betta.message.service.impl;

import com.betta.message.dto.CommandDTO;
import com.betta.message.service.ILLMParseService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 解析实现：内置简单规则 + 可扩展为调用大模型 API
 * 示例规则：豆芽加卡20张 -> add_card(target=豆芽, quantity=20)；启动xxx任务 -> start_task(taskName=xxx)
 *
 * @author betta
 */
@Slf4j
@Service
public class LLMParseServiceImpl implements ILLMParseService {

    /** 加卡：xxx加卡N张 / xxx 加卡 N */
    private static final Pattern ADD_CARD_PATTERN = Pattern.compile("(.+?)(?:加卡|加\\s*卡)\\s*([\\d]+)\\s*张?");

    /** 启动任务：启动xxx / 马上跑xxx */
    private static final Pattern START_TASK_PATTERN = Pattern.compile("(?:启动|运行|执行|马上跑一下?)\\s*(.+)");

    @Override
    public CommandDTO parse(String rawText) {
        if (StringUtils.isBlank(rawText)) {
            return unknown();
        }
        String text = rawText.trim();

        // 1) 加卡
        Matcher addCard = ADD_CARD_PATTERN.matcher(text);
        if (addCard.find()) {
            CommandDTO dto = new CommandDTO();
            dto.setIntent("add_card");
            dto.setTarget(addCard.group(1).trim());
            try {
                dto.setQuantity(Integer.parseInt(addCard.group(2)));
            } catch (NumberFormatException e) {
                dto.setQuantity(0);
            }
            return dto;
        }

        // 2) 启动任务
        Matcher startTask = START_TASK_PATTERN.matcher(text);
        if (startTask.find()) {
            CommandDTO dto = new CommandDTO();
            dto.setIntent("start_task");
            dto.setTaskName(startTask.group(1).trim());
            return dto;
        }

        // 可在此处调用真实 LLM API，将 prompt 和 text 发送，解析返回的 JSON 为 CommandDTO
        // return callRealLLM(text);
        return unknown();
    }

    /**
     * 返回未知意图
     *
     * @return CommandDTO intent=unknown
     */
    private static CommandDTO unknown() {
        CommandDTO dto = new CommandDTO();
        dto.setIntent("unknown");
        return dto;
    }
}
