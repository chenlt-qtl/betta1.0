package com.betta.eng.mapper;

import com.betta.eng.domain.EngWord;
import com.betta.eng.domain.vo.EngWordFormMatchVo;
import com.betta.eng.domain.vo.EngWordVo;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 英语单词数据访问接口。 */
public interface EngWordMapper {
    /** 根据主键查询单词；id 为单词主键，返回单词详情。 */
    EngWord selectEngWordById(Long id);
    /** 按主键集合批量查询单词。 */
    List<EngWord> selectByIds(@Param("ids") List<Long> ids);
    /** 查询单词列表；word 为筛选条件，返回单词集合。 */
    List<EngWordVo> selectEngWordList(EngWord word);
    /** 按规范化文本查询单词；wordName 为单词文本，返回匹配集合。 */
    List<EngWord> selectEngWordByWordName(String wordName);
    /** 批量解析规范词、别名和历史原型；forms 为去重后的规范化词形。 */
    List<EngWordFormMatchVo> selectWordFormMatches(@Param("forms") List<String> forms);
    /** 查询文章关联单词及当前用户熟悉度；articleId 为文章主键、username 为登录名，返回单词集合。 */
    List<EngWordVo> selectWordListByArticleId(@Param("articleId") Long articleId,
            @Param("username") String username);
    /** 新增单词；word 为待写入实体，返回影响行数。 */
    int insertEngWord(EngWord word);
    /** 修改单词；word 为待更新实体，返回影响行数。 */
    int updateEngWord(EngWord word);
    /** 删除单词；id 为单词主键，返回影响行数。 */
    int deleteEngWordById(Long id);
    /** 统计规范词被历史测试明细引用的次数。 */
    int countStudyRecordWordRefs(Long id);
    /** 查询当前用户关联的生词；word 为筛选条件、username 为登录名，返回单词集合。 */
    List<EngWord> selectRelList(@Param("word") EngWord word, @Param("username") String username);
}
