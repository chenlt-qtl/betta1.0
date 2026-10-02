-- 独立保存每个用户的当前学习文章。
CREATE TABLE IF NOT EXISTS `eng_current_article` (
  `user_id` bigint NOT NULL COMMENT '用户主键',
  `article_id` bigint NOT NULL COMMENT '当前文章主键',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  KEY `idx_eng_current_article_article_id` (`article_id`)
) ENGINE=InnoDB COMMENT='用户当前学习文章';
