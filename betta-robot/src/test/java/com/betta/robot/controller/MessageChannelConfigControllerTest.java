package com.betta.robot.controller;

import com.betta.common.core.domain.AjaxResult;
import com.betta.common.core.domain.entity.SysUser;
import com.betta.common.core.domain.model.LoginUser;
import com.betta.robot.dto.RobotLinkConfigRequest;
import com.betta.robot.service.RobotLinkConfigService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

/** MessageChannelConfigController 前端访问地址接口独立回归测试入口。 */
public class MessageChannelConfigControllerTest {

    public static void main(String[] args) throws Exception {
        try {
            testGetFrontBaseUrlReturnsData();
            testSaveFrontBaseUrlReturnsDataAndPassesOperator();
            System.out.println("MessageChannelConfigControllerTest passed");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static void testGetFrontBaseUrlReturnsData() throws Exception {
        StubRobotLinkConfigService service = new StubRobotLinkConfigService();
        service.savedFrontBaseUrl = "https://bettaspace.asia";
        MessageChannelConfigController controller = createController(service);

        AjaxResult result = controller.getFrontBaseUrl();

        assertEquals("操作成功", result.get(AjaxResult.MSG_TAG), "查询成功消息应保持标准值");
        assertEquals("https://bettaspace.asia", result.get(AjaxResult.DATA_TAG), "查询结果应放入 data");
    }

    private static void testSaveFrontBaseUrlReturnsDataAndPassesOperator() throws Exception {
        StubRobotLinkConfigService service = new StubRobotLinkConfigService();
        service.savedResult = "https://bettaspace.asia";
        MessageChannelConfigController controller = createController(service);
        loginAs("operator");
        RobotLinkConfigRequest request = new RobotLinkConfigRequest();
        request.setFrontBaseUrl("https://bettaspace.asia/");

        AjaxResult result = controller.saveFrontBaseUrl(request);

        assertEquals("操作成功", result.get(AjaxResult.MSG_TAG), "保存成功消息应保持标准值");
        assertEquals("https://bettaspace.asia", result.get(AjaxResult.DATA_TAG), "保存结果应放入 data");
        assertEquals(request, service.lastRequest, "保存请求应原样传给服务");
        assertEquals("operator", service.lastOperator, "当前登录用户名应作为操作人传给服务");
    }

    private static MessageChannelConfigController createController(StubRobotLinkConfigService service)
            throws Exception {
        MessageChannelConfigController controller = new MessageChannelConfigController();
        Field field = MessageChannelConfigController.class.getDeclaredField("robotLinkConfigService");
        field.setAccessible(true);
        field.set(controller, service);
        return controller;
    }

    private static void loginAs(String username) {
        SysUser user = new SysUser();
        user.setUserName(username);
        LoginUser loginUser = new LoginUser();
        loginUser.setUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + "，expected=" + expected + "，actual=" + actual);
        }
    }

    private static class StubRobotLinkConfigService extends RobotLinkConfigService {
        private String savedFrontBaseUrl;
        private String savedResult;
        private RobotLinkConfigRequest lastRequest;
        private String lastOperator;

        @Override
        public String getSavedFrontBaseUrl() {
            return savedFrontBaseUrl;
        }

        @Override
        public synchronized String saveFrontBaseUrl(RobotLinkConfigRequest request, String operator) {
            lastRequest = request;
            lastOperator = operator;
            return savedResult;
        }
    }
}
