-- 将首页自动登录指令改为动态昵称匹配，保留已部署环境的工具参数和工具类配置。
UPDATE robot_tool_config
SET keywords = '登录',
    description = '用户发送“昵称+登录”时，返回对应昵称用户的首页自动登录链接',
    regex_pattern = '^\\s*(\\S(?:.*\\S)?)登录\\s*$',
    regex_param_map = '{"1":"nickname"}',
    remark = '机器人按动态昵称生成首页自动登录链接',
    update_by = 'admin',
    update_time = sysdate()
WHERE config_name = '首页登录链接';
