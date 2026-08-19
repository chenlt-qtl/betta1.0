package com.betta.eng.service;
import com.betta.eng.domain.EngIcibaSentence;
import java.util.List;
/** 词典例句业务接口。 */
public interface IEngIcibaSentenceService {
    /** 根据 sentence 条件查询并返回例句列表。 */
    List<EngIcibaSentence> selectEngIcibaSentenceList(EngIcibaSentence sentence);
    /** 新增 sentence 并返回影响行数。 */
    int insertEngIcibaSentence(EngIcibaSentence sentence);
    /** 删除 wordId 对应例句。 */
    void deleteByWordId(Long wordId);
}
