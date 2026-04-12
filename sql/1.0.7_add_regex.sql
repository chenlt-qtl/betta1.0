-- 为 robot_tool_config 表新增正则表达式和参数映射字段
-- 执行时间：2026-04-11
-- 版本：1.0.7

-- 新增字段
ALTER TABLE robot_tool_config ADD COLUMN regex_pattern VARCHAR(1024) DEFAULT NULL COMMENT '正则表达式';

ALTER TABLE robot_tool_config ADD COLUMN regex_param_map TEXT DEFAULT NULL COMMENT '捕获组参数映射（JSON格式，key为组号，value为DTO字段名）';
