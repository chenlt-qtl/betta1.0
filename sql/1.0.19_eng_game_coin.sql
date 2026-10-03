-- 英语闯关金币钱包：旧学习记录不补发金币，新版提交开始记录实际奖励。

CREATE TABLE IF NOT EXISTS `eng_user_coin_wallet` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '金币钱包主键',
  `user_id` bigint NOT NULL COMMENT '用户主键',
  `coin_balance` bigint unsigned NOT NULL DEFAULT 0 COMMENT '当前金币余额',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eng_user_coin_wallet_user` (`user_id`)
) ENGINE=InnoDB COMMENT='英语学习用户金币钱包';

SET @has_coin_reward := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'eng_study_record'
      AND COLUMN_NAME = 'coin_reward'
);
SET @add_coin_reward_sql := IF(
    @has_coin_reward = 0,
    'ALTER TABLE `eng_study_record` ADD COLUMN `coin_reward` BIGINT NOT NULL DEFAULT 0 COMMENT ''本轮获得金币'' AFTER `passed`',
    'SELECT 1'
);
PREPARE add_coin_reward_stmt FROM @add_coin_reward_sql;
EXECUTE add_coin_reward_stmt;
DEALLOCATE PREPARE add_coin_reward_stmt;
