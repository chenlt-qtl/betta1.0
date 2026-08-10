-- 工具执行参数已统一使用 Map，不再依赖 DTO 类名反序列化。
ALTER TABLE robot_tool_config DROP COLUMN dto_class;
