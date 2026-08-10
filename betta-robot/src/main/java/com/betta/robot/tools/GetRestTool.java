package com.betta.robot.tools;

import com.betta.robot.dto.ActionResult;
import com.betta.system.domain.CardAccount;
import com.betta.system.service.ICardAccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 查询并汇总全部卡片账户余额的机器人工具。
 */
@Slf4j
@Component
public class GetRestTool implements ITool {

    @Autowired
    private ICardAccountService cardAccountService;

    /**
     * 查询所有账户余额并生成面向用户的汇总文本。
     *
     * @param params 统一工具参数；当前查询余额业务无需使用参数
     * @return 余额查询结果
     */
    @Override
    public ActionResult execute(Map<String, Object> params) {
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

}
