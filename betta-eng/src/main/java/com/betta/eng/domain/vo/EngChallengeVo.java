package com.betta.eng.domain.vo;

import com.betta.eng.domain.EngArticleProgress;
import java.util.List;
import lombok.Data;

/**
 * 文章闯关内容展示对象，包含文章元数据、历史进度和无答案题目。
 */
@Data
public class EngChallengeVo {
    /** 文章主键。 */
    private Long articleId;
    /** 文章标题。 */
    private String title;
    /** 历史最好进度；没有记录时返回零值对象。 */
    private EngArticleProgress progress;
    /** 本次挑战题目集合。 */
    private List<EngChallengeQuestionVo> questions;
}
