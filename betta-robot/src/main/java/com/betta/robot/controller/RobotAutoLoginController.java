package com.betta.robot.controller;

import com.betta.common.constant.Constants;
import com.betta.common.core.domain.AjaxResult;
import com.betta.robot.dto.AutoLoginResult;
import com.betta.robot.service.RobotAutoLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 机器人自动登录兑换接口。
 *
 * <p>该接口允许匿名访问，但只接受短期一次性 ticket。
 * 它不会接收密码，也不会直接从前端指定 username，登录用户和目标页都来自后端签发的 ticket。</p>
 *
 * @author betta
 */
@RestController
@RequestMapping("/robot")
public class RobotAutoLoginController {

    @Autowired
    private RobotAutoLoginService robotAutoLoginService;

    @PostMapping("/auto-login")
    public AjaxResult autoLogin(@RequestBody Map<String, String> body) {
        String ticket = body == null ? null : body.get("ticket");
        AutoLoginResult result = robotAutoLoginService.exchangeToken(ticket);
        AjaxResult ajax = AjaxResult.success();

        // token 使用现有前端约定字段名，前端 setToken 后即可进入正常登录态。
        ajax.put(Constants.TOKEN, result.getToken());

        // targetPath 由后端 ticket 决定，前端只负责跳转，便于多个业务模块复用 /auto-login。
        ajax.put("targetPath", result.getTargetPath());
        return ajax;
    }
}
