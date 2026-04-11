package com.betta.robot.service;

import com.betta.robot.dto.ActionResult;
import com.betta.robot.dto.CommandDTO;

/**
 * 命令分发：根据解析结果调用对应 API 或触发定时任务
 *
 * @author betta
 */
public interface ICommandDispatchService {

    /**
     * 执行命令
     *
     * @param command 解析后的命令
     * @return 执行结果
     */
    ActionResult dispatch(CommandDTO command);
}
