package com.betta.robot.tools;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.betta.common.utils.StringUtils;
import com.betta.robot.dto.ActionResult;
import com.betta.robot.dto.CommandDTO;
import com.betta.robot.service.RobotAutoLoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    @Override
    public ActionResult execute(CommandDTO commandDTO) {
        try {
            // tool_params 示例：
            // {"frontBaseUrl":"https://your-domain.com","username":"damu","targetPath":"/dance","expireMinutes":5}
            // 以后其它模块可以复用 RobotAutoLoginService，只需要换 targetPath 或另建工具配置。
            JSONObject params = parseParams(commandDTO);
            String frontBaseUrl = params.getString("frontBaseUrl");
            if (StringUtils.isBlank(frontBaseUrl)) {
                return ActionResult.fail("未配置前端地址 frontBaseUrl");
            }
            String username = StringUtils.defaultIfBlank(params.getString("username"), DEFAULT_USERNAME);
            String targetPath = StringUtils.defaultIfBlank(params.getString("targetPath"), DEFAULT_TARGET_PATH);
            Integer expireMinutes = params.getInteger("expireMinutes");
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

    private JSONObject parseParams(CommandDTO commandDTO) {
        if (commandDTO == null || StringUtils.isBlank(commandDTO.getIntent())) {
            return new JSONObject();
        }
        try {
            // 未配置 dto_class 时，ApiDispatchService 会把 tool_params 原样放到 intent 字段。
            return JSON.parseObject(commandDTO.getIntent());
        } catch (Exception e) {
            log.warn("跳舞链接参数不是JSON：{}", commandDTO.getIntent());
            return new JSONObject();
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
