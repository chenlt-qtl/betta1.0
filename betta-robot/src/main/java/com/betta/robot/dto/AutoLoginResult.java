package com.betta.robot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 自动登录兑换结果。
 *
 * <p>token 用于建立前端登录态；targetPath 用于告诉通用 /auto-login 页面兑换成功后跳到哪个业务页面。</p>
 *
 * @author betta
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoLoginResult {

    /** 系统正常登录 token，与 /login 接口返回的 token 用法一致。 */
    private String token;

    /** 自动登录成功后跳转的站内路径，例如 /dance。 */
    private String targetPath;
}
