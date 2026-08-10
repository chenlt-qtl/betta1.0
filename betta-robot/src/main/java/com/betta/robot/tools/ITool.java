package com.betta.robot.tools;

import com.betta.robot.dto.ActionResult;

import java.util.Map;

/**
 * 机器人业务工具统一执行接口。
 *
 * <p>所有工具均接收键值形式的参数，固定配置参数与动态提取参数由调用方合并后传入。</p>
 */
public interface ITool {

    /**
     * 执行工具业务。
     *
     * @param params 工具参数，键为配置的参数名，值为固定值或动态提取值
     * @return 工具执行结果，包含成功状态和面向用户的提示信息
     */
    ActionResult execute(Map<String, Object> params);
}
