package com.betta.robot.service;

import com.betta.common.exception.ServiceException;
import com.betta.robot.config.RobotLinkProperties;
import com.betta.robot.dto.RobotLinkConfigRequest;
import com.betta.system.domain.SysConfig;
import com.betta.system.service.ISysConfigService;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** RobotLinkConfigService 独立回归测试入口。 */
public class RobotLinkConfigServiceTest {

    public static void main(String[] args) throws Exception {
        testCreateAndNormalizeConfiguredUrl();
        testUpdateWithCurrentOrigin();
        testResolvePriority();
        testInvalidAddressesDoNotOverwriteConfig();
        System.out.println("RobotLinkConfigServiceTest passed");
    }

    private static void testCreateAndNormalizeConfiguredUrl() throws Exception {
        ConfigServiceHandler handler = new ConfigServiceHandler();
        RobotLinkConfigService service = createService(handler, "https://environment.example.com");

        String saved = service.saveFrontBaseUrl(request(" https://BETTA.ICU/// ", "http://1.2.3.4"), "admin");

        assertEquals("https://betta.icu", saved, "应规范化协议、主机和末尾斜杠");
        assertEquals(1, handler.configs.size(), "首次保存应自动创建配置");
        SysConfig config = handler.configs.get(0);
        assertEquals(RobotLinkConfigService.CONFIG_KEY, config.getConfigKey(), "配置键应固定");
        assertEquals("https://betta.icu", config.getConfigValue(), "应保存规范化地址");
        assertEquals("admin", config.getCreateBy(), "应记录创建人");
        assertEquals("Y", config.getConfigType(), "自动创建的配置应为内置参数");
    }

    private static void testUpdateWithCurrentOrigin() throws Exception {
        ConfigServiceHandler handler = new ConfigServiceHandler();
        handler.configs.add(config(7L, "https://old.example.com"));
        RobotLinkConfigService service = createService(handler, null);

        String saved = service.saveFrontBaseUrl(request(" ", " http://123.45.67.89:8080/ "), "operator");

        assertEquals("http://123.45.67.89:8080", saved, "页面留空时应保存当前浏览器 origin");
        assertEquals(1, handler.configs.size(), "更新时不应创建重复配置");
        assertEquals("http://123.45.67.89:8080", handler.configs.get(0).getConfigValue(), "应更新既有配置");
        assertEquals("operator", handler.configs.get(0).getUpdateBy(), "应记录更新人");
    }

    private static void testResolvePriority() throws Exception {
        ConfigServiceHandler handler = new ConfigServiceHandler();
        RobotLinkConfigService service = createService(handler, " https://environment.example.com/ ");

        assertEquals("https://environment.example.com", service.resolveFrontBaseUrl("https://tool.example.com/"),
                "页面配置缺失时环境配置应优先于旧工具参数");

        RobotLinkConfigService toolFallbackService = createService(handler, " ");
        assertEquals("https://tool.example.com", toolFallbackService.resolveFrontBaseUrl(" https://tool.example.com/// "),
                "页面和环境配置缺失时应使用旧工具参数");

        handler.configs.add(config(8L, "https://page.example.com"));
        assertEquals("https://page.example.com", service.resolveFrontBaseUrl("https://tool.example.com"),
                "页面保存配置应具有最高优先级");
    }

    private static void testInvalidAddressesDoNotOverwriteConfig() throws Exception {
        List<String> invalidValues = List.of(
                "ftp://betta.example.com",
                "https://user:password@betta.example.com",
                "https://betta.example.com/business",
                "https://betta.example.com?from=robot",
                "https://betta.example.com#fragment",
                "https://betta.example.com:65536",
                "betta.example.com",
                "http://");
        for (String invalidValue : invalidValues) {
            ConfigServiceHandler handler = new ConfigServiceHandler();
            handler.configs.add(config(9L, "https://original.example.com"));
            RobotLinkConfigService service = createService(handler, null);

            assertThrows(() -> service.saveFrontBaseUrl(request(invalidValue, "https://current.example.com"), "admin"),
                    "非法地址应保存失败：" + invalidValue);
            assertEquals("https://original.example.com", handler.configs.get(0).getConfigValue(),
                    "非法地址不得覆盖已有配置");
        }

        ConfigServiceHandler handler = new ConfigServiceHandler();
        RobotLinkConfigService service = createService(handler, null);
        assertThrows(() -> service.saveFrontBaseUrl(request(" ", " "), "admin"),
                "页面输入和当前 origin 均为空时应保存失败");
        assertEquals(0, handler.configs.size(), "校验失败时不得创建配置");
    }

    private static RobotLinkConfigService createService(ConfigServiceHandler handler,
                                                        String environmentUrl) throws Exception {
        RobotLinkConfigService service = new RobotLinkConfigService();
        ISysConfigService configService = (ISysConfigService) Proxy.newProxyInstance(
                ISysConfigService.class.getClassLoader(),
                new Class<?>[]{ISysConfigService.class}, handler);
        setField(service, "sysConfigService", configService);
        RobotLinkProperties properties = new RobotLinkProperties();
        properties.setFrontBaseUrl(environmentUrl);
        setField(service, "robotLinkProperties", properties);
        return service;
    }

    private static RobotLinkConfigRequest request(String frontBaseUrl, String currentOrigin) {
        RobotLinkConfigRequest request = new RobotLinkConfigRequest();
        request.setFrontBaseUrl(frontBaseUrl);
        request.setCurrentOrigin(currentOrigin);
        return request;
    }

    private static SysConfig config(Long id, String value) {
        SysConfig config = new SysConfig();
        config.setConfigId(id);
        config.setConfigName("机器人前端访问地址");
        config.setConfigKey(RobotLinkConfigService.CONFIG_KEY);
        config.setConfigValue(value);
        config.setConfigType("Y");
        return config;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void assertThrows(ThrowingRunnable action, String message) {
        try {
            action.run();
        } catch (ServiceException e) {
            return;
        } catch (Exception e) {
            throw new AssertionError(message + "，抛出了意外异常", e);
        }
        throw new AssertionError(message + "，expected=ServiceException");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + "，expected=" + expected + "，actual=" + actual);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static class ConfigServiceHandler implements InvocationHandler {
        private final List<SysConfig> configs = new ArrayList<>();
        private long nextId = 1L;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "selectConfigByKey" -> configs.stream()
                        .filter(config -> Objects.equals(config.getConfigKey(), args[0]))
                        .map(SysConfig::getConfigValue)
                        .findFirst()
                        .orElse("");
                case "selectConfigList" -> new ArrayList<>(configs);
                case "insertConfig" -> insert((SysConfig) args[0]);
                case "updateConfig" -> update((SysConfig) args[0]);
                default -> defaultValue(method.getReturnType());
            };
        }

        private int insert(SysConfig config) {
            config.setConfigId(nextId++);
            configs.add(config);
            return 1;
        }

        private int update(SysConfig updated) {
            for (int i = 0; i < configs.size(); i++) {
                if (Objects.equals(configs.get(i).getConfigId(), updated.getConfigId())) {
                    configs.set(i, updated);
                    return 1;
                }
            }
            return 0;
        }

        private Object defaultValue(Class<?> returnType) {
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == int.class) {
                return 0;
            }
            return null;
        }
    }
}
