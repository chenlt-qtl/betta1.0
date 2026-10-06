package com.betta.robot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 飞书等机器人回复中使用的前端公网地址配置。 */
@Data
@Component
@ConfigurationProperties(prefix = "robot.link")
public class RobotLinkProperties {

    /** 对外可访问的前端基础地址，配置后优先于数据库工具参数。 */
    private String frontBaseUrl;
}
