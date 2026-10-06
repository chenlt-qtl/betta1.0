package com.betta.robot.tools;

import com.betta.robot.config.RobotLinkProperties;
import com.betta.robot.dto.ActionResult;
import com.betta.robot.service.RobotAutoLoginService;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * HomeLoginLinkTool 独立回归测试入口，不依赖测试框架。
 */
public class HomeLoginLinkToolTest {

    public static void main(String[] args) throws Exception {
        testConfiguredFrontBaseUrlTakesPriority();
        testToolParamFrontBaseUrlFallback();
        testMissingFrontBaseUrl();
        System.out.println("HomeLoginLinkToolTest passed");
    }

    private static void testConfiguredFrontBaseUrlTakesPriority() throws Exception {
        RecordingAutoLoginService autoLoginService = new RecordingAutoLoginService();
        HomeLoginLinkTool tool = createTool(autoLoginService, " https://public.example.com/ ");
        Map<String, Object> params = new HashMap<>();
        params.put("frontBaseUrl", " https://betta.example.com/ ");
        params.put("nickname", "新同学 小满");

        ActionResult result = tool.execute(params);

        assertTrue(result.isSuccess(), "首页链接应生成成功");
        assertEquals("首页链接：https://public.example.com/auto-login?ticket=test-ticket",
                result.getMessage(), "环境配置的公网地址应优先于工具参数");
        assertEquals("新同学 小满", autoLoginService.nickname, "应支持未预置且包含空格的新昵称");
        assertEquals("/index", autoLoginService.targetPath, "未配置目标路径时应跳转首页");
        assertEquals(Integer.valueOf(5), autoLoginService.expireMinutes, "未配置有效期时应使用 5 分钟");
    }

    private static void testToolParamFrontBaseUrlFallback() throws Exception {
        RecordingAutoLoginService autoLoginService = new RecordingAutoLoginService();
        HomeLoginLinkTool tool = createTool(autoLoginService, " ");

        ActionResult result = tool.execute(Map.of(
                "frontBaseUrl", " https://betta.example.com/ ",
                "nickname", "回退用户"));

        assertTrue(result.isSuccess(), "环境地址为空时应使用工具参数");
        assertEquals("首页链接：https://betta.example.com/auto-login?ticket=test-ticket",
                result.getMessage(), "工具参数地址回退应正确");
    }

    private static void testMissingFrontBaseUrl() throws Exception {
        RecordingAutoLoginService autoLoginService = new RecordingAutoLoginService();
        HomeLoginLinkTool tool = createTool(autoLoginService, null);

        ActionResult result = tool.execute(Map.of("nickname", "任意用户"));

        assertTrue(!result.isSuccess(), "缺失前端地址时应拒绝生成链接");
        assertTrue(result.getMessage().contains("frontBaseUrl"), "错误信息应指出缺失的配置");
        assertTrue(!autoLoginService.called, "配置校验失败时不应签发 ticket");
    }

    private static HomeLoginLinkTool createTool(RobotAutoLoginService autoLoginService,
                                                 String configuredFrontBaseUrl) throws Exception {
        HomeLoginLinkTool tool = new HomeLoginLinkTool();
        setField(tool, "robotAutoLoginService", autoLoginService);
        RobotLinkProperties properties = new RobotLinkProperties();
        properties.setFrontBaseUrl(configuredFrontBaseUrl);
        setField(tool, "robotLinkProperties", properties);
        return tool;
    }

    private static void setField(HomeLoginLinkTool tool, String fieldName, Object value) throws Exception {
        Field field = HomeLoginLinkTool.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(tool, value);
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

    private static class RecordingAutoLoginService extends RobotAutoLoginService {
        private boolean called;
        private String nickname;
        private String targetPath;
        private Integer expireMinutes;

        @Override
        public String createTicketByNickname(String nickname, String targetPath, Integer expireMinutes) {
            this.called = true;
            this.nickname = nickname;
            this.targetPath = targetPath;
            this.expireMinutes = expireMinutes;
            return "test-ticket";
        }
    }
}
