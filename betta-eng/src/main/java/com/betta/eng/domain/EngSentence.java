package com.betta.eng.domain;

import com.betta.common.annotation.Excel;
import com.betta.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 英语文章句子实体，支持文章详情、跟读和播放列表。
 */
@Data
public class EngSentence extends BaseEntity {
    private static final long serialVersionUID = 1L;
    /** 句子主键。 */
    private Long id;
    /** 所属文章主键。 */
    private Long articleId;
    /** 英文内容。 */
    @Excel(name = "句子内容")
    private String content;
    /** 中文释义。 */
    @Excel(name = "解释")
    private String acceptation;
    /** 文章内顺序。 */
    private Long idx;
    /** 句子图片。 */
    private String picture;
    /** 句子音频。 */
    private String mp3;
    /** 音频起点与时长，格式为“秒数,时长”。 */
    private String mp3Time;
    /** 句子状态。 */
    private String status;
}
