package com.betta.eng.domain;

import com.betta.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 用户闯关学习记录实体，每次合法提交均保留一条历史记录。
 */
@Data
public class EngStudyRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;
    /** 学习记录主键。 */
    private Long id;
    /** 登录用户主键。 */
    private Long userId;
    /** 文章主键。 */
    private Long articleId;
    /** 客户端一次测试的幂等标识。 */
    private String attemptId;
    /** 学习模式：NEW、REVIEW、PRONUNCIATION 或 SPELLING。 */
    private String studyMode;
    /** 文章关卡号，全局复习与拼写测试时为空。 */
    private Integer levelNo;
    /** 本次得分。 */
    private Integer score;
    /** 正确题数。 */
    private Integer correctCount;
    /** 总题数。 */
    private Integer totalCount;
    /** 是否通关，零未通关、一已通关。 */
    private Integer passed;
    /** 本轮星级。 */
    private Integer stars;
    /** 本轮新词里程碑金币。 */
    private Long milestoneCoin;
    /** 本轮复习金币。 */
    private Long reviewCoin;
    /** 本轮获得金币，包含答对题数对应的基础金币和成绩档位奖励。 */
    private Long coinReward;
    /** 文章标题，仅用于联表查询展示。 */
    private String articleTitle;
}
