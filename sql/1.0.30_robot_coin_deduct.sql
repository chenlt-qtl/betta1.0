-- 机器人按昵称扣减金币工具配置，支持“豆芽买文具消费100金币”等指令。

UPDATE robot_tool_config
SET class_name = 'com.betta.robot.tools.DeductCoinTool',
    tool_params = '{}',
    keywords = '消费,金币',
    priority = 120,
    description = '用户发送“昵称+消费内容+消费N金币”时，扣减对应用户金币',
    prompt = NULL,
    regex_pattern = '^\\s*(豆芽|桐桐)\\s*(.+?)\\s*消费\\s*(\\d+)\\s*金币\\s*$',
    regex_param_map = '{"1":"nickname","2":"content","3":"coinCost"}',
    llm_config_id = NULL,
    status = '0',
    remark = '机器人按昵称扣减金币',
    update_by = 'admin',
    update_time = sysdate()
WHERE config_name = '扣金币工具';

INSERT INTO robot_tool_config
(config_name, class_name, tool_params, keywords, priority, description, prompt, regex_pattern,
 regex_param_map, llm_config_id, status, remark, create_by, create_time)
SELECT '扣金币工具',
       'com.betta.robot.tools.DeductCoinTool',
       '{}',
       '消费,金币',
       120,
       '用户发送“昵称+消费内容+消费N金币”时，扣减对应用户金币',
       NULL,
       '^\\s*(豆芽|桐桐)\\s*(.+?)\\s*消费\\s*(\\d+)\\s*金币\\s*$',
       '{"1":"nickname","2":"content","3":"coinCost"}',
       NULL,
       '0',
       '机器人按昵称扣减金币',
       'admin',
       sysdate()
WHERE NOT EXISTS (
    SELECT 1 FROM robot_tool_config WHERE config_name = '扣金币工具'
);
