package com.betta.robot.tools;

import com.alibaba.fastjson2.JSON;
import com.betta.common.utils.StringUtils;
import com.betta.robot.dto.ActionResult;
import com.betta.robot.dto.AddCardDTO;
import com.betta.robot.dto.CommandDTO;
import com.betta.system.domain.CardAccount;
import com.betta.system.domain.CardHistory;
import com.betta.system.service.ICardAccountService;
import com.betta.system.service.ICardHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询余额
 */
@Slf4j
@Component
public class GetRestTool implements ITool {

    @Autowired
    private ICardAccountService cardAccountService;

    @Override
    public ActionResult execute(CommandDTO commandDTO) {
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
