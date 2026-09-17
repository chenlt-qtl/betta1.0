-- 牛津高阶英汉双解词典第 10 版：复用现有词条与例句表，并增加别名关系。

ALTER TABLE `eng_word`
    MODIFY COLUMN `acceptation` LONGTEXT NULL COMMENT '解释',
    ADD COLUMN `display_name` VARCHAR(200) NULL COMMENT '词典展示词头' AFTER `word_name`,
    ADD COLUMN `phonetics_uk` VARCHAR(200) NULL COMMENT '英式音标' AFTER `phonetics`,
    ADD COLUMN `phonetics_us` VARCHAR(200) NULL COMMENT '美式音标' AFTER `phonetics_uk`,
    ADD COLUMN `ph_mp3_uk` VARCHAR(1000) NULL COMMENT '英式发音音频位置' AFTER `ph_mp3`,
    ADD COLUMN `ph_mp3_us` VARCHAR(1000) NULL COMMENT '美式发音音频位置' AFTER `ph_mp3_uk`,
    ADD COLUMN `word_forms` TEXT NULL COMMENT '词形变化' AFTER `parts`,
    ADD COLUMN `usage_labels` TEXT NULL COMMENT '使用标签' AFTER `word_forms`,
    ADD COLUMN `variants` TEXT NULL COMMENT '拼写或表达变体' AFTER `usage_labels`,
    ADD COLUMN `grammar` TEXT NULL COMMENT '语法标签' AFTER `variants`,
    ADD COLUMN `dictionary_source` VARCHAR(32) NULL COMMENT '词典来源' AFTER `grammar`,
    ADD COLUMN `dictionary_detail` JSON NULL COMMENT '结构化词典义项' AFTER `dictionary_source`,
    ADD KEY `idx_eng_word_dictionary_source` (`dictionary_source`);

ALTER TABLE `eng_iciba_sentence`
    ADD COLUMN `source` VARCHAR(32) NULL COMMENT '例句来源' AFTER `trans`,
    ADD COLUMN `sense_id` VARCHAR(255) NULL COMMENT '原始词典义项ID' AFTER `source`,
    ADD COLUMN `sort_order` INT NULL COMMENT '词条内排序号' AFTER `sense_id`,
    ADD COLUMN `audio_path` VARCHAR(1000) NULL COMMENT '例句朗读音频位置' AFTER `sort_order`,
    ADD KEY `idx_eng_iciba_sentence_word_source` (`word_id`, `source`);

CREATE TABLE `eng_word_alias` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `alias_key` VARCHAR(200) COLLATE utf8mb4_bin NOT NULL COMMENT '规范化别名检索键',
    `raw_alias_key` VARCHAR(200) NOT NULL COMMENT '源数据原始别名',
    `raw_target_key` VARCHAR(200) NOT NULL COMMENT '源数据原始跳转目标',
    `word_id` BIGINT NOT NULL COMMENT '最终正式词条ID',
    `source` VARCHAR(32) NOT NULL COMMENT '词典来源',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '同一别名目标顺序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eng_word_alias_source_key_word` (`source`, `alias_key`, `word_id`),
    KEY `idx_eng_word_alias_alias_key` (`alias_key`),
    KEY `idx_eng_word_alias_word_id` (`word_id`),
    CONSTRAINT `fk_eng_word_alias_word_id` FOREIGN KEY (`word_id`) REFERENCES `eng_word` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='单词别名关系表';
