package com.betta.eng.domain.vo;

import com.betta.eng.domain.EngIcibaSentence;
import com.betta.eng.domain.EngWord;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import java.util.List;
import lombok.Data;

/**
 * 单词详情展示对象，聚合文章关系、词典例句和自定义例句。
 */
@Data
public class EngWordVo extends EngWord {
    private static final long serialVersionUID = 1L;
    /** 当前文章关系主键。 */
    private Long relId;
    /** 当前用户对该单词的有效熟悉度；无记录时为 0。 */
    private Integer familiarity;
    /** 数据库中的基础熟悉度，仅供服务端分类和增量更新。 */
    @JsonIgnore
    private Integer baseFamiliarity;
    /** 是否存在成绩记录，用于区分零熟悉度学习词和从未学习的新词。 */
    @JsonIgnore
    private Boolean scoreExists;
    /** 最后复习时间，优先取更新时间，否则取创建时间。 */
    @JsonIgnore
    private Date lastReviewTime;
    /** 本轮队列分类，仅供提交时登记每日完成记录。 */
    @JsonIgnore
    private String reviewCategory;
    /** 词典例句集合。 */
    private List<EngIcibaSentence> icibaSentenceList;
    /** 用户文章中的自定义例句集合。 */
    private List<SentenceVo> sentenceList;
}
