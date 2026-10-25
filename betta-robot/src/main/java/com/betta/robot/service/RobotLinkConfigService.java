package com.betta.robot.service;

import com.betta.common.exception.ServiceException;
import com.betta.common.utils.StringUtils;
import com.betta.robot.config.RobotLinkProperties;
import com.betta.robot.dto.RobotLinkConfigRequest;
import com.betta.system.domain.SysConfig;
import com.betta.system.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * 机器人回复链接的前端访问地址配置服务。
 */
@Service
public class RobotLinkConfigService {

    /** 页面配置在 sys_config 中使用的固定键名。 */
    public static final String CONFIG_KEY = "robot.link.frontBaseUrl";

    private static final String CONFIG_NAME = "机器人前端访问地址";

    @Autowired
    private ISysConfigService sysConfigService;

    @Autowired
    private RobotLinkProperties robotLinkProperties;

    /**
     * 查询页面已保存的前端访问地址，未保存时返回空字符串。
     *
     * @return 页面保存值
     */
    public String getSavedFrontBaseUrl() {
        return StringUtils.defaultString(sysConfigService.selectConfigByKey(CONFIG_KEY));
    }

    /**
     * 保存前端访问地址；页面输入为空时使用当前浏览器 origin。
     *
     * @param request 保存请求
     * @param operator 操作人
     * @return 规范化后的保存值
     */
    public synchronized String saveFrontBaseUrl(RobotLinkConfigRequest request, String operator) {
        if (request == null) {
            throw new ServiceException("前端访问地址不能为空");
        }
        String selectedUrl = StringUtils.isBlank(request.getFrontBaseUrl())
                ? request.getCurrentOrigin() : request.getFrontBaseUrl();
        String normalizedUrl = normalizeOrigin(selectedUrl);

        SysConfig existingConfig = findConfig();
        int rows;
        if (existingConfig == null) {
            SysConfig config = new SysConfig();
            config.setConfigName(CONFIG_NAME);
            config.setConfigKey(CONFIG_KEY);
            config.setConfigValue(normalizedUrl);
            config.setConfigType("Y");
            config.setCreateBy(operator);
            config.setRemark("飞书等机器人回复中使用的前端公网地址");
            rows = sysConfigService.insertConfig(config);
        } else {
            existingConfig.setConfigValue(normalizedUrl);
            existingConfig.setUpdateBy(operator);
            rows = sysConfigService.updateConfig(existingConfig);
        }
        if (rows <= 0) {
            throw new ServiceException("前端访问地址保存失败");
        }
        return normalizedUrl;
    }

    /**
     * 按页面配置、环境配置、旧工具参数的顺序解析链接基础地址。
     *
     * @param toolFrontBaseUrl 旧工具参数中的前端地址
     * @return 可用的前端基础地址，均未配置时返回空字符串
     */
    public String resolveFrontBaseUrl(String toolFrontBaseUrl) {
        String savedFrontBaseUrl = getSavedFrontBaseUrl();
        if (StringUtils.isNotBlank(savedFrontBaseUrl)) {
            return savedFrontBaseUrl.trim();
        }
        if (StringUtils.isNotBlank(robotLinkProperties.getFrontBaseUrl())) {
            return trimTrailingSlash(robotLinkProperties.getFrontBaseUrl());
        }
        return StringUtils.isBlank(toolFrontBaseUrl) ? StringUtils.EMPTY : trimTrailingSlash(toolFrontBaseUrl);
    }

    private SysConfig findConfig() {
        SysConfig query = new SysConfig();
        query.setConfigKey(CONFIG_KEY);
        List<SysConfig> configs = sysConfigService.selectConfigList(query);
        if (configs == null) {
            return null;
        }
        return configs.stream()
                .filter(config -> CONFIG_KEY.equals(config.getConfigKey()))
                .findFirst()
                .orElse(null);
    }

    private String normalizeOrigin(String value) {
        if (StringUtils.isBlank(value)) {
            throw new ServiceException("前端访问地址不能为空");
        }
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            boolean httpScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            String path = uri.getRawPath();
            boolean originPath = StringUtils.isEmpty(path) || path.matches("/+");
            if (!httpScheme || StringUtils.isBlank(uri.getHost()) || uri.getRawUserInfo() != null
                    || uri.getPort() > 65535 || !originPath
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                throw invalidAddress();
            }
            return new URI(scheme.toLowerCase(), null, uri.getHost().toLowerCase(), uri.getPort(),
                    null, null, null).toASCIIString();
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw invalidAddress();
        }
    }

    private String trimTrailingSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private ServiceException invalidAddress() {
        return new ServiceException("前端访问地址必须是无路径、查询参数、片段和账号信息的 HTTP(S) 地址");
    }
}
