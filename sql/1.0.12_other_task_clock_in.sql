-- 其他模块：任务与打卡数据

CREATE TABLE IF NOT EXISTS `task_info`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `type`        BIGINT       NULL DEFAULT NULL COMMENT '类型：1普通任务 2长期任务',
    `task_status` BIGINT       NULL DEFAULT NULL COMMENT '任务状态：1进行中 2已完成',
    `content`     VARCHAR(500) NULL DEFAULT NULL COMMENT '任务内容',
    `comment`     VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME     NULL DEFAULT NULL,
    `create_by`   VARCHAR(64)  NULL DEFAULT NULL,
    `update_time` DATETIME     NULL DEFAULT NULL,
    `update_by`   VARCHAR(64)  NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_type_status` (`type`, `task_status`),
    KEY `idx_create_by_time` (`create_by`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务表';

CREATE TABLE IF NOT EXISTS `clock_in_data`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `time`        DATE         NULL DEFAULT NULL COMMENT '打卡日期',
    `value`       VARCHAR(200) NULL DEFAULT NULL COMMENT '值',
    `create_time` DATETIME     NULL DEFAULT NULL,
    `create_by`   VARCHAR(64)  NULL DEFAULT NULL,
    `update_time` DATETIME     NULL DEFAULT NULL,
    `update_by`   VARCHAR(64)  NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_time` (`time`),
    KEY `idx_create_by` (`create_by`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='打卡数据表';

-- 菜单与按钮权限
INSERT IGNORE INTO sys_menu VALUES('2200', '其他', '0', '6', 'other', NULL, '', '', 1, 0, 'M', '0', '0', '', 'list', 'admin', sysdate(), '', NULL, '其他功能目录');
INSERT IGNORE INTO sys_menu VALUES('2201', '任务', '2200', '1', 'task', 'other/task/index', '', '', 1, 0, 'C', '0', '0', 'other:task:list', 'job', 'admin', sysdate(), '', NULL, '任务页面');
INSERT IGNORE INTO sys_menu VALUES('2202', '打卡数据', '2200', '2', 'clockInData', 'other/clockInData/index', '', '', 1, 0, 'C', '0', '0', 'other:clockInData:list', 'time', 'admin', sysdate(), '', NULL, '打卡数据页面');

INSERT IGNORE INTO sys_menu VALUES('2210', '任务查询', '2201', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'other:task:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu VALUES('2211', '任务新增', '2201', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'other:task:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu VALUES('2212', '任务修改', '2201', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'other:task:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu VALUES('2213', '任务删除', '2201', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'other:task:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu VALUES('2214', '任务导出', '2201', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'other:task:export', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_menu VALUES('2220', '打卡查询', '2202', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'other:clockInData:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu VALUES('2221', '打卡新增', '2202', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'other:clockInData:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu VALUES('2222', '打卡修改', '2202', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'other:clockInData:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu VALUES('2223', '打卡删除', '2202', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'other:clockInData:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu VALUES('2224', '打卡导出', '2202', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'other:clockInData:export', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 2200 AND 2224;
