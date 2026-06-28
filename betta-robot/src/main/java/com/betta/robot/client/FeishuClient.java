package com.betta.robot.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.betta.robot.domain.MessageChannelConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 飞书 Open API 客户端：获取 token、发送消息
 *
 * @author betta
 */
@Slf4j
@Component
public class FeishuClient {

    private static final String URL_GET_TOKEN = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
    private static final String URL_SEND_MESSAGE = "https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=chat_id";

    /** 缓存 configId -> (token, expireTimeMs) */
    private final Map<Long, TokenCache> tokenCache = new ConcurrentHashMap<>();

    /**
     * 获取 tenant_access_token（带简单内存缓存）
     *
     * @param config 通道配置
     * @return token，失败返回 null
     */
    public String getTenantAccessToken(MessageChannelConfig config) {
        if (config == null || StringUtils.isBlank(config.getAppId()) || StringUtils.isBlank(config.getAppSecret())) {
            return null;
        }
        TokenCache cached = tokenCache.get(config.getId());
        if (cached != null && cached.expireTimeMs > System.currentTimeMillis()) {
            return cached.token;
        }
        JSONObject body = new JSONObject();
        body.put("app_id", config.getAppId());
        body.put("app_secret", config.getAppSecret());
        String res = JsonHttpClient.postJson(URL_GET_TOKEN, body, null);
        if (StringUtils.isBlank(res)) {
            return null;
        }
        JSONObject obj = JSON.parseObject(res);
        if (obj.getIntValue("code") != 0) {
            log.warn("Feishu get token failed: {}", res);
            return null;
        }
        String token = obj.getString("tenant_access_token");
        int expire = obj.getIntValue("expire");
        tokenCache.put(config.getId(), new TokenCache(token, System.currentTimeMillis() + (expire - 60) * 1000L));
        return token;
    }

    /**
     * 发送文本消息到指定会话（chat_id）
     *
     * @param config   通道配置
     * @param chatId   会话 ID（群或单聊）
     * @param text     文本内容
     * @return 是否成功
     */
    public boolean sendText(MessageChannelConfig config, String chatId, String text) {
        String token = getTenantAccessToken(config);
        if (token == null || StringUtils.isBlank(chatId)) {
            return false;
        }
        JSONObject content = new JSONObject();
        content.put("text", text == null ? "" : text);
        JSONObject body = new JSONObject();
        body.put("receive_id", chatId);
        body.put("msg_type", "text");
        body.put("content", content.toJSONString());
        String res = JsonHttpClient.postJson(URL_SEND_MESSAGE, body, token);
        if (StringUtils.isBlank(res)) {
            return false;
        }
        JSONObject obj = JSON.parseObject(res);
        if (obj.getIntValue("code") != 0) {
            log.warn("Feishu send message failed: {}", res);
            return false;
        }
        return true;
    }

    private static class TokenCache {
        final String token;
        final long expireTimeMs;

        TokenCache(String token, long expireTimeMs) {
            this.token = token;
            this.expireTimeMs = expireTimeMs;
        }
    }
}
