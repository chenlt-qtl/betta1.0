package com.betta.eng.domain;

import com.betta.common.annotation.Excel;
import com.betta.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 英语单词实体，保存词形、音标、音频和释义。
 */
@Data
public class EngWord extends BaseEntity {
    private static final long serialVersionUID = 1L;
    /** 单词主键。 */
    private Long id;
    /** 单词文本。 */
    @Excel(name = "单词内容")
    private String wordName;
    /** 原型词。 */
    private String prototype;
    /** 发音音频地址。 */
    private String phMp3;
    /** 音标。 */
    private String phonetics;
    /** 词典展示词头。 */
    private String displayName;
    /** 英式音标。 */
    private String phoneticsUk;
    /** 美式音标。 */
    private String phoneticsUs;
    /** 英式发音音频地址。 */
    private String phMp3Uk;
    /** 美式发音音频地址。 */
    private String phMp3Us;
    /** 中文释义。 */
    private String acceptation;
    /** 词形变化或人工备注。 */
    private String exchange;
    /** 词性信息。 */
    private String parts;
    /** 词形变化。 */
    private String wordForms;
    /** 使用标签。 */
    private String usageLabels;
    /** 拼写或表达变体。 */
    private String variants;
    /** 语法标签。 */
    private String grammar;
    /** 词典数据来源。 */
    private String dictionarySource;
    /** 结构化词典义项 JSON。 */
    private String dictionaryDetail;
    /** 单词状态。 */
    private String status;
}
