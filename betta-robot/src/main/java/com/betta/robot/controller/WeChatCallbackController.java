package com.betta.robot.controller;

import com.betta.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 企业微信接收消息回调：GET 用于 URL 校验，POST 用于接收消息
 * 配置回调 URL：https://你的域名/message/wechat/callback
 * 若开启加密，需在后续实现解密逻辑后解析并入库、异步处理
 *
 * @author betta
 */
@Slf4j
@RestController
@RequestMapping("/message/wechat")
public class WeChatCallbackController {

    /**
     * GET：企业微信校验 URL 时使用
     *
     * @param msgSignature 签名
     * @param timestamp     时间戳
     * @param nonce         随机数
     * @param echostr       随机字符串
     * @return 校验通过时需原样返回 echostr（若使用加密需先解密再返回）
     */
    @GetMapping("/callback")
    public String verify(@RequestParam(required = false) String msg_signature,
                         @RequestParam(required = false) String timestamp,
                         @RequestParam(required = false) String nonce,
                         @RequestParam(required = false) String echostr) {
        if (StringUtils.isNotEmpty(echostr)) {
            return echostr;
        }
        return "";
    }

    /**
     * POST：接收企业微信消息（需根据 token/encodingAesKey 解密 body 后解析 XML，再入库并异步处理）
     * 当前为占位：直接返回空字符串避免企业微信重试；后续可接入解密与 MessageProcessService
     *
     * @param body 企业微信 POST 的 body（可能为加密或明文 XML）
     * @return 空或成功响应
     */
    @PostMapping("/callback")
    public String callback(@RequestBody(required = false) String body) {
        if (StringUtils.isNotBlank(body)) {
            log.info("WeChat callback body length: {}", body.length());
            // TODO: 解密 + 解析 XML 得到 FromUser, Content 等 -> 入库 MessageRecord -> messageProcessService.processReceiveMessageAsync(recordId)
        }
        return "";
    }
}
