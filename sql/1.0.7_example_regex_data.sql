-- 正则配置示例数据
-- 执行时间：2026-04-11
-- 版本：1.0.7

-- 示例：配置加卡工具的正则
UPDATE robot_tool_config
SET regex_pattern = '(豆芽|桐桐)\s*(.*?),*\s*(加卡|加|消费|扣|扣卡)\s*(\d+)\s*(?:张|元|张卡)',
    regex_param_map = '{"1":"account","2":"conent","3":"action","4":"quantity"}'
WHERE config_name = '加卡工具';

-- 示例：配置查询余额工具的正则
UPDATE robot_tool_config
SET regex_pattern = '(?:现在|当前)?(?:有多少|剩余|查询|还剩|余额)?(?:多少张卡|多少卡|卡数|余额|张卡|多少元|元|是多少)',
    regex_param_map = '{}'
WHERE config_name = '查询余额';

-- 示例：配置启动任务工具的正则
UPDATE robot_tool_config
SET regex_pattern = '(?:启动|运行|执行|马上跑一下?)\s*(.+)',
    regex_param_map = '{"1":"taskName"}'
WHERE config_name = '启动任务';
