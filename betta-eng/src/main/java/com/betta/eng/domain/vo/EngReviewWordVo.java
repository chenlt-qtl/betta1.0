package com.betta.eng.domain.vo;

import java.util.Date;
import java.util.List;
import lombok.Data;

/** 全局复习词状态。 */
@Data
public class EngReviewWordVo
{
    private Long wordId;
    private String wordName;
    private String acceptation;
    private Integer highestStars;
    private Integer latestStars;
    private Integer currentStars;
    private Date latestTestTime;
    private Boolean recommended;
    /** 是否满足四字母随机挖空拼写测试条件。 */
    private Boolean spellingEligible;
    private List<String> sourceArticles;
    /** Mapper 聚合来源文章的内部逗号文本。 */
    private String sourceArticleNames;
}
