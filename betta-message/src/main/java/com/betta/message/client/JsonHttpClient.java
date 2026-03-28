package com.betta.message.client;

import com.alibaba.fastjson2.JSON;
import com.betta.common.constant.Constants;
import lombok.extern.slf4j.Slf4j;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 发送 JSON 的 HTTP 客户端（用于飞书/微信 Open API）
 *
 * @author betta
 */
@Slf4j
public final class JsonHttpClient {

    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 10000;

    private JsonHttpClient() {
    }

    /**
     * POST JSON 到指定 URL，并返回响应字符串
     *
     * @param url   URL
     * @param body  请求体对象（将序列化为 JSON）
     * @param token 可选，Bearer token，为 null 则不设置
     * @return 响应体字符串
     */
    public static String postJson(String url, Object body, String token) {
        String json = body == null ? "{}" : JSON.toJSONString(body);
        try {
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "application/json");
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
            conn.setDoOutput(true);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            String res = readResponse(conn);
            if (code >= 200 && code < 300) {
                return res;
            }
            log.warn("postJson failed: url={}, code={}, res={}", url, code, res);
            return res;
        } catch (Exception e) {
            log.error("postJson error: url=" + url, e);
            return null;
        }
    }

    /**
     * GET 请求
     *
     * @param urlWithParams 完整 URL（含参数）
     * @param token         可选 Bearer token
     * @return 响应体
     */
    public static String get(String urlWithParams, String token) {
        try {
            URL u = new URL(urlWithParams);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            return readResponse(conn);
        } catch (Exception e) {
            log.error("get error: url=" + urlWithParams, e);
            return null;
        }
    }

    private static String readResponse(HttpURLConnection conn) throws java.io.IOException {
        java.io.InputStream is = conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream();
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
