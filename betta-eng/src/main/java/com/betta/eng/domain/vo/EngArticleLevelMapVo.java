package com.betta.eng.domain.vo;

import java.util.List;
import lombok.Data;

/** 文章的新词关卡地图。 */
@Data
public class EngArticleLevelMapVo
{
    private Long articleId;
    private String title;
    private Integer totalLevels;
    private Integer completedLevels;
    /** 当前服务配置是否支持跟读评分。 */
    private Boolean pronunciationEnabled;
    private List<EngArticleLevelVo> levels;
}
