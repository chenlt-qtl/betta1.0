package com.betta.robot.tools;

import com.betta.robot.config.RobotLinkProperties;
import com.betta.robot.dto.ActionResult;
import com.betta.robot.service.RobotAutoLoginService;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

/** DanceLinkTool 公网地址配置独立回归测试入口。 */
public class DanceLinkToolTest {

    public static void main(String[] args) throws Exception {
        RecordingAutoLoginService autoLoginService = new RecordingAutoLoginService();
        DanceLinkTool tool = createTool(autoLoginService, " https://public.example.com/ ");

        ActionResult result = tool.execute(Map.of(
                "frontBaseUrl", "https://database.example.com",
                "username", "damu"));

        assertTrue(result.isSuccess(), "跳舞链接应生成成功");
        assertEquals("跳舞链接：https://public.example.com/auto-login?ticket=test-ticket",
                result.getMessage(), "环境配置的公网地址应优先于工具参数");
        assertEquals("damu", autoLoginService.username, "应使用工具参数中的用户名");
        assertEquals("/dance", autoLoginService.targetPath, "未配置目标路径时应跳转跳舞页面");
        assertEquals(Integer.valueOf(5), autoLoginService.expireMinutes, "未配置有效期时应使用 5 分钟");
        System.out.println("DanceLinkToolTest passed");
    }

    private static DanceLinkTool createTool(RobotAutoLoginService autoLoginService,
                                             String configuredFrontBaseUrl) throws Exception {
        DanceLinkTool tool = new DanceLinkTool();
        setField(tool, "robotAutoLoginService", autoLoginService);
        RobotLinkProperties properties = new RobotLinkProperties();
        properties.setFrontBaseUrl(configuredFrontBaseUrl);
        setField(tool, "robotLinkProperties", properties);
        return tool;
    }

    private static void setField(DanceLinkTool tool, String fieldName, Object value) throws Exception {
        Field field = DanceLinkTool.class.getDeclaredField(fieldName);
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
        private String username;
        private String targetPath;
        private Integer expireMinutes;

        @Override
        public String createTicket(String username, String targetPath, Integer expireMinutes) {
            this.username = username;
            this.targetPath = targetPath;
            this.expireMinutes = expireMinutes;
            return "test-ticket";
        }
    }
}
