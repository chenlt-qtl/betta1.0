-- 机器人“跳舞”自动登录链接工具配置
-- 上线前请将 frontBaseUrl 替换为 betta1.0/betta-ui 的真实前端域名。

UPDATE robot_tool_config
SET class_name = 'com.betta.robot.tools.DanceLinkTool',
    tool_params = '{"frontBaseUrl":"https://your-domain.com","username":"damu","targetPath":"/dance","expireMinutes":5}',
    keywords = '跳舞,dance',
    priority = 100,
    description = '用户发送跳舞时，返回自动登录damu并跳转跳舞页面的短期链接',
    prompt = NULL,
    dto_class = NULL,
    regex_pattern = '^\\s*(跳舞|dance)\\s*$',
    regex_param_map = NULL,
    llm_config_id = NULL,
    status = '0',
    remark = '机器人跳舞自动登录链接',
    update_by = 'admin',
    update_time = sysdate()
WHERE config_name = '跳舞链接';

INSERT INTO robot_tool_config
(config_name, class_name, tool_params, keywords, priority, description, prompt, dto_class, regex_pattern, regex_param_map, llm_config_id, status, remark, create_by, create_time)
SELECT '跳舞链接',
       'com.betta.robot.tools.DanceLinkTool',
       '{"frontBaseUrl":"https://your-domain.com","username":"damu","targetPath":"/dance","expireMinutes":5}',
       '跳舞,dance',
       100,
       '用户发送跳舞时，返回自动登录damu并跳转跳舞页面的短期链接',
       NULL,
       NULL,
       '^\\s*(跳舞|dance)\\s*$',
       NULL,
       NULL,
       '0',
       '机器人跳舞自动登录链接',
       'admin',
       sysdate()
WHERE NOT EXISTS (
    SELECT 1 FROM robot_tool_config WHERE config_name = '跳舞链接'
);
