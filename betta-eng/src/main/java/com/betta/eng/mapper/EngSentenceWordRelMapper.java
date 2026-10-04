package com.betta.eng.mapper;

import com.betta.eng.domain.EngSentenceWordRel;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 句子与规范词关系数据访问接口。 */
public interface EngSentenceWordRelMapper
{
    /** 查询当前用户某句子的全部关系。 */
    List<EngSentenceWordRel> selectBySentenceId(@Param("sentenceId") Long sentenceId,
            @Param("username") String username);

    /** 查询当前用户文章内某规范词的首条有效句子关系。 */
    EngSentenceWordRel selectFirstByArticleAndWord(@Param("articleId") Long articleId,
            @Param("wordId") Long wordId, @Param("username") String username);
    /** 跨当前用户全部文章查询规范词首条有效句子。 */
    EngSentenceWordRel selectFirstByWord(@Param("wordId") Long wordId, @Param("username") String username);

    /** 统计文章内仍引用规范词的句子数。 */
    int countByArticleAndWord(@Param("articleId") Long articleId, @Param("wordId") Long wordId,
            @Param("username") String username);

    /** 新增关系。 */
    int insert(EngSentenceWordRel relation);

    /** 更新句中实际词形。 */
    int updateMatchedText(EngSentenceWordRel relation);

    /** 批量新增关系或更新已有关系的实际词形。 */
    int upsertBatch(@Param("relations") List<EngSentenceWordRel> relations);

    /** 删除指定句子和规范词的关系。 */
    int deleteBySentenceAndWord(@Param("sentenceId") Long sentenceId, @Param("wordId") Long wordId,
            @Param("username") String username);

    /** 批量删除指定句子的规范词关系。 */
    int deleteBySentenceAndWordIds(@Param("sentenceId") Long sentenceId,
            @Param("wordIds") List<Long> wordIds, @Param("username") String username);

    /** 删除当前用户指定句子的全部关系。 */
    int deleteBySentenceIds(@Param("sentenceIds") Long[] sentenceIds, @Param("username") String username);

    /** 删除当前用户文章内指定规范词的全部句子关系。 */
    int deleteByArticleAndWordNames(@Param("articleId") Long articleId,
            @Param("wordNames") List<String> wordNames, @Param("username") String username);

    /** 删除当前用户文章内全部句子关系。 */
    int deleteByArticle(@Param("articleId") Long articleId, @Param("username") String username);
}
