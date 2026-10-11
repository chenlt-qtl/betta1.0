package com.betta.robot.service;

import com.betta.common.core.domain.entity.SysUser;
import com.betta.common.exception.ServiceException;
import com.betta.eng.mapper.EngCoinWalletMapper;
import com.betta.system.mapper.SysUserMapper;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Objects;

/** CoinDeductService 独立回归测试入口，不依赖测试框架。 */
public class CoinDeductServiceTest {

    public static void main(String[] args) {
        testDeductSuccess();
        testInsufficientBalance();
        testUserNotFound();
        testDuplicateNickname();
        testDisabledUser();
        testInvalidCoinCost();
        System.out.println("CoinDeductServiceTest passed");
    }

    private static void testDeductSuccess() {
        TestWalletMapper walletMapper = new TestWalletMapper(300);
        CoinDeductService service = createService(List.of(user(1L, "0")), walletMapper);

        long balance = service.deductCoin("豆芽", 100);

        assertEquals(200L, balance, "应返回扣减后的余额");
        assertEquals(100L, walletMapper.lastCoinCost, "应按请求数量扣减金币");
        assertEquals("robot", walletMapper.lastUsername, "机器人扣减应记录固定操作人");
    }

    private static void testInsufficientBalance() {
        TestWalletMapper walletMapper = new TestWalletMapper(99);
        CoinDeductService service = createService(List.of(user(1L, "0")), walletMapper);

        assertServiceError("金币余额不足", () -> service.deductCoin("豆芽", 100));
        assertEquals(99L, walletMapper.balance, "余额不足时不应改变余额");
    }

    private static void testUserNotFound() {
        TestWalletMapper walletMapper = new TestWalletMapper(300);
        CoinDeductService service = createService(List.of(), walletMapper);

        assertServiceError("用户不存在", () -> service.deductCoin("豆芽", 100));
        assertEquals(0, walletMapper.decreaseCalls, "用户不存在时不应访问扣减方法");
    }

    private static void testDuplicateNickname() {
        TestWalletMapper walletMapper = new TestWalletMapper(300);
        CoinDeductService service = createService(List.of(user(1L, "0"), user(2L, "0")), walletMapper);

        assertServiceError("用户昵称不唯一", () -> service.deductCoin("豆芽", 100));
        assertEquals(0, walletMapper.decreaseCalls, "昵称重复时不应扣减");
    }

    private static void testDisabledUser() {
        TestWalletMapper walletMapper = new TestWalletMapper(300);
        CoinDeductService service = createService(List.of(user(1L, "1")), walletMapper);

        assertServiceError("用户已停用", () -> service.deductCoin("豆芽", 100));
        assertEquals(0, walletMapper.decreaseCalls, "停用用户不应扣减");
    }

    private static void testInvalidCoinCost() {
        TestWalletMapper walletMapper = new TestWalletMapper(300);
        CoinDeductService service = createService(List.of(user(1L, "0")), walletMapper);

        assertServiceError("金币数量必须为正整数", () -> service.deductCoin("豆芽", 0));
        assertServiceError("金币数量必须为正整数", () -> service.deductCoin("豆芽", -1));
        assertEquals(0, walletMapper.decreaseCalls, "非法数量不应扣减");
    }

    private static CoinDeductService createService(List<SysUser> users, TestWalletMapper walletMapper) {
        SysUserMapper userMapper = (SysUserMapper) Proxy.newProxyInstance(
                SysUserMapper.class.getClassLoader(),
                new Class<?>[]{SysUserMapper.class},
                (proxy, method, args) -> {
                    if ("selectUsersByNickName".equals(method.getName())) {
                        return users;
                    }
                    return defaultValue(method.getReturnType());
                });
        return new CoinDeductService(userMapper, walletMapper);
    }

    private static SysUser user(Long userId, String status) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setStatus(status);
        return user;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }

    private static void assertServiceError(String expected, Runnable action) {
        try {
            action.run();
            throw new AssertionError("预期抛出 ServiceException：" + expected);
        } catch (ServiceException e) {
            assertEquals(expected, e.getMessage(), "业务异常提示不正确");
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + "，expected=" + expected + "，actual=" + actual);
        }
    }

    /** 模拟数据库条件扣减的钱包 Mapper。 */
    private static final class TestWalletMapper implements EngCoinWalletMapper {
        private long balance;
        private int decreaseCalls;
        private long lastCoinCost;
        private String lastUsername;

        private TestWalletMapper(long balance) {
            this.balance = balance;
        }

        @Override
        public long selectCoinBalance(Long userId) {
            return balance;
        }

        @Override
        public int increaseCoinBalance(Long userId, long coinReward, String username) {
            balance += coinReward;
            return 1;
        }

        @Override
        public int decreaseCoinBalance(Long userId, long coinCost, String username) {
            decreaseCalls++;
            lastCoinCost = coinCost;
            lastUsername = username;
            if (balance < coinCost) {
                return 0;
            }
            balance -= coinCost;
            return 1;
        }
    }
}
