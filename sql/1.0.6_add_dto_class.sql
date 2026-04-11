-- 添加 dto_class 字段到 robot_tool_config 表
ALTER TABLE `robot_tool_config` ADD COLUMN `dto_class` VARCHAR(512) NULL DEFAULT NULL COMMENT 'DTO类名（用于参数反序列化）' AFTER `prompt`;
