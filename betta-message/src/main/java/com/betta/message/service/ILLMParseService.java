package com.betta.message.service;

import com.betta.message.dto.CommandDTO;

/**
 * 大模型解析服务：将用户自然语言解析为结构化命令
 * 可替换为真实 LLM API（如 OpenAI/通义/豆包等）
 *
 * @author betta
 */
public interface ILLMParseService {

    /**
     * 解析用户输入为命令
     *
     * @param rawText 用户原文，如 "豆芽加卡20张"、"启动备份任务"
     * @return 解析结果，intent 为 unknown 时表示无法识别
     */
    CommandDTO parse(String rawText);
}
