package com.betta.robot.tools;

import com.betta.common.exception.ServiceException;
import com.betta.robot.dto.ActionResult;
import com.betta.robot.service.CoinDeductService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** DeductCoinTool 独立回归测试入口，不依赖测试框架。 */
public class DeductCoinToolTest {

    public static void main(String[] args) {
        testStringAmountAndSuccessMessage();
        testNumberAmount();
        testInvalidAmount();
        testBusinessFailure();
        System.out.println("DeductCoinToolTest passed");
    }

    private static void testStringAmountAndSuccessMessage() {
        StubCoinDeductService service = new StubCoinDeductService(900);
        DeductCoinTool tool = new DeductCoinTool(service);

        ActionResult result = tool.execute(Map.of(
                "nickname", "豆芽",
                "content", "买文具",
                "coinCost", "100"));

        assertTrue(result.isSuccess(), "合法数字字符串应扣减成功");
        assertEquals("【豆芽】买文具消费 100 金币，当前余额 900 金币",
                result.getMessage(), "成功回复应包含消费内容、数量和余额");
        assertEquals(100L, service.coinCost, "数字字符串应精确转换为 long");
    }

    private static void testNumberAmount() {
        StubCoinDeductService service = new StubCoinDeductService(50);
        DeductCoinTool tool = new DeductCoinTool(service);

        ActionResult result = tool.execute(Map.of(
                "nickname", "桐桐",
                "content", "买零食",
                "coinCost", 20));

        assertTrue(result.isSuccess(), "Number 类型参数应扣减成功");
        assertEquals(20L, service.coinCost, "Number 类型参数应正确转换");
    }

    private static void testInvalidAmount() {
        StubCoinDeductService service = new StubCoinDeductService(50);
        DeductCoinTool tool = new DeductCoinTool(service);
        Map<String, Object> params = new HashMap<>();
        params.put("nickname", "豆芽");
        params.put("content", "买文具");
        params.put("coinCost", "1.5");

        ActionResult result = tool.execute(params);

        assertTrue(!result.isSuccess(), "小数金币数量应被拒绝");
        assertEquals("请指定有效的金币数量", result.getMessage(), "非法数量提示应明确");
        assertEquals(0, service.calls, "参数转换失败时不应调用业务服务");
    }

    private static void testBusinessFailure() {
        StubCoinDeductService service = new StubCoinDeductService(0);
        service.failure = "金币余额不足";
        DeductCoinTool tool = new DeductCoinTool(service);

        ActionResult result = tool.execute(Map.of(
                "nickname", "豆芽",
                "content", "买文具",
                "coinCost", "100"));

        assertTrue(!result.isSuccess(), "业务异常应转换为失败结果");
        assertEquals("扣金币失败：金币余额不足", result.getMessage(), "应保留明确业务失败原因");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + "，expected=" + expected + "，actual=" + actual);
        }
    }

    /** 仅记录调用参数的金币服务替身。 */
    private static final class StubCoinDeductService extends CoinDeductService {
        private final long balance;
        private int calls;
        private long coinCost;
        private String failure;

        private StubCoinDeductService(long balance) {
            super(null, null);
            this.balance = balance;
        }

        @Override
        public long deductCoin(String nickname, long coinCost) {
            calls++;
            this.coinCost = coinCost;
            if (failure != null) {
                throw new ServiceException(failure);
            }
            return balance;
        }
    }
}
