
-- 消息通道配置表（飞书/微信等，页面配置，不写死配置文件）
CREATE TABLE IF NOT EXISTS `message_channel_config`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `channel_type`    VARCHAR(32)  NOT NULL COMMENT '渠道：FEISHU/WECHAT',
    `channel_name`    VARCHAR(64)  NULL DEFAULT NULL COMMENT '渠道名称（展示用）',
    `app_id`          VARCHAR(128) NULL DEFAULT NULL COMMENT '应用ID',
    `app_secret`      VARCHAR(256) NULL DEFAULT NULL COMMENT '应用密钥',
    `encrypt_key`     VARCHAR(256) NULL DEFAULT NULL COMMENT '回调加密密钥',
    `verification_token` VARCHAR(256) NULL DEFAULT NULL COMMENT '回调校验Token',
    `agent_id`        VARCHAR(64)  NULL DEFAULT NULL COMMENT '企业微信AgentId',
    `corp_id`         VARCHAR(128) NULL DEFAULT NULL COMMENT '企业微信CorpId',
    `status`          CHAR(1)      NULL DEFAULT '0' COMMENT '状态：0启用 1停用',
    `remark`          VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
    `create_by`       VARCHAR(64)  NULL DEFAULT NULL,
    `create_time`     DATETIME     NULL DEFAULT NULL,
    `update_by`       VARCHAR(64)  NULL DEFAULT NULL,
    `update_time`     DATETIME     NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_channel_type` (`channel_type`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='消息通道配置';

-- 消息记录表（收发均可查）
CREATE TABLE IF NOT EXISTS `message_record`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_id`       BIGINT       NULL DEFAULT NULL COMMENT '通道配置ID',
    `channel_type`    VARCHAR(32)  NOT NULL COMMENT '渠道：FEISHU/WECHAT',
    `direction`       VARCHAR(16)  NOT NULL COMMENT '方向：RECEIVE/SEND',
    `content`         TEXT         NULL COMMENT '消息内容',
    `reply_content`   TEXT         NULL COMMENT '回复内容（仅RECEIVE时有）',
    `chat_id`         VARCHAR(128) NULL DEFAULT NULL COMMENT '会话ID（回发用）',
    `user_id`         VARCHAR(128) NULL DEFAULT NULL COMMENT '用户ID',
    `message_id`      VARCHAR(128) NULL DEFAULT NULL COMMENT '平台消息ID',
    `parsed_intent`   VARCHAR(64)  NULL DEFAULT NULL COMMENT '解析意图：add_card/start_task等',
    `parsed_json`     VARCHAR(1024) NULL DEFAULT NULL COMMENT '解析结果JSON',
    `status`          VARCHAR(16)  NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAIL',
    `error_msg`       VARCHAR(512) NULL DEFAULT NULL COMMENT '失败原因',
    `create_time`     DATETIME     NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_config_id` (`config_id`),
    KEY `idx_channel_direction` (`channel_type`, `direction`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='消息记录';

-- 菜单：消息通道管理（放在"系统管理"下，parent_id 需根据实际 sys_menu 中"系统管理"的 id 调整，这里用 1 占位）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('消息通道', 1, 10, 'message', 'message/config/index', 1, 0, 'C', '0', '0', 'message:config:list', 'message', 'admin', sysdate(), '', NULL, '飞书/微信通道配置与消息记录');

SET @messageMenuId := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('通道配置', @messageMenuId, 1, '', '', 1, 0, 'F', '0', '0', 'message:config:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('配置新增', @messageMenuId, 2, '', '', 1, 0, 'F', '0', '0', 'message:config:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('配置修改', @messageMenuId, 3, '', '', 1, 0, 'F', '0', '0', 'message:config:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('配置删除', @messageMenuId, 4, '', '', 1, 0, 'F', '0', '0', 'message:config:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('消息记录', @messageMenuId, 5, '', '', 1, 0, 'F', '0', '0', 'message:record:list', '#', 'admin', sysdate(), '', NULL, '');

-- 大模型配置表
CREATE TABLE IF NOT EXISTS `message_llm_config`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_name`     VARCHAR(64)  NOT NULL COMMENT '配置名称',
    `provider`       VARCHAR(32)  NOT NULL COMMENT '提供商：OPENAI/ANTHROPIC/BAIDU/ALIYUN/LOCAL等',
    `api_key`         VARCHAR(256) NULL DEFAULT NULL COMMENT 'API Key',
    `api_endpoint`    VARCHAR(512) NULL DEFAULT NULL COMMENT 'API 端点（可选，本地部署时使用）',
    `model_name`      VARCHAR(64)  NOT NULL COMMENT '模型名称',
    `temperature`    DECIMAL(3,2) NULL DEFAULT 0.7 COMMENT '温度参数（0-1）',
    `max_tokens`      INT          NULL DEFAULT 2000 COMMENT '最大Token数',
    `top_p`           DECIMAL(3,2) NULL DEFAULT 1.00 COMMENT 'Top P 参数',
    `timeout`        INT          NULL DEFAULT 30 COMMENT '超时时间（秒）',
    `status`          CHAR(1)      NULL DEFAULT '0' COMMENT '状态：0启用 1停用',
    `remark`          VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
    `create_by`       VARCHAR(64)  NULL DEFAULT NULL,
    `create_time`     DATETIME     NULL DEFAULT NULL,
    `update_by`       VARCHAR(64)  NULL DEFAULT NULL,
    `update_time`     DATETIME     NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_provider` (`provider`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='大模型配置';

-- 菜单：大模型配置（改为按钮权限）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('大模型配置', @messageMenuId, 6, '', '', 1, 0, 'F', '0', '0', 'message:llm:list', '#', 'admin', sysdate(), '', NULL, '');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('配置查询', @messageMenuId, 7, '', '', 1, 0, 'F', '0', '0', 'message:llm:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('配置新增', @messageMenuId, 8, '', '', 1, 0, 'F', '0', '0', 'message:llm:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('配置修改', @messageMenuId, 9, '', '', 1, 0, 'F', '0', '0', 'message:llm:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('配置删除', @messageMenuId, 10, '', '', 1, 0, 'F', '0', '0', 'message:llm:remove', '#', 'admin', sysdate(), '', NULL, '');

-- API配置表
CREATE TABLE IF NOT EXISTS `message_api_config`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_name`     VARCHAR(64)  NOT NULL COMMENT '配置名称',
    `api_url`         VARCHAR(512) NOT NULL COMMENT 'API地址',
    `api_params`      TEXT         NULL COMMENT 'API参数（JSON格式，如：{"name":"${name}"}）',
    `keywords`        VARCHAR(512) NULL DEFAULT NULL COMMENT '关键词（多个用逗号分隔）',
    `priority`        INT          NULL DEFAULT 0 COMMENT '优先级（数字越大越优先）',
    `description`     VARCHAR(500) NULL DEFAULT NULL COMMENT '描述（用于大模型选择）',
    `prompt`          TEXT         NULL COMMENT '提示词（用于提取参数）',
    `llm_config_id`   BIGINT       NULL DEFAULT NULL COMMENT '关联的大模型配置ID',
    `status`          CHAR(1)      NULL DEFAULT '0' COMMENT '状态：0启用 1停用',
    `remark`          VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
    `create_by`       VARCHAR(64)  NULL DEFAULT NULL,
    `create_time`     DATETIME     NULL DEFAULT NULL,
    `update_by`       VARCHAR(64)  NULL DEFAULT NULL,
    `update_time`     DATETIME     NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_priority` (`priority`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='API配置';

-- 菜单：API配置（改为按钮权限）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('API配置', @messageMenuId, 11, '', '', 1, 0, 'F', '0', '0', 'message:api:list', '#', 'admin', sysdate(), '', NULL, '');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('配置查询', @messageMenuId, 12, '', '', 1, 0, 'F', '0', '0', 'message:api:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('配置新增', @messageMenuId, 13, '', '', 1, 0, 'F', '0', '0', 'message:api:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('配置修改', @messageMenuId, 14, '', '', 1, 0, 'F', '0', '0', 'message:api:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('配置删除', @messageMenuId, 15, '', '', 1, 0, 'F', '0', '0', 'message:api:remove', '#', 'admin', sysdate(), '', NULL, '');

-- 角色菜单权限分配：将消息通道相关菜单分配给admin角色(role_id=1)
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, @messageMenuId);

-- 同时将所有按钮权限分配给admin角色（通过查询刚插入的按钮menu_id）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE parent_id = @messageMenuId AND menu_type = 'F';
