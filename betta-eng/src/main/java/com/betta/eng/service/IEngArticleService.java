package com.betta.eng.service;
import com.betta.eng.domain.EngArticle;
import java.util.List;
/** 英语文章业务接口，定义文章维护与播放查询能力。 */
public interface IEngArticleService {
    /** 根据 id 查询文章并返回详情。 */
    EngArticle selectEngArticleById(Long id);
    /** 根据 article 条件查询并返回文章列表。 */
    List<EngArticle> selectEngArticleList(EngArticle article);
    /** 新增 article 并返回含主键实体。 */
    EngArticle insertEngArticle(EngArticle article);
    /** 更新 article 并返回影响行数。 */
    int updateEngArticle(EngArticle article);
    /** 删除 id 对应文章及其句子、单词关系，返回影响行数。 */
    int deleteEngArticleById(Long id);
    /** 按 article 条件、inPlayList 包含关系和 username 查询播放文章。 */
    List<EngArticle> selectPlayList(EngArticle article, boolean inPlayList, String username);
    /** 查询当前用户正在学习的文章并返回。 */
    EngArticle getCurrent();
    /** 聚合 articleId 的句子和单词文本并返回。 */
    List<String> exportArticle(Long articleId);
}
