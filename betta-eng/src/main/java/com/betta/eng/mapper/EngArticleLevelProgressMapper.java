package com.betta.eng.mapper;

import com.betta.eng.domain.EngArticleLevelProgress;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 用户文章关卡进度数据访问。 */
public interface EngArticleLevelProgressMapper
{
    /** 查询用户指定文章关卡进度。 */
    EngArticleLevelProgress selectByUserArticleLevel(EngArticleLevelProgress progress);
    /** 查询用户在文章下的全部关卡进度。 */
    List<EngArticleLevelProgress> selectByUserAndArticle(@Param("userId") Long userId,
            @Param("articleId") Long articleId);
    /** 统计关卡是否已被任一用户开始，用于冻结关卡成员。 */
    int countAnyProgress(@Param("articleId") Long articleId, @Param("levelNo") Integer levelNo);
    /** 查询文章曾经产生进度的最高关卡号，防止删除后复用。 */
    Integer selectMaxLevelNoByArticle(Long articleId);
    /** 幂等刷新关卡最好成绩与最高星级。 */
    int upsertBest(EngArticleLevelProgress progress);
    /** 幂等标记全部单词已在其他文章掌握的关卡。 */
    int upsertKnown(EngArticleLevelProgress progress);
    /** 统计当前用户已完成文章数。 */
    long countCompletedArticles(@Param("userId") Long userId, @Param("username") String username);
}
