-- 每日单词完成记录同时支持跨文章日去重和按文章统计新词配额。
CREATE TABLE IF NOT EXISTS eng_daily_word_completion (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户主键',
    article_id BIGINT NOT NULL COMMENT '来源文章主键',
    word_id BIGINT NOT NULL COMMENT '单词主键',
    study_record_id BIGINT NULL COMMENT '学习记录主键',
    study_date DATE NOT NULL COMMENT '完成日期',
    category VARCHAR(16) NOT NULL COMMENT 'LEARNING/REVIEW/NEW',
    create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_eng_daily_user_word_date (user_id, word_id, study_date),
    KEY idx_eng_daily_new_quota (user_id, article_id, study_date, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日单词完成记录';

-- 重复执行只会再次校正越界历史值，不会改变合法熟悉度。
UPDATE eng_user_score SET familiarity=0 WHERE familiarity<0;
UPDATE eng_user_score SET familiarity=10 WHERE familiarity>10;
