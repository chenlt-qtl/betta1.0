package com.betta.robot.tools;

import com.betta.common.exception.ServiceException;
import com.betta.common.utils.StringUtils;
import com.betta.robot.dto.ActionResult;
import com.betta.robot.service.CoinDeductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/** 处理机器人按昵称扣减金币的业务工具。 */
@Slf4j
@Component
public class DeductCoinTool implements ITool {

    private final CoinDeductService coinDeductService;

    /** 创建扣金币工具。 */
    public DeductCoinTool(CoinDeductService coinDeductService) {
        this.coinDeductService = coinDeductService;
    }

    /** 根据昵称、消费内容和金币数量执行扣减。 */
    @Override
    public ActionResult execute(Map<String, Object> params) {
        String nickname = getStringParam(params, "nickname");
        String content = getStringParam(params, "content");
        Long coinCost = getLongParam(params, "coinCost");
        log.info("接收到扣金币请求：nickname={}, content={}, coinCost={}", nickname, content, coinCost);

        if (StringUtils.isEmpty(nickname)) {
            return ActionResult.fail("请说明消费人员名称");
        }
        if (StringUtils.isEmpty(content)) {
            return ActionResult.fail("请说明消费内容");
        }
        if (coinCost == null) {
            return ActionResult.fail("请指定有效的金币数量");
        }

        try {
            long balance = coinDeductService.deductCoin(nickname, coinCost);
            return ActionResult.ok("【" + nickname + "】" + content + "消费 " + coinCost
                    + " 金币，当前余额 " + balance + " 金币");
        } catch (ServiceException e) {
            log.warn("扣金币失败：nickname={}, coinCost={}, reason={}", nickname, coinCost, e.getMessage());
            return ActionResult.fail("扣金币失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("扣金币失败：nickname={}, coinCost={}", nickname, coinCost, e);
            return ActionResult.fail("扣金币失败：" + e.getMessage());
        }
    }

    /** 读取工具字符串参数。 */
    private String getStringParam(Map<String, Object> params, String key) {
        if (params == null || params.get(key) == null) {
            return null;
        }
        return String.valueOf(params.get(key)).trim();
    }

    /** 将 Number 或数字字符串精确转换为 long，拒绝小数和超范围值。 */
    private Long getLongParam(Map<String, Object> params, String key) {
        if (params == null || params.get(key) == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(params.get(key)).trim()).longValueExact();
        } catch (RuntimeException e) {
            log.warn("工具参数 {} 不是有效整数：{}", key, params.get(key));
            return null;
        }
    }
}
