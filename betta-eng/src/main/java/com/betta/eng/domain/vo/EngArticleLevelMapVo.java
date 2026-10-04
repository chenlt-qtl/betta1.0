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
    private List<EngArticleLevelVo> levels;
}
