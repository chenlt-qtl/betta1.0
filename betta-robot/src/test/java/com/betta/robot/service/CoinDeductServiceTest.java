package com.betta.robot.service;

import com.betta.common.core.domain.entity.SysUser;
import com.betta.common.exception.ServiceException;
import com.betta.eng.domain.mall.MallExchangeRecord;
import com.betta.eng.mapper.EngCoinWalletMapper;
import com.betta.eng.mapper.MallExchangeRecordMapper;
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
        testRecordSaveFailure();
        System.out.println("CoinDeductServiceTest passed");
    }

    private static void testDeductSuccess() {
        TestWalletMapper walletMapper = new TestWalletMapper(300);
        TestExchangeRecordMapper recordMapper = new TestExchangeRecordMapper();
        CoinDeductService service = createService(List.of(user(1L, "0")), walletMapper, recordMapper);

        long balance = service.deductCoin("豆芽", "买文具", 100);

        assertEquals(200L, balance, "应返回扣减后的余额");
        assertEquals(100L, walletMapper.lastCoinCost, "应按请求数量扣减金币");
        assertEquals("robot", walletMapper.lastUsername, "机器人扣减应记录固定操作人");
        MallExchangeRecord record = recordMapper.lastRecord;
        assertEquals(1L, record.getUserId(), "消费记录应关联扣款用户");
        assertEquals(0L, record.getProductId(), "普通消费应使用固定商品标识");
        assertEquals("买文具", record.getProductName(), "消费记录应保存消费内容");
        assertEquals("", record.getProductImages(), "普通消费应保存空图片");
        assertEquals(100L, record.getCoinPrice(), "消费记录应保存扣减金币数量");
        assertEquals("robot", record.getCreateBy(), "消费记录应保存固定操作人");
    }

    private static void testInsufficientBalance() {
        TestWalletMapper walletMapper = new TestWalletMapper(99);
        TestExchangeRecordMapper recordMapper = new TestExchangeRecordMapper();
        CoinDeductService service = createService(List.of(user(1L, "0")), walletMapper, recordMapper);

        assertServiceError("金币余额不足", () -> service.deductCoin("豆芽", "买文具", 100));
        assertEquals(99L, walletMapper.balance, "余额不足时不应改变余额");
        assertEquals(0, recordMapper.insertCalls, "余额不足时不应写消费记录");
    }

    private static void testUserNotFound() {
        TestWalletMapper walletMapper = new TestWalletMapper(300);
        TestExchangeRecordMapper recordMapper = new TestExchangeRecordMapper();
        CoinDeductService service = createService(List.of(), walletMapper, recordMapper);

        assertServiceError("用户不存在", () -> service.deductCoin("豆芽", "买文具", 100));
        assertEquals(0, walletMapper.decreaseCalls, "用户不存在时不应访问扣减方法");
        assertEquals(0, recordMapper.insertCalls, "用户不存在时不应写消费记录");
    }

    private static void testDuplicateNickname() {
        TestWalletMapper walletMapper = new TestWalletMapper(300);
        TestExchangeRecordMapper recordMapper = new TestExchangeRecordMapper();
        CoinDeductService service = createService(
                List.of(user(1L, "0"), user(2L, "0")), walletMapper, recordMapper);

        assertServiceError("用户昵称不唯一", () -> service.deductCoin("豆芽", "买文具", 100));
        assertEquals(0, walletMapper.decreaseCalls, "昵称重复时不应扣减");
        assertEquals(0, recordMapper.insertCalls, "昵称重复时不应写消费记录");
    }

    private static void testDisabledUser() {
        TestWalletMapper walletMapper = new TestWalletMapper(300);
        TestExchangeRecordMapper recordMapper = new TestExchangeRecordMapper();
        CoinDeductService service = createService(List.of(user(1L, "1")), walletMapper, recordMapper);

        assertServiceError("用户已停用", () -> service.deductCoin("豆芽", "买文具", 100));
        assertEquals(0, walletMapper.decreaseCalls, "停用用户不应扣减");
        assertEquals(0, recordMapper.insertCalls, "停用用户不应写消费记录");
    }

    private static void testInvalidCoinCost() {
        TestWalletMapper walletMapper = new TestWalletMapper(300);
        TestExchangeRecordMapper recordMapper = new TestExchangeRecordMapper();
        CoinDeductService service = createService(List.of(user(1L, "0")), walletMapper, recordMapper);

        assertServiceError("金币数量必须为正整数", () -> service.deductCoin("豆芽", "买文具", 0));
        assertServiceError("金币数量必须为正整数", () -> service.deductCoin("豆芽", "买文具", -1));
        assertEquals(0, walletMapper.decreaseCalls, "非法数量不应扣减");
        assertEquals(0, recordMapper.insertCalls, "非法数量不应写消费记录");
    }

    private static void testRecordSaveFailure() {
        TestWalletMapper walletMapper = new TestWalletMapper(300);
        TestExchangeRecordMapper recordMapper = new TestExchangeRecordMapper();
        recordMapper.insertResult = 0;
        CoinDeductService service = createService(List.of(user(1L, "0")), walletMapper, recordMapper);

        assertServiceError("金币消费记录保存失败",
                () -> service.deductCoin("豆芽", "买文具", 100));
        assertEquals(1, walletMapper.decreaseCalls, "记录保存前应已执行金币扣减");
        assertEquals(1, recordMapper.insertCalls, "应尝试保存消费记录");
    }

    private static CoinDeductService createService(List<SysUser> users, TestWalletMapper walletMapper,
                                                   TestExchangeRecordMapper recordMapper) {
        SysUserMapper userMapper = (SysUserMapper) Proxy.newProxyInstance(
                SysUserMapper.class.getClassLoader(),
                new Class<?>[]{SysUserMapper.class},
                (proxy, method, args) -> {
                    if ("selectUsersByNickName".equals(method.getName())) {
                        return users;
                    }
                    return defaultValue(method.getReturnType());
                });
        return new CoinDeductService(userMapper, walletMapper, recordMapper);
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

    /** 记录最近一次写入内容的兑换记录 Mapper。 */
    private static final class TestExchangeRecordMapper implements MallExchangeRecordMapper {
        private int insertResult = 1;
        private int insertCalls;
        private MallExchangeRecord lastRecord;

        @Override
        public int insertExchangeRecord(MallExchangeRecord record) {
            insertCalls++;
            lastRecord = record;
            return insertResult;
        }

        @Override
        public List<MallExchangeRecord> selectExchangeRecordList(MallExchangeRecord record) {
            return List.of();
        }
    }
}
