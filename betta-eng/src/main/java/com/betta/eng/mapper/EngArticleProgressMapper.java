package com.betta.eng.mapper;

import com.betta.eng.domain.EngArticleProgress;

/** 文章最好进度数据访问接口。 */
public interface EngArticleProgressMapper {
    /** 按用户和文章查询进度；progress 携带 userId/articleId，返回进度。 */
    EngArticleProgress selectByUserAndArticle(EngArticleProgress progress);
    /** 原子新增或择优更新进度；progress 为本次成绩，返回影响行数。 */
    int upsertBestProgress(EngArticleProgress progress);
    /** 统计用户已通关文章；userId 为登录用户主键，返回文章数。 */
    long countCompletedByUserId(Long userId);
}
