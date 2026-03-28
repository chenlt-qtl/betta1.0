package com.betta.message.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.betta.common.utils.http.HttpUtils;
import com.betta.message.domain.MessageChannelConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.betta.common.constant.Constants;

/**
 * 企业微信 API 客户端：获取 access_token、发送消息
 *
 * @author betta
 */
@Slf4j
@Component
public class WeChatClient {

    private static final String URL_GET_TOKEN = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s";
    private static final String URL_SEND_MESSAGE = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=%s";

    private final Map<Long, TokenCache> tokenCache = new ConcurrentHashMap<>();

    /**
     * 获取 access_token（带简单内存缓存）
     *
     * @param config 通道配置
     * @return token，失败返回 null
     */
    public String getAccessToken(MessageChannelConfig config) {
        if (config == null || StringUtils.isBlank(config.getCorpId()) || StringUtils.isBlank(config.getAppSecret())) {
            return null;
        }
        TokenCache cached = tokenCache.get(config.getId());
        if (cached != null && cached.expireTimeMs > System.currentTimeMillis()) {
            return cached.token;
        }
        String url = String.format(URL_GET_TOKEN, config.getCorpId(), config.getAppSecret());
        String res = HttpUtils.sendGet(url, "", Constants.UTF8);
        if (StringUtils.isBlank(res)) {
            return null;
        }
        JSONObject obj = JSON.parseObject(res);
        if (obj.getIntValue("errcode") != 0) {
            log.warn("WeChat get token failed: {}", res);
            return null;
        }
        String token = obj.getString("access_token");
        int expire = obj.getIntValue("expires_in");
        tokenCache.put(config.getId(), new TokenCache(token, System.currentTimeMillis() + (expire - 60) * 1000L));
        return token;
    }

    /**
     * 发送文本消息给指定用户（企业微信：touser 为 userid）
     *
     * @param config 通道配置
     * @param userId 用户 id（企业微信 member 的 userid）
     * @param text   文本内容
     * @return 是否成功
     */
    public boolean sendText(MessageChannelConfig config, String userId, String text) {
        String token = getAccessToken(config);
        if (token == null || StringUtils.isBlank(userId)) {
            return false;
        }
        String url = String.format(URL_SEND_MESSAGE, token);
        JSONObject body = new JSONObject();
        body.put("touser", userId);
        body.put("msgtype", "text");
        body.put("agentid", config.getAgentId());
        JSONObject content = new JSONObject();
        content.put("content", text == null ? "" : text);
        body.put("text", content);
        String res = JsonHttpClient.postJson(url, body, null);
        if (StringUtils.isBlank(res)) {
            return false;
        }
        JSONObject obj = JSON.parseObject(res);
        if (obj.getIntValue("errcode") != 0) {
            log.warn("WeChat send message failed: {}", res);
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
