package com.betta.message.service.impl;

import com.betta.common.utils.StringUtils;
import com.betta.message.dto.ActionResult;
import com.betta.message.dto.CommandDTO;
import com.betta.message.service.ICommandDispatchService;
import com.betta.quartz.domain.SysJob;
import com.betta.quartz.service.ISysJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.quartz.SchedulerException;
import java.util.List;

/**
 * 命令分发实现：add_card 暂不支持，start_task 立即执行定时任务
 *
 * @author betta
 */
@Slf4j
@Service
public class CommandDispatchServiceImpl implements ICommandDispatchService {

    @Autowired
    private ISysJobService jobService;

    @Override
    public ActionResult dispatch(CommandDTO command) {
        if (command == null || StringUtils.isEmpty(command.getIntent())) {
            return ActionResult.fail("无效命令");
        }
        switch (command.getIntent()) {
            case "add_card":
                return doAddCard(command);
            case "start_task":
                return doStartTask(command);
            default:
                return ActionResult.fail("暂不支持该指令，请换个说法");
        }
    }

    /**
     * 加卡：功能暂未迁移，返回提示信息
     *
     * @param command 命令（target=名称, quantity=数量）
     * @return 执行结果
     */
    private ActionResult doAddCard(CommandDTO command) {
        // TODO: betta-other 模块未迁移，加卡功能暂不可用
        return ActionResult.fail("加卡功能暂未实现，请先迁移 betta-other 模块或修改为支持 start_task 功能");
    }

    /**
     * 启动定时任务：按任务名称查找后立即执行一次
     *
     * @param command 命令（taskName=任务名称）
     * @return 执行结果
     */
    private ActionResult doStartTask(CommandDTO command) {
        String taskName = StringUtils.isNotBlank(command.getTaskName()) ? command.getTaskName() : command.getTarget();
        if (StringUtils.isEmpty(taskName)) {
            return ActionResult.fail("请说明要启动的任务名称");
        }
        try {
            SysJob query = new SysJob();
            query.setJobName(taskName);
            List<SysJob> list = jobService.selectJobList(query);
            if (list == null || list.isEmpty()) {
                return ActionResult.fail("未找到任务：" + taskName);
            }
            SysJob job = list.get(0);
            boolean run = jobService.run(job);
            if (run) {
                return ActionResult.ok("任务【" + taskName + "】已触发执行");
            } else {
                return ActionResult.fail("任务触发失败，请检查任务状态");
            }
        } catch (SchedulerException e) {
            log.error("启动任务失败: taskName={}", taskName, e);
            return ActionResult.fail("启动任务失败：" + e.getMessage());
        }
    }
}
