package com.betta.eng.mapper;

import com.betta.eng.domain.EngWord;
import com.betta.eng.domain.vo.EngWordVo;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 英语单词数据访问接口。 */
public interface EngWordMapper {
    /** 根据主键查询单词；id 为单词主键，返回单词详情。 */
    EngWord selectEngWordById(Long id);
    /** 查询单词列表；word 为筛选条件，返回单词集合。 */
    List<EngWordVo> selectEngWordList(EngWord word);
    /** 按规范化文本查询单词；wordName 为单词文本，返回匹配集合。 */
    List<EngWord> selectEngWordByWordName(String wordName);
    /** 查询文章关联单词；articleId 为文章主键，返回单词集合。 */
    List<EngWordVo> selectWordListByArticleId(Long articleId);
    /** 新增单词；word 为待写入实体，返回影响行数。 */
    int insertEngWord(EngWord word);
    /** 修改单词；word 为待更新实体，返回影响行数。 */
    int updateEngWord(EngWord word);
    /** 删除单词；id 为单词主键，返回影响行数。 */
    int deleteEngWordById(Long id);
    /** 查询当前用户关联的生词；word 为筛选条件、username 为登录名，返回单词集合。 */
    List<EngWord> selectRelList(@Param("word") EngWord word, @Param("username") String username);
}
