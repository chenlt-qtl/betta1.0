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

@Slf4j
@Component
public class AddCardTool implements ITool {

    @Autowired
    private ICardAccountService cardAccountService;

    @Autowired
    private ICardHistoryService cardHistoryService;

    @Override
    public ActionResult execute(CommandDTO commandDTO) {
        if (commandDTO instanceof AddCardDTO) {
            AddCardDTO addCardDTO = (AddCardDTO) commandDTO;
            log.info("接收到加卡请求：account={}, quantity={}, content={}",
                    addCardDTO.getAccount(), addCardDTO.getQuantity(), addCardDTO.getConent());

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
        } else {
            return ActionResult.fail("AddCardTool收到非AddCardDTO对象");
        }
    }

    public static void main(String[] args) {
        AddCardDTO addCardDTO  = new AddCardDTO();
        addCardDTO.setAccount("account");
        addCardDTO.setQuantity(1);
        addCardDTO.setConent("content");
        System.out.println(JSON.toJSONString(addCardDTO));
    }
}
