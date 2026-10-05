-- 单词跟读评测只持久化最终整数分和是否合格，不保存原始音频及音素明细。
alter table eng_study_record_word
    add column pronunciation_score int null comment '跟读最终整数分' after stars,
    add column pronunciation_passed tinyint null comment '跟读是否合格：0否，1是' after pronunciation_score;
