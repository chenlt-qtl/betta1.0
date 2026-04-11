package com.betta.robot.dto;

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
}
