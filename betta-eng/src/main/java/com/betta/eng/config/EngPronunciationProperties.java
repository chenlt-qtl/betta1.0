package com.betta.eng.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import com.betta.common.utils.StringUtils;
import lombok.Data;

/** 腾讯云英文跟读评测配置，所有敏感值仅从环境变量绑定。 */
@Data
@Component
@ConfigurationProperties(prefix = "eng.pronunciation")
public class EngPronunciationProperties
{
    private boolean enabled;
    private Tencent tencent = new Tencent();

    /** 只有显式启用且凭据完整时才向用户生成跟读题。 */
    public boolean isAvailable()
    {
        return enabled && StringUtils.isNotEmpty(tencent.getAppId())
                && StringUtils.isNotEmpty(tencent.getSecretId())
                && StringUtils.isNotEmpty(tencent.getSecretKey());
    }

    /** 腾讯云应用与访问凭据。 */
    @Data
    public static class Tencent
    {
        private String appId;
        private String secretId;
        private String secretKey;
    }
}
