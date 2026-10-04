package com.betta.eng.mapper;

import com.betta.eng.domain.EngArticleWordRel;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 文章单词关系数据访问接口。 */
public interface EngArticleWordRelMapper {
    /** 根据主键查询关系；id 为关系主键，返回关系详情。 */
    EngArticleWordRel selectEngArticleWordRelById(@Param("id") Long id, @Param("username") String username);
    /** 查询关系列表；rel 为筛选条件，返回关系集合。 */
    List<EngArticleWordRel> selectEngArticleWordRelList(EngArticleWordRel rel);
    /** 新增关系；rel 为待写入关系，返回影响行数。 */
    int insertEngArticleWordRel(EngArticleWordRel rel);
    /** 批量新增已分配永久关卡号的文章词关系。 */
    int insertBatch(@Param("relations") List<EngArticleWordRel> relations);
    /** 批量查询文章已存在的规范词名。 */
    List<String> selectExistingWordNames(@Param("articleId") Long articleId,
            @Param("wordNames") List<String> wordNames, @Param("username") String username);
    /** 批量删除文章中已无句子引用的规范词关系。 */
    int deleteUnusedByWordIds(@Param("articleId") Long articleId,
            @Param("wordIds") List<Long> wordIds, @Param("username") String username);
    /** 批量删除关系；ids 为关系主键数组，返回影响行数。 */
    int deleteEngArticleWordRelByIds(@Param("ids") Long[] ids, @Param("username") String username);
    /** 删除文章全部关系；articleId 为文章主键，返回影响行数。 */
    int deleteByArticleId(@Param("articleId") Long articleId, @Param("username") String username);
    /** 锁定当前用户文章，串行化关卡分配。 */
    Long lockArticle(@Param("articleId") Long articleId, @Param("username") String username);
    Integer selectMaxLevelNo(Long articleId);
    int countByArticleAndLevel(@Param("articleId") Long articleId, @Param("levelNo") Integer levelNo);
    List<EngArticleWordRel> selectByArticleAndLevel(@Param("articleId") Long articleId,
            @Param("levelNo") Integer levelNo, @Param("username") String username);
}
