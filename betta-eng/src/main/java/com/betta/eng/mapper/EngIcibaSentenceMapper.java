package com.betta.eng.mapper;

import com.betta.eng.domain.EngIcibaSentence;
import java.util.List;

/** 词典例句数据访问接口。 */
public interface EngIcibaSentenceMapper {
    /** 查询词典例句列表；sentence 为筛选条件，返回例句集合。 */
    List<EngIcibaSentence> selectEngIcibaSentenceList(EngIcibaSentence sentence);
    /** 新增词典例句；sentence 为待写入例句，返回影响行数。 */
    int insertEngIcibaSentence(EngIcibaSentence sentence);
    /** 删除指定单词的词典例句；wordId 为单词主键，返回影响行数。 */
    int deleteByWordId(Long wordId);
}
