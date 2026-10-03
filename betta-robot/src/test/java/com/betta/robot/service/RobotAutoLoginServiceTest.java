package com.betta.robot.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.betta.common.core.domain.entity.SysUser;
import com.betta.common.core.redis.RedisCache;
import com.betta.common.enums.UserStatus;
import com.betta.common.exception.ServiceException;
import com.betta.system.service.ISysUserService;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * RobotAutoLoginService 独立回归测试入口，不依赖测试框架。
 */
public class RobotAutoLoginServiceTest {

    public static void main(String[] args) throws Exception {
        testCreateTicketByNickname();
        testNicknameValidation();
        testUsernameTicketCompatibility();
        System.out.println("RobotAutoLoginServiceTest passed");
    }

    private static void testCreateTicketByNickname() throws Exception {
        MemoryRedisCache redisCache = new MemoryRedisCache();
        Map<String, List<SysUser>> usersByNickname = new HashMap<>();
        usersByNickname.put("豆芽", Collections.singletonList(user("douya", "豆芽", UserStatus.OK.getCode())));
        RobotAutoLoginService service = createService(redisCache, usersByNickname);

        String ticket = service.createTicketByNickname("豆芽", "/index", 5);
        JSONObject payload = redisCache.payload(ticket);
        assertEquals("douya", payload.getString("username"), "昵称应解析为对应登录账号");
        assertEquals("/index", payload.getString("targetPath"), "首页目标路径应写入 ticket");
    }

    private static void testNicknameValidation() throws Exception {
        Map<String, List<SysUser>> usersByNickname = new HashMap<>();
        usersByNickname.put("重复", List.of(
                user("repeat1", "重复", UserStatus.OK.getCode()),
                user("repeat2", "重复", UserStatus.OK.getCode())));
        usersByNickname.put("停用", Collections.singletonList(
                user("disabled", "停用", UserStatus.DISABLE.getCode())));
        RobotAutoLoginService service = createService(new MemoryRedisCache(), usersByNickname);

        assertServiceException(() -> service.createTicketByNickname(" ", "/index", 5), "昵称不能为空");
        assertServiceException(() -> service.createTicketByNickname("不存在", "/index", 5), "用户不存在");
        assertServiceException(() -> service.createTicketByNickname("重复", "/index", 5), "昵称不唯一");
        assertServiceException(() -> service.createTicketByNickname("停用", "/index", 5), "用户已停用");
    }

    private static void testUsernameTicketCompatibility() throws Exception {
        MemoryRedisCache redisCache = new MemoryRedisCache();
        RobotAutoLoginService service = createService(redisCache, Collections.emptyMap());

        String ticket = service.createTicket("damu", "/dance", 5);
        JSONObject payload = redisCache.payload(ticket);
        assertEquals("damu", payload.getString("username"), "原按账号签发能力应保持兼容");
        assertEquals("/dance", payload.getString("targetPath"), "原目标路径应保持兼容");
    }

    private static RobotAutoLoginService createService(MemoryRedisCache redisCache,
                                                        Map<String, List<SysUser>> usersByNickname)
            throws Exception {
        RobotAutoLoginService service = new RobotAutoLoginService();
        ISysUserService userService = (ISysUserService) Proxy.newProxyInstance(
                ISysUserService.class.getClassLoader(),
                new Class<?>[]{ISysUserService.class},
                (proxy, method, args) -> {
                    if ("selectUsersByNickName".equals(method.getName())) {
                        return new ArrayList<>(usersByNickname.getOrDefault(args[0], Collections.emptyList()));
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        setField(service, "redisCache", redisCache);
        setField(service, "userService", userService);
        return service;
    }

    private static SysUser user(String username, String nickname, String status) {
        SysUser user = new SysUser();
        user.setUserName(username);
        user.setNickName(nickname);
        user.setStatus(status);
        user.setDelFlag(UserStatus.OK.getCode());
        return user;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void assertServiceException(ThrowingRunnable action, String expectedMessagePart) {
        try {
            action.run();
            throw new AssertionError("预期抛出 ServiceException：" + expectedMessagePart);
        } catch (ServiceException e) {
            if (e.getMessage() == null || !e.getMessage().contains(expectedMessagePart)) {
                throw new AssertionError("异常信息不符合预期：" + e.getMessage());
            }
        } catch (Exception e) {
            throw new AssertionError("异常类型不符合预期", e);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + "，expected=" + expected + "，actual=" + actual);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static class MemoryRedisCache extends RedisCache {
        private final Map<String, Object> values = new HashMap<>();

        @Override
        public <T> void setCacheObject(String key, T value, Integer timeout, TimeUnit timeUnit) {
            values.put(key, value);
        }

        private JSONObject payload(String ticket) {
            Object value = values.get("robot:auto-login:" + ticket);
            if (value == null) {
                throw new AssertionError("未保存 ticket 载荷");
            }
            return JSON.parseObject(String.valueOf(value));
        }
    }
}
