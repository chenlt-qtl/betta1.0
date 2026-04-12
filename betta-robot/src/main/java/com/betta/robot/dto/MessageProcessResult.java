package com.betta.robot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息处理结果DTO
 *
 * @author betta
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageProcessResult {

    /**
     * 匹配类型：REGEX（正则匹配）、LLM_ROUTE（大模型路由）、LLM_DIRECT（大模型直接回答）
     */
    private String matchType;

    /**
     * 工具名称（如果有）
     */
    private String toolName;

    /**
     * 执行结果或回答
     */
    private String result;

    /**
     * 耗时（秒）
     */
    private Double duration;
}
