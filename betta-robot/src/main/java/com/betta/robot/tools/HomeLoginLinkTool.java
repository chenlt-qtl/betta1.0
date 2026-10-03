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
 * 按用户昵称生成首页自动登录链接。
 *
 * @author betta
 */
@Slf4j
@Component
public class HomeLoginLinkTool implements ITool {

    private static final String DEFAULT_TARGET_PATH = "/index";

    private static final int DEFAULT_EXPIRE_MINUTES = 5;

    @Autowired
    private RobotAutoLoginService robotAutoLoginService;

    /**
     * 根据工具参数生成指定昵称用户的短期首页登录链接。
     *
     * @param params 工具参数，支持 frontBaseUrl、nickname、targetPath、expireMinutes
     * @return 自动登录链接生成结果
     */
    @Override
    public ActionResult execute(Map<String, Object> params) {
        try {
            String frontBaseUrl = getStringParam(params, "frontBaseUrl");
            if (StringUtils.isBlank(frontBaseUrl)) {
                return ActionResult.fail("未配置前端地址 frontBaseUrl");
            }
            String nickname = getStringParam(params, "nickname");
            String targetPath = StringUtils.defaultIfBlank(
                    getStringParam(params, "targetPath"), DEFAULT_TARGET_PATH);
            Integer expireMinutes = getIntegerParam(params, "expireMinutes");
            if (expireMinutes == null || expireMinutes <= 0) {
                expireMinutes = DEFAULT_EXPIRE_MINUTES;
            }

            String ticket = robotAutoLoginService.createTicketByNickname(nickname, targetPath, expireMinutes);
            String link = trimTrailingSlash(frontBaseUrl) + "/auto-login?ticket=" + ticket;
            log.info("生成首页自动登录链接，nickname={}, expireMinutes={}", nickname, expireMinutes);
            return ActionResult.ok("首页链接：" + link);
        } catch (Exception e) {
            log.error("生成首页自动登录链接失败", e);
            return ActionResult.fail("生成首页链接失败：" + e.getMessage());
        }
    }

    private String getStringParam(Map<String, Object> params, String key) {
        if (params == null || params.get(key) == null) {
            return null;
        }
        return String.valueOf(params.get(key));
    }

    private Integer getIntegerParam(Map<String, Object> params, String key) {
        if (params == null || params.get(key) == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(params.get(key)).trim()).intValueExact();
        } catch (RuntimeException e) {
            log.warn("首页链接参数 {} 不是有效整数：{}，将使用默认有效期", key, params.get(key));
            return null;
        }
    }

    private String trimTrailingSlash(String url) {
        String result = url.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
