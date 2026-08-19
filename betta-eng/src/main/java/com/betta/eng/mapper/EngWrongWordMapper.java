package com.betta.eng.mapper;

import com.betta.eng.domain.EngWrongWord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 用户错词数据访问接口。 */
public interface EngWrongWordMapper {
    /** 查询用户错词列表；wrongWord 为用户隔离和筛选条件，返回错词集合。 */
    List<EngWrongWord> selectEngWrongWordList(EngWrongWord wrongWord);
    /** 按用户、文章和单词查询错词；wrongWord 携带组合键，返回记录。 */
    EngWrongWord selectByUserArticleWord(EngWrongWord wrongWord);
    /** 原子新增或累计错词；wrongWord 为本次错误，返回影响行数。 */
    int upsertWrongWord(EngWrongWord wrongWord);
    /** 更新错词累计和掌握状态；wrongWord 为待更新实体，返回影响行数。 */
    int updateEngWrongWord(EngWrongWord wrongWord);
    /** 按用户、文章和单词原子标记已掌握；组合主键定位错词且不更新错误次数，返回影响行数。 */
    int markMasteredByUserArticleWord(@Param("userId") Long userId, @Param("articleId") Long articleId,
            @Param("wordId") Long wordId, @Param("updateBy") String updateBy);
    /** 统计用户指定掌握状态错词；userId 为用户、mastered 为状态，返回数量。 */
    long countByUserAndMastered(@Param("userId") Long userId, @Param("mastered") int mastered);
    /** 将归属于当前用户的错词标记掌握；id 为错词主键、userId 为用户，返回影响行数。 */
    int markMastered(@Param("id") Long id, @Param("userId") Long userId, @Param("updateBy") String updateBy);
}
