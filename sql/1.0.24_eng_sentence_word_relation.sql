-- 文章自定义句子与规范词的显式关系，解决复数、变体回显和句子题关联不稳定问题。

CREATE TABLE IF NOT EXISTS `eng_sentence_word_rel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `sentence_id` BIGINT NOT NULL COMMENT '句子ID',
    `word_id` BIGINT NOT NULL COMMENT '规范词ID',
    `matched_text` VARCHAR(200) NOT NULL COMMENT '句中实际命中的词形',
    `create_by` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eng_sentence_word_rel_sentence_word` (`sentence_id`, `word_id`),
    KEY `idx_eng_sentence_word_rel_word` (`word_id`),
    CONSTRAINT `fk_eng_sentence_word_rel_sentence` FOREIGN KEY (`sentence_id`)
        REFERENCES `eng_sentence` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_eng_sentence_word_rel_word` FOREIGN KEY (`word_id`)
        REFERENCES `eng_word` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='句子与规范词关系表';

-- 只在同一文章已有词表范围内回填。候选词形出现歧义时不写入，避免建立错误关系。
INSERT IGNORE INTO `eng_sentence_word_rel`
    (`sentence_id`, `word_id`, `matched_text`, `create_by`, `create_time`)
WITH `article_words` AS (
    SELECT DISTINCT r.article_id,w.id word_id,w.word_name,r.create_by
    FROM eng_article_word_rel r
    JOIN eng_word w ON w.word_name=r.word_name
    WHERE r.article_id>0 AND w.word_name IS NOT NULL AND w.word_name!=''
    UNION
    SELECT DISTINCT r.article_id,w.id,w.word_name,r.create_by
    FROM eng_article_word_rel r
    JOIN eng_word_alias a ON a.alias_key=LOWER(r.word_name)
    JOIN eng_word w ON w.id=a.word_id
    WHERE r.article_id>0
      AND NOT EXISTS(SELECT 1 FROM eng_word exact_word WHERE exact_word.word_name=r.word_name)
    UNION
    SELECT DISTINCT r.article_id,w.id,w.word_name,r.create_by
    FROM eng_article_word_rel r
    JOIN eng_word w ON LOWER(w.prototype)=LOWER(r.word_name)
    WHERE r.article_id>0 AND w.prototype IS NOT NULL AND w.prototype!=''
      AND NOT EXISTS(SELECT 1 FROM eng_word exact_word WHERE exact_word.word_name=r.word_name)
      AND NOT EXISTS(SELECT 1 FROM eng_word_alias a WHERE a.alias_key=LOWER(r.word_name))
),
`candidate_forms` AS (
    SELECT article_id,word_id,LOWER(word_name) matched_form,create_by
    FROM article_words
    WHERE word_name REGEXP '^[A-Za-z]+(''[A-Za-z]+)?$'
    UNION
    SELECT aw.article_id,aw.word_id,LOWER(a.alias_key),aw.create_by
    FROM article_words aw JOIN eng_word_alias a ON a.word_id=aw.word_id
    WHERE a.alias_key REGEXP '^[A-Za-z]+(''[A-Za-z]+)?$'
    UNION
    SELECT aw.article_id,aw.word_id,LOWER(w.prototype),aw.create_by
    FROM article_words aw JOIN eng_word w ON w.id=aw.word_id
    WHERE w.prototype IS NOT NULL AND w.prototype!=''
      AND w.prototype REGEXP '^[A-Za-z]+(''[A-Za-z]+)?$'
),
`matched` AS (
    SELECT s.id sentence_id,c.word_id,c.matched_form,s.create_by,
           REGEXP_INSTR(s.content,
               CONCAT('(^|[^A-Za-z])',c.matched_form,'([^A-Za-z]|$)'),1,1,0,'i') match_position,
           REGEXP_REPLACE(
               REGEXP_SUBSTR(s.content,
                   CONCAT('(^|[^A-Za-z])',c.matched_form,'([^A-Za-z]|$)'),1,1,'i'),
               '^[^A-Za-z]+|[^A-Za-z]+$','') matched_text
    FROM eng_sentence s
    JOIN candidate_forms c ON c.article_id=s.article_id AND c.create_by=s.create_by
    WHERE REGEXP_LIKE(s.content,
        CONCAT('(^|[^A-Za-z])',c.matched_form,'([^A-Za-z]|$)'),'i')
),
`unambiguous` AS (
    SELECT sentence_id,matched_form
    FROM matched
    GROUP BY sentence_id,matched_form
    HAVING COUNT(DISTINCT word_id)=1
),
`confirmed` AS (
    SELECT m.*,
           ROW_NUMBER() OVER(PARTITION BY m.sentence_id,m.word_id
               ORDER BY m.match_position,m.matched_form) match_order
    FROM matched m
    JOIN unambiguous u ON u.sentence_id=m.sentence_id AND u.matched_form=m.matched_form
)
SELECT sentence_id,word_id,matched_text,create_by,NOW()
FROM confirmed
WHERE match_order=1 AND matched_text IS NOT NULL AND matched_text!='';
