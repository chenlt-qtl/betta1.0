package com.betta.eng.service;
import com.betta.eng.domain.EngWord;
import com.betta.eng.domain.vo.EngWordVo;
import java.util.List;
/** 英语单词业务接口。 */
public interface IEngWordService {
    /** 根据 id 查询并返回单词。 */
    EngWord selectEngWordById(Long id);
    /** 根据 word 条件查询并返回单词列表。 */
    List<EngWordVo> selectEngWordList(EngWord word);
    /** 查询 articleId 关联单词。 */
    List<EngWordVo> selectWordListByArticle(Long articleId);
    /** 新增 word 并返回影响行数。 */
    int addEngWord(EngWord word);
    /** 查询 wordName 的聚合详情。 */
    EngWordVo getWordVo(String wordName);
    /** 从外部词典查询 wordName 并返回临时结果，不直接写库。 */
    EngWord getWordFromApi(String wordName);
    /** 更新 word 并返回影响行数。 */
    int updateEngWord(EngWord word);
    /** 删除 ids 对应单词。 */
    void deleteEngWordByIds(Long[] ids);
    /** 查询当前用户的生词列表。 */
    List<EngWord> selectNewList(EngWord word);
    /** 用 words 全量更新 articleId 的关联单词。 */
    void updateByArticle(List<String> words, Long articleId);
    /** 为 articleId 新增 wordName 关系。 */
    void addArticleWord(Long articleId, String wordName);
}
