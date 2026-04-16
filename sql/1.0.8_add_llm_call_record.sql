-- 大模型调用记录表
CREATE TABLE IF NOT EXISTS `llm_call_record`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `llm_config_id`   BIGINT       NULL DEFAULT NULL COMMENT '大模型配置ID',
    `model_name`      VARCHAR(64)  NULL DEFAULT NULL COMMENT '模型名称',
    `provider`        VARCHAR(32)  NULL DEFAULT NULL COMMENT '提供商：OPENAI/ANTHROPIC/BAIDU/ALIYUN/LOCAL等',
    `prompt` TEXT         NULL COMMENT '提示词',
    `response_content` TEXT         NULL COMMENT '响应内容（JSON格式）',
    `status`          VARCHAR(16)  NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SUCCESS/FAIL',
    `error_msg`       VARCHAR(512) NULL DEFAULT NULL COMMENT '失败原因',
    `duration`        INT          NULL DEFAULT NULL COMMENT '耗时（毫秒）',
    `create_time`     DATETIME     NULL DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_llm_config_id` (`llm_config_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='大模型调用记录';
