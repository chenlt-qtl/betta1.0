package com.betta.eng.service.impl;

import com.betta.common.exception.ServiceException;
import com.betta.eng.config.EngPronunciationProperties;
import com.tencent.soe.OralEvaluationResponse;

/** 腾讯云客户端配置保护的独立回归入口，不依赖真实云端凭据。 */
public class TencentPronunciationAssessmentClientTest
{
    public static void main(String[] args)
    {
        shouldRejectMissingConfiguration();
        shouldPreserveUnexpectedException();
        shouldPreserveTencentFailureDetails();
        shouldPreserveWebSocketCloseDetails();
        shouldUseTencentSoeWebSocketEndpoint();
    }

    private static void shouldRejectMissingConfiguration()
    {
        EngPronunciationProperties properties = new EngPronunciationProperties();
        TencentPronunciationAssessmentClient client = new TencentPronunciationAssessmentClient(properties);
        try
        {
            client.assess("apple", new byte[44]);
            throw new AssertionError("未配置凭据时不得调用腾讯云");
        }
        catch (ServiceException expected)
        {
            if (!expected.getMessage().contains("暂未启用")) throw expected;
        }
    }

    private static void shouldPreserveUnexpectedException()
    {
        IllegalStateException cause = new IllegalStateException("sdk failure");
        ServiceException exception = TencentPronunciationAssessmentClient.unexpectedFailure(cause);
        if (exception.getCause() != cause) throw new AssertionError("必须保留 SDK 原始异常");
    }

    private static void shouldPreserveTencentFailureDetails()
    {
        OralEvaluationResponse response = new OralEvaluationResponse();
        response.setCode(1001); response.setMessage("invalid credential");
        ServiceException exception = TencentPronunciationAssessmentClient.responseFailure(response);
        String detail = exception.getCause() == null ? "" : exception.getCause().getMessage();
        if (!detail.contains("1001") || !detail.contains("invalid credential"))
            throw new AssertionError("必须保留腾讯云失败响应的错误码和消息");
    }

    private static void shouldPreserveWebSocketCloseDetails()
    {
        ServiceException exception = TencentPronunciationAssessmentClient.connectionFailure(1008, "policy violation");
        String detail = exception.getCause() == null ? "" : exception.getCause().getMessage();
        if (!detail.contains("1008") || !detail.contains("policy violation"))
            throw new AssertionError("必须保留 WebSocket 关闭码和原因");
    }

    private static void shouldUseTencentSoeWebSocketEndpoint()
    {
        String endpoint = TencentPronunciationAssessmentClient.soeEndpoint();
        if (!"wss://soe.cloud.tencent.com/soe/api/".equals(endpoint))
            throw new AssertionError("必须使用腾讯云 SOE TLS WebSocket 地址");
    }
}
