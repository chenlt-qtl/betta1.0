-- 每个用户独立保存普通单词测试题型偏好；无记录时由服务端按全部启用处理。
CREATE TABLE IF NOT EXISTS eng_user_challenge_setting (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户主键',
  word_to_meaning_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '看词选义是否启用',
  meaning_to_word_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '看义选词是否启用',
  sentence_cloze_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '句子挖空是否启用',
  pronunciation_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '跟读是否启用',
  setting_version BIGINT NOT NULL DEFAULT 1 COMMENT '设置版本，用于使旧测试失效',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_eng_user_challenge_setting_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户单词测试题型设置';
