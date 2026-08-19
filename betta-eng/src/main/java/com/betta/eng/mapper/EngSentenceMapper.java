package com.betta.eng.mapper;

import com.betta.eng.domain.EngSentence;
import com.betta.eng.domain.vo.SentenceVo;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 文章句子数据访问接口。 */
public interface EngSentenceMapper {
    /** 根据主键查询句子；id 为句子主键，返回句子详情。 */
    EngSentence selectEngSentenceById(@Param("id") Long id, @Param("username") String username);
    /** 查询句子列表；sentence 为筛选条件，返回句子集合。 */
    List<EngSentence> selectEngSentenceList(EngSentence sentence);
    /** 新增句子；sentence 为待写入实体，返回影响行数。 */
    int insertEngSentence(EngSentence sentence);
    /** 修改句子；sentence 为待更新实体，返回影响行数。 */
    int updateEngSentence(EngSentence sentence);
    /** 批量删除句子；ids 为句子主键数组，返回影响行数。 */
    int deleteEngSentenceByIds(@Param("ids") Long[] ids, @Param("username") String username);
    /** 删除文章全部句子；articleId 为文章主键，返回影响行数。 */
    int deleteByArticleId(@Param("articleId") Long articleId, @Param("username") String username);
    /** 统计文章句子数；articleId 为文章主键，返回句子数量。 */
    long countByArticleId(Long articleId);
    /** 查询播放列表句子；sentence 为筛选条件，idList 为包含或排除主键，include 控制包含关系，返回句子集合。 */
    List<SentenceVo> selectPlayList(@Param("sentence") EngSentence sentence, @Param("username") String username,
            @Param("idList") List<Long> idList, @Param("include") boolean include);
    /** 查询包含单词的最多十个句子；wordName 与 prototype 为匹配词形，username 为用户，返回展示集合。 */
    List<SentenceVo> selectByWordTop10(@Param("wordName") String wordName, @Param("prototype") String prototype,
            @Param("username") String username);
}
