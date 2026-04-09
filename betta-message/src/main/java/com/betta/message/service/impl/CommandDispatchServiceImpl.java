package com.betta.message.service.impl;

import com.betta.common.utils.StringUtils;
import com.betta.message.dto.ActionResult;
import com.betta.message.dto.AddCardDTO;
import com.betta.message.dto.CommandDTO;
import com.betta.message.service.ICommandDispatchService;
import com.betta.quartz.service.ISysJobService;
import com.betta.system.domain.CardAccount;
import com.betta.system.domain.CardHistory;
import com.betta.system.service.ICardAccountService;
import com.betta.system.service.ICardHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 命令分发实现：add_card 加卡，query_balance 查询余额，start_task 立即执行定时任务
 *
 * @author betta
 */
@Slf4j
@Service
public class CommandDispatchServiceImpl implements ICommandDispatchService {

    @Autowired
    private ISysJobService jobService;

    @Autowired
    private ICardAccountService cardAccountService;

    @Autowired
    private ICardHistoryService cardHistoryService;

    @Override
    public ActionResult dispatch(CommandDTO command) {
        if (command == null || StringUtils.isEmpty(command.getIntent())) {
            return ActionResult.fail("无效命令");
        }
        switch (command.getIntent()) {
            case "add_card":
                return doAddCard(command);
            case "query_balance":
                return doQueryBalance(command);
            case "start_task":
                return doStartTask(command);
            default:
                return ActionResult.fail("暂不支持该指令，请换个说法");
        }
    }

    /**
     * 加卡：为指定账户添加卡片
     *
     * @param command 命令（account=名称, quantity=数量, conent=内容）
     * @return 执行结果
     */
    private ActionResult doAddCard(CommandDTO command) {
        AddCardDTO addCardDTO = (AddCardDTO) command;

        // 参数校验
        String accountName = addCardDTO.getAccount();
        if (StringUtils.isEmpty(accountName)) {
            return ActionResult.fail("请说明要加卡的人员名称");
        }

        Integer quantity = addCardDTO.getQuantity();
        if (quantity == null) {
            return ActionResult.fail("请指定加卡数量");
        }

        try {
            // 查询或创建账户
            CardAccount account = cardAccountService.selectCardAccountByName(accountName);
            if (account == null) {
                return ActionResult.fail("帐户"+accountName+"不存在");
            }

            // 更新余额
            int newBalance = account.getBalance() + quantity;
            account.setBalance(newBalance);
            account.setUpdateBy("robot");
            cardAccountService.updateCardAccount(account);

            // 记录历史
            CardHistory history = new CardHistory();
            history.setAccountId(account.getId());
            history.setChangeValue(quantity);
            history.setRemainValue(newBalance);
            history.setContent(StringUtils.isEmpty(addCardDTO.getConent()) ? "" : addCardDTO.getConent());
            history.setCreateBy("robot");
            cardHistoryService.insertCardHistory(history);

            log.info("加卡成功: {} +{} -> {}", accountName, quantity, newBalance);
            return ActionResult.ok("已为【" + accountName + "】加卡 " + quantity + " 张，当前余额 " + newBalance + " 张");

        } catch (Exception e) {
            log.error("加卡失败: accountName={}, quantity={}", accountName, quantity, e);
            return ActionResult.fail("加卡失败：" + e.getMessage());
        }
    }

    /**
     * 查询余额：查询所有账户的卡数余额
     *
     * @param command 命令
     * @return 执行结果
     */
    private ActionResult doQueryBalance(CommandDTO command) {
        try {
            List<CardAccount> accounts = cardAccountService.selectCardAccountList(new CardAccount());
            if (accounts == null || accounts.isEmpty()) {
                return ActionResult.ok("当前还没有任何账户");
            }

            StringBuilder result = new StringBuilder("当前所有账户余额：\n");
            int total = 0;
            for (CardAccount account : accounts) {
                result.append("• ").append(account.getName()).append("：").append(account.getBalance()).append(" 张\n");
                total += account.getBalance();
            }
            result.append("\n总计：").append(total).append(" 张");

            log.info("查询余额成功：共 {} 个账户，总计 {} 张卡", accounts.size(), total);
            return ActionResult.ok(result.toString());

        } catch (Exception e) {
            log.error("查询余额失败", e);
            return ActionResult.fail("查询余额失败：" + e.getMessage());
        }
    }

    /**
     * 启动定时任务：按任务名称查找后立即执行一次
     *
     * @param command 命令（taskName=任务名称）
     * @return 执行结果
     */
    private ActionResult doStartTask(CommandDTO command) {
//        String taskName = StringUtils.isNotBlank(command.getTaskName()) ? command.getTaskName() : command.getTarget();
//        if (StringUtils.isEmpty(taskName)) {
//            return ActionResult.fail("请说明要启动的任务名称");
//        }
//        try {
//            SysJob query = new SysJob();
//            query.setJobName(taskName);
//            List<SysJob> list = jobService.selectJobList(query);
//            if (list == null || list.isEmpty()) {
//                return ActionResult.fail("未找到任务：" + taskName);
//            }
//            SysJob job = list.get(0);
//            boolean run = jobService.run(job);
//            if (run) {
//                return ActionResult.ok("任务【" + taskName + "】已触发执行");
//            } else {
                return ActionResult.fail("任务触发失败，请检查任务状态");
//            }
//        } catch (SchedulerException e) {
//            log.error("启动任务失败: taskName={}", taskName, e);
//            return ActionResult.fail("启动任务失败：" + e.getMessage());
//        }
    }
}
