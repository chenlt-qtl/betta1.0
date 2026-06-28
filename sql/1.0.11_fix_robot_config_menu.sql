-- 修复机器人配置菜单无法打开的问题。
-- 原菜单 component 写成了 message/config/index，但新前端实际组件在 views/robot/config/index.vue。

-- 兼容旧表结构：早期建表字段名为 params，代码和后续脚本实际使用 tool_params。
SET @has_params := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'robot_tool_config'
      AND COLUMN_NAME = 'params'
);
SET @has_tool_params := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'robot_tool_config'
      AND COLUMN_NAME = 'tool_params'
);
SET @rename_params_sql := IF(
    @has_params > 0 AND @has_tool_params = 0,
    'ALTER TABLE robot_tool_config CHANGE COLUMN `params` `tool_params` TEXT NULL COMMENT ''参数（JSON格式）''',
    'SELECT 1'
);
PREPARE rename_params_stmt FROM @rename_params_sql;
EXECUTE rename_params_stmt;
DEALLOCATE PREPARE rename_params_stmt;

UPDATE sys_menu
SET component = 'robot/config/index',
    update_by = 'admin',
    update_time = sysdate()
WHERE component = 'message/config/index'
  AND path = 'message';

-- 工具配置接口使用 message:api:* 权限；补充导出按钮权限，避免导出接口无对应授权。
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '配置导出', menu_id, 16, '', '', 1, 0, 'F', '0', '0', 'message:api:export', '#', 'admin', sysdate(), '', NULL, ''
FROM sys_menu parent
WHERE parent.component = 'robot/config/index'
  AND parent.path = 'message'
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu child
      WHERE child.parent_id = parent.menu_id
        AND child.perms = 'message:api:export'
  );

-- 如果 admin 角色存在，给 admin 补上新导出按钮权限。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
LEFT JOIN sys_role_menu rm ON rm.role_id = 1 AND rm.menu_id = m.menu_id
WHERE m.perms = 'message:api:export'
  AND rm.menu_id IS NULL;
