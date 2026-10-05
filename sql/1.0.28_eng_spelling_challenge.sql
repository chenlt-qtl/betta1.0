-- 拼写测试奖励星级与普通测试里程碑独立记录；历史数据默认未领取拼写奖励。
SET @has_spelling_rewarded_stars := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='eng_user_word_progress'
    AND COLUMN_NAME='spelling_rewarded_stars');
SET @sql := IF(@has_spelling_rewarded_stars=0,
  'ALTER TABLE eng_user_word_progress ADD COLUMN spelling_rewarded_stars TINYINT NOT NULL DEFAULT 0 COMMENT ''拼写测试已奖励最高星级'' AFTER rewarded_stars',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
