-- ----------------------------
-- 加卡功能相关表
-- ----------------------------

-- 卡账户表
CREATE TABLE `card_account` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` varchar(32) NOT NULL COMMENT '人名',
    `balance` int DEFAULT 0 COMMENT '当前剩余卡数',
    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='卡账户表';

-- 卡历史记录表
CREATE TABLE `card_history` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `account_id` bigint NOT NULL COMMENT '关联 card_account.id',
    `change_value` int DEFAULT NULL COMMENT '变动值(正数为加卡,负数为扣卡)',
    `remain_value` int DEFAULT NULL COMMENT '变动后剩余',
    `content` varchar(500) DEFAULT NULL COMMENT '原因/内容',
    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_account_id` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='卡历史记录表';

-- 卡预设项表
CREATE TABLE `card_item` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `type` int DEFAULT NULL COMMENT '类型(1-加卡项 2-扣卡项)',
    `value` int DEFAULT NULL COMMENT '变动值',
    `name` varchar(32) DEFAULT NULL COMMENT '名称',
    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='卡预设项表';

-- 初始化一些预设项
INSERT INTO `card_item` (`type`, `value`, `name`, `remark`) VALUES
(1, 1, '加1张', '单次加1张卡'),
(1, 5, '加5张', '单次加5张卡'),
(1, 10, '加10张', '单次加10张卡'),
(2, -1, '扣1张', '单次扣1张卡'),
(2, -5, '扣5张', '单次扣5张卡'),
(2, -10, '扣10张', '单次扣10张卡');
