-- 新词永久分关、用户全局词进度、关卡进度和测试词明细。

SET @has_level_no := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME='eng_article_word_rel' AND COLUMN_NAME='level_no');
SET @sql := IF(@has_level_no=0,
  'ALTER TABLE eng_article_word_rel ADD COLUMN level_no INT NULL COMMENT ''永久关卡号'' AFTER word_name, ADD KEY idx_eng_article_level(article_id,level_no,id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 历史重复关系保留最早添加顺序，然后用唯一约束保障所有新增入口幂等。
UPDATE eng_article_word_rel SET create_by='' WHERE create_by IS NULL;
DELETE duplicate_rel FROM eng_article_word_rel duplicate_rel
JOIN eng_article_word_rel kept
  ON kept.article_id=duplicate_rel.article_id AND kept.word_name=duplicate_rel.word_name
 AND kept.create_by=duplicate_rel.create_by AND kept.id<duplicate_rel.id;
SET @has_unique_article_word := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME='eng_article_word_rel' AND INDEX_NAME='uk_eng_article_word_owner');
SET @sql := IF(@has_unique_article_word=0,
  'ALTER TABLE eng_article_word_rel ADD UNIQUE KEY uk_eng_article_word_owner(article_id,word_name,create_by)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE eng_article_word_rel r
JOIN (
  SELECT id,CEIL(ROW_NUMBER() OVER(PARTITION BY article_id ORDER BY id)/5) level_no
  FROM eng_article_word_rel WHERE article_id>0
) numbered ON numbered.id=r.id
SET r.level_no=numbered.level_no WHERE r.level_no IS NULL;

CREATE TABLE IF NOT EXISTS eng_user_word_progress (
  id BIGINT NOT NULL AUTO_INCREMENT,user_id BIGINT NOT NULL,word_id BIGINT NOT NULL,learned TINYINT NOT NULL DEFAULT 0,
  highest_stars TINYINT NOT NULL DEFAULT 0,latest_stars TINYINT NOT NULL DEFAULT 0,latest_test_time DATETIME NULL,
  rewarded_stars TINYINT NOT NULL DEFAULT 0,first_article_id BIGINT NULL,first_level_no INT NULL,
  create_by VARCHAR(64) DEFAULT '',create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(id),UNIQUE KEY uk_eng_user_word_progress(user_id,word_id),
  KEY idx_eng_user_word_review(user_id,learned,latest_test_time),
  CONSTRAINT fk_eng_user_word_progress_word FOREIGN KEY(word_id) REFERENCES eng_word(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户规范词全局进度';

CREATE TABLE IF NOT EXISTS eng_article_level_progress (
  id BIGINT NOT NULL AUTO_INCREMENT,user_id BIGINT NOT NULL,article_id BIGINT NOT NULL,level_no INT NOT NULL,
  best_score INT NOT NULL DEFAULT 0,highest_stars TINYINT NOT NULL DEFAULT 0,
  completed_by_known_words TINYINT NOT NULL DEFAULT 0,
  create_by VARCHAR(64) DEFAULT '',create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(id),UNIQUE KEY uk_eng_article_level_progress(user_id,article_id,level_no),
  KEY idx_eng_article_level_progress_article(article_id,level_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户文章关卡进度';

SET @has_attempt_id := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME='eng_study_record' AND COLUMN_NAME='attempt_id');
SET @sql := IF(@has_attempt_id=0,
  'ALTER TABLE eng_study_record ADD COLUMN attempt_id VARCHAR(64) NULL AFTER article_id, ADD COLUMN study_mode VARCHAR(16) NOT NULL DEFAULT ''NEW'' AFTER attempt_id, ADD COLUMN level_no INT NULL AFTER study_mode, ADD COLUMN stars TINYINT NOT NULL DEFAULT 0 AFTER passed, ADD COLUMN milestone_coin BIGINT NOT NULL DEFAULT 0 AFTER stars, ADD COLUMN review_coin BIGINT NOT NULL DEFAULT 0 AFTER milestone_coin, ADD UNIQUE KEY uk_eng_study_record_attempt(user_id,attempt_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE eng_study_record MODIFY COLUMN article_id BIGINT NULL COMMENT '新词来源文章，复习可空';

CREATE TABLE IF NOT EXISTS eng_study_record_word (
  id BIGINT NOT NULL AUTO_INCREMENT,study_record_id BIGINT NOT NULL,word_id BIGINT NOT NULL,
  source_article_id BIGINT NULL,study_mode VARCHAR(16) NOT NULL,correct_count INT NULL,total_count INT NULL,
  all_correct TINYINT NOT NULL DEFAULT 0,stars TINYINT NOT NULL DEFAULT 0,
  milestone_coin BIGINT NOT NULL DEFAULT 0,review_coin BIGINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(id),UNIQUE KEY uk_eng_study_record_word(study_record_id,word_id),
  KEY idx_eng_study_record_word_word(word_id),
  CONSTRAINT fk_eng_study_record_word_record FOREIGN KEY(study_record_id) REFERENCES eng_study_record(id) ON DELETE CASCADE,
  CONSTRAINT fk_eng_study_record_word_word FOREIGN KEY(word_id) REFERENCES eng_word(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习记录规范词明细';

-- 历史测试明细不得因删除词典词条而级联丢失。
SET @word_fk_rule := (SELECT DELETE_RULE FROM information_schema.REFERENTIAL_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='eng_study_record_word'
    AND CONSTRAINT_NAME='fk_eng_study_record_word_word' LIMIT 1);
SET @sql := IF(@word_fk_rule='CASCADE',
  'ALTER TABLE eng_study_record_word DROP FOREIGN KEY fk_eng_study_record_word_word, ADD CONSTRAINT fk_eng_study_record_word_word FOREIGN KEY(word_id) REFERENCES eng_word(id) ON DELETE RESTRICT',
  IF(@word_fk_rule IS NULL,
    'ALTER TABLE eng_study_record_word ADD CONSTRAINT fk_eng_study_record_word_word FOREIGN KEY(word_id) REFERENCES eng_word(id) ON DELETE RESTRICT',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 仅回填旧记录头的星级，不补发任何历史金币。
UPDATE eng_study_record
SET stars=CASE WHEN score=100 THEN 3 WHEN score>=90 THEN 2 WHEN score>=80 THEN 1 ELSE 0 END
WHERE attempt_id IS NULL
  AND stars<>CASE WHEN score=100 THEN 3 WHEN score>=90 THEN 2 WHEN score>=80 THEN 1 ELSE 0 END;

-- 历史文章进度只迁到第一关；旧 completed 用户至少保留一星解锁，且不补发金币。
INSERT IGNORE INTO eng_article_level_progress(user_id,article_id,level_no,best_score,highest_stars,
 completed_by_known_words,create_by,create_time,update_by,update_time)
SELECT user_id,article_id,1,best_score,
 CASE WHEN best_score=100 THEN 3 WHEN best_score>=90 THEN 2 WHEN best_score>=80 OR completed=1 THEN 1 ELSE 0 END,
 0,create_by,create_time,update_by,update_time FROM eng_article_progress;

-- 每日完成历史可确认“学过”，按关联学习记录近似迁移星级；rewarded_stars 同步，绝不补发旧金币。
INSERT INTO eng_user_word_progress(user_id,word_id,learned,highest_stars,latest_stars,latest_test_time,
 rewarded_stars,first_article_id,first_level_no,create_by,create_time,update_by,update_time)
SELECT d.user_id,d.word_id,1,
 MAX(CASE WHEN r.score=100 THEN 3 WHEN r.score>=90 THEN 2 WHEN r.score>=80 THEN 1 ELSE 0 END),
 SUBSTRING_INDEX(GROUP_CONCAT(CASE WHEN r.score=100 THEN 3 WHEN r.score>=90 THEN 2 WHEN r.score>=80 THEN 1 ELSE 0 END
   ORDER BY d.study_date DESC,d.id DESC),',',1),MAX(d.create_time),
 MAX(CASE WHEN r.score=100 THEN 3 WHEN r.score>=90 THEN 2 WHEN r.score>=80 THEN 1 ELSE 0 END),
 SUBSTRING_INDEX(GROUP_CONCAT(d.article_id ORDER BY d.study_date,d.id),',',1),
 SUBSTRING_INDEX(GROUP_CONCAT(COALESCE(awr.level_no,1) ORDER BY d.study_date,d.id),',',1),
 MAX(d.create_by),MIN(d.create_time),MAX(d.create_by),MAX(d.create_time)
FROM eng_daily_word_completion d LEFT JOIN eng_study_record r ON r.id=d.study_record_id
LEFT JOIN eng_word w ON w.id=d.word_id
LEFT JOIN eng_article_word_rel awr ON awr.article_id=d.article_id AND awr.word_name=w.word_name
GROUP BY d.user_id,d.word_id
ON DUPLICATE KEY UPDATE learned=1,highest_stars=greatest(highest_stars,values(highest_stars)),
 latest_stars=if(latest_test_time is null or values(latest_test_time)>latest_test_time,
   values(latest_stars),latest_stars),
 latest_test_time=if(latest_test_time is null or values(latest_test_time)>latest_test_time,
   values(latest_test_time),latest_test_time),
 rewarded_stars=greatest(rewarded_stars,values(rewarded_stars));

INSERT IGNORE INTO eng_user_word_progress(user_id,word_id,learned,highest_stars,latest_stars,rewarded_stars,
 create_by,create_time,update_by,update_time)
SELECT u.user_id,w.id,1,0,0,0,s.user,s.create_time,s.user,coalesce(s.update_time,s.create_time)
FROM eng_user_score s JOIN sys_user u ON u.user_name=s.user JOIN eng_word w ON w.word_name=s.word_name;

INSERT IGNORE INTO eng_study_record_word(study_record_id,word_id,source_article_id,study_mode,all_correct,stars,create_time)
SELECT d.study_record_id,d.word_id,d.article_id,'NEW',0,
 CASE WHEN r.score=100 THEN 3 WHEN r.score>=90 THEN 2 WHEN r.score>=80 THEN 1 ELSE 0 END,d.create_time
FROM eng_daily_word_completion d JOIN eng_study_record r ON r.id=d.study_record_id WHERE d.study_record_id IS NOT NULL;

INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,
 menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9116,'文章关卡',9100,14,'study/levels/:articleId','eng/study/levelMap','','',1,0,'C','1','0',
 'eng:study:challenge','star','admin',NOW(),'文章新词关卡地图'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9116);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,
 menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9117,'单词复习',9100,4,'study/review','eng/study/review','','',1,0,'C','0','0',
 'eng:study:challenge','skill','admin',NOW(),'全局单词复习'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9117);
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) VALUES(1,9116),(1,9117);
