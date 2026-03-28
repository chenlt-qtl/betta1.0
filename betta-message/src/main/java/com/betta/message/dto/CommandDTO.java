package com.betta.message.dto;

import lombok.Data;

/**
 * LLM 解析后的命令结构
 *
 * @author betta
 */
@Data
public class CommandDTO {

    /** 意图：add_card / start_task / unknown */
    private String intent;
    /** 对象/名称，如 豆芽 */
    private String target;
    /** 数量 */
    private Integer quantity;
    /** 任务名称（启动任务时） */
    private String taskName;
}
