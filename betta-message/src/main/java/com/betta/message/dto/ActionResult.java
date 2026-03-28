package com.betta.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 命令执行结果
 *
 * @author betta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionResult {

    private boolean success;
    private String message;
    private Object data;

    public static ActionResult ok(String message) {
        return ActionResult.builder().success(true).message(message).build();
    }

    public static ActionResult fail(String message) {
        return ActionResult.builder().success(false).message(message).build();
    }
}
