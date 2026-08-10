package com.betta.robot.tools;

import com.betta.common.utils.StringUtils;
import com.betta.robot.dto.ActionResult;
import com.betta.system.domain.CardAccount;
import com.betta.system.domain.CardHistory;
import com.betta.system.service.ICardAccountService;
import com.betta.system.service.ICardHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 处理机器人加卡、扣卡请求的业务工具。
 */
@Slf4j
@Component
public class AddCardTool implements ITool {

    @Autowired
    private ICardAccountService cardAccountService;

    @Autowired
    private ICardHistoryService cardHistoryService;

    /**
     * 根据统一参数执行账户卡片数量变更，并写入变更历史。
     *
     * @param params 工具参数，支持 account、action、quantity、conent，其中 conent 沿用历史拼写
     * @return 加卡或扣卡结果
     */
    @Override
    public ActionResult execute(Map<String, Object> params) {
        String accountName = getStringParam(params, "account");
        String action = getStringParam(params, "action");
        Integer quantity = getIntegerParam(params, "quantity");
        String content = getStringParam(params, "conent");
        log.info("接收到加卡请求：account={}, action={}, quantity={}, content={}",
                accountName, action, quantity, content);

        // 保持原有参数校验顺序和失败提示，避免改变机器人对话契约。
        if (StringUtils.isEmpty(accountName)) {
            return ActionResult.fail("请说明要加卡的人员名称");
        }
        if (quantity == null) {
            return ActionResult.fail("请指定加卡数量");
        }

        // 扣卡、消费或减卡动作统一转换为负数，其他动作保持原数量符号。
        if (action != null && (action.contains("扣") || action.contains("消费") || action.contains("减"))) {
            quantity = -Math.abs(quantity);
        }

        try {
            // 账户必须已存在，保持原业务不自动创建账户。
            CardAccount account = cardAccountService.selectCardAccountByName(accountName);
            if (account == null) {
                return ActionResult.fail("帐户" + accountName + "不存在");
            }

            // 先更新账户余额，再按相同变更值记录历史明细。
            int newBalance = account.getBalance() + quantity;
            account.setBalance(newBalance);
            account.setUpdateBy("robot");
            cardAccountService.updateCardAccount(account);

            CardHistory history = new CardHistory();
            history.setAccountId(account.getId());
            history.setChangeValue(quantity);
            history.setRemainValue(newBalance);
            history.setContent(StringUtils.isEmpty(content) ? "" : content);
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
     * 从参数 Map 中读取字符串值。
     *
     * @param params 工具参数，允许为空
     * @param key 参数名
     * @return 参数不存在时返回 null，否则返回字符串形式的参数值
     */
    private String getStringParam(Map<String, Object> params, String key) {
        if (params == null || params.get(key) == null) {
            return null;
        }
        return String.valueOf(params.get(key));
    }

    /**
     * 将 Number 或数字字符串安全转换为整数，拒绝小数和超出 Integer 范围的值。
     *
     * @param params 工具参数，允许为空
     * @param key 参数名
     * @return 可精确转换时返回整数，否则返回 null 以复用原有数量缺失提示
     */
    private Integer getIntegerParam(Map<String, Object> params, String key) {
        if (params == null || params.get(key) == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(params.get(key)).trim()).intValueExact();
        } catch (RuntimeException e) {
            log.warn("工具参数 {} 不是有效整数：{}", key, params.get(key));
            return null;
        }
    }
}
