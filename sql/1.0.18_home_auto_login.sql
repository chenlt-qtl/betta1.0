-- 机器人按昵称生成首页自动登录链接工具配置
-- 前端地址优先保留已有配置，其次复用“跳舞链接”，均未配置时使用占位值。

SET @home_front_base_url = (
    SELECT CASE
               WHEN JSON_VALID(tool_params)
                   THEN JSON_UNQUOTE(JSON_EXTRACT(tool_params, '$.frontBaseUrl'))
               ELSE NULL
           END
    FROM robot_tool_config
    WHERE config_name = '首页登录链接'
    LIMIT 1
);

SET @dance_front_base_url = (
    SELECT CASE
               WHEN JSON_VALID(tool_params)
                   THEN JSON_UNQUOTE(JSON_EXTRACT(tool_params, '$.frontBaseUrl'))
               ELSE NULL
           END
    FROM robot_tool_config
    WHERE config_name = '跳舞链接'
    LIMIT 1
);

SET @home_login_front_base_url = COALESCE(
    NULLIF(TRIM(@home_front_base_url), ''),
    NULLIF(TRIM(@dance_front_base_url), ''),
    'https://your-domain.com'
);

UPDATE robot_tool_config
SET class_name = 'com.betta.robot.tools.HomeLoginLinkTool',
    tool_params = JSON_OBJECT(
        'frontBaseUrl', @home_login_front_base_url,
        'nickname', '',
        'targetPath', '/index',
        'expireMinutes', 5
    ),
    keywords = '登录',
    priority = 110,
    description = '用户发送“昵称+登录”时，返回对应昵称用户的首页自动登录链接',
    prompt = NULL,
    regex_pattern = '^\\s*(\\S(?:.*\\S)?)登录\\s*$',
    regex_param_map = '{"1":"nickname"}',
    llm_config_id = NULL,
    status = '0',
    remark = '机器人按昵称生成首页自动登录链接',
    update_by = 'admin',
    update_time = sysdate()
WHERE config_name = '首页登录链接';

INSERT INTO robot_tool_config
(config_name, class_name, tool_params, keywords, priority, description, prompt, regex_pattern, regex_param_map, llm_config_id, status, remark, create_by, create_time)
SELECT '首页登录链接',
       'com.betta.robot.tools.HomeLoginLinkTool',
       JSON_OBJECT(
           'frontBaseUrl', @home_login_front_base_url,
           'nickname', '',
           'targetPath', '/index',
           'expireMinutes', 5
       ),
       '登录',
       110,
       '用户发送“昵称+登录”时，返回对应昵称用户的首页自动登录链接',
       NULL,
       '^\\s*(\\S(?:.*\\S)?)登录\\s*$',
       '{"1":"nickname"}',
       NULL,
       '0',
       '机器人按昵称生成首页自动登录链接',
       'admin',
       sysdate()
WHERE NOT EXISTS (
    SELECT 1 FROM robot_tool_config WHERE config_name = '首页登录链接'
);
