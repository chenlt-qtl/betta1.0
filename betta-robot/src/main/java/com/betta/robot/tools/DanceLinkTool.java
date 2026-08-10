package com.betta.robot.tools;

import com.betta.common.utils.StringUtils;
import com.betta.robot.dto.ActionResult;
import com.betta.robot.service.RobotAutoLoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 生成跳舞自动登录链接。
 *
 * <p>这是自动登录能力的一个具体业务工具：
 * 用户问“跳舞”时，生成一个可点击链接。链接进入通用前端中转页 /auto-login，
 * 中转页兑换 ticket 后会根据 ticket 中的 targetPath 跳到 /dance。</p>
 *
 * @author betta
 */
@Slf4j
@Component
public class DanceLinkTool implements ITool {

    private static final String DEFAULT_TARGET_PATH = "/dance";

    private static final String DEFAULT_USERNAME = "damu";

    private static final int DEFAULT_EXPIRE_MINUTES = 5;

    @Autowired
    private RobotAutoLoginService robotAutoLoginService;

    /**
     * 根据工具参数生成跳舞页面的短期自动登录链接。
     *
     * @param params 工具参数，支持 frontBaseUrl、username、targetPath、expireMinutes
     * @return 自动登录链接生成结果
     */
    @Override
    public ActionResult execute(Map<String, Object> params) {
        try {
            // tool_params 示例：
            // {"frontBaseUrl":"https://your-domain.com","username":"damu","targetPath":"/dance","expireMinutes":5}
            // 以后其它模块可以复用 RobotAutoLoginService，只需要换 targetPath 或另建工具配置。
            String frontBaseUrl = getStringParam(params, "frontBaseUrl");
            if (StringUtils.isBlank(frontBaseUrl)) {
                return ActionResult.fail("未配置前端地址 frontBaseUrl");
            }
            String username = StringUtils.defaultIfBlank(getStringParam(params, "username"), DEFAULT_USERNAME);
            String targetPath = StringUtils.defaultIfBlank(getStringParam(params, "targetPath"), DEFAULT_TARGET_PATH);
            Integer expireMinutes = getIntegerParam(params, "expireMinutes");
            if (expireMinutes == null || expireMinutes <= 0) {
                expireMinutes = DEFAULT_EXPIRE_MINUTES;
            }

            String ticket = robotAutoLoginService.createTicket(username, targetPath, expireMinutes);
            // 前端统一使用 /auto-login 作为中转入口，具体跳到哪里由后端 ticket 的 targetPath 决定。
            String link = trimTrailingSlash(frontBaseUrl) + "/auto-login?ticket=" + ticket;
            log.info("生成跳舞自动登录链接，username={}, expireMinutes={}", username, expireMinutes);
            return ActionResult.ok("跳舞链接：" + link);
        } catch (Exception e) {
            log.error("生成跳舞自动登录链接失败", e);
            return ActionResult.fail("生成跳舞链接失败：" + e.getMessage());
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
     * 将 Number 或数字字符串转换为有效期分钟数。
     *
     * @param params 工具参数，允许为空
     * @param key 参数名
     * @return 可精确转换时返回整数，非法值返回 null 并由调用方使用默认有效期
     */
    private Integer getIntegerParam(Map<String, Object> params, String key) {
        if (params == null || params.get(key) == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(params.get(key)).trim()).intValueExact();
        } catch (RuntimeException e) {
            log.warn("跳舞链接参数 {} 不是有效整数：{}，将使用默认有效期", key, params.get(key));
            return null;
        }
    }

    /**
     * 删除地址末尾多余的斜杠，避免拼接中转路径时出现双斜杠。
     *
     * @param url 已校验非空的前端基础地址
     * @return 去除末尾斜杠后的地址
     */
    private String trimTrailingSlash(String url) {
        String result = url.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
