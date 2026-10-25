package com.betta.robot.dto;

import lombok.Data;

/**
 * 机器人前端访问地址保存请求。
 */
@Data
public class RobotLinkConfigRequest {

    /** 页面填写的前端访问地址，留空时使用 currentOrigin。 */
    private String frontBaseUrl;

    /** 当前浏览器访问后台时的 origin。 */
    private String currentOrigin;
}
