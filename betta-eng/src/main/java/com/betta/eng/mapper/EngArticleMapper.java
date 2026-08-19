package com.betta.eng.mapper;

import com.betta.eng.domain.EngArticle;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 英语文章数据访问接口，只负责文章表的查询和写入。
 */
public interface EngArticleMapper {
    /** 根据主键查询文章；id 为文章主键，返回文章详情。 */
    EngArticle selectEngArticleById(@Param("id") Long id, @Param("username") String username);
    /** 按筛选条件查询文章；article 为筛选条件，返回文章集合。 */
    List<EngArticle> selectEngArticleList(EngArticle article);
    /** 新增文章；article 为待写入实体，返回影响行数。 */
    int insertEngArticle(EngArticle article);
    /** 修改文章；article 为待更新实体，返回影响行数。 */
    int updateEngArticle(EngArticle article);
    /** 删除文章；id 为文章主键，返回影响行数。 */
    int deleteEngArticleById(@Param("id") Long id, @Param("username") String username);
    /** 查询指定用户播放候选文章；article 为筛选条件，idList 为包含或排除的文章主键，include 控制包含关系，返回文章集合。 */
    List<EngArticle> selectPlayList(@Param("article") EngArticle article, @Param("username") String username,
            @Param("idList") List<Long> idList, @Param("include") boolean include);
    /** 查询当前学习文章；username 为用户名、dataType 为数据类型，返回文章。 */
    EngArticle getCurrentArticle(@Param("username") String username, @Param("dataType") int dataType);
}
