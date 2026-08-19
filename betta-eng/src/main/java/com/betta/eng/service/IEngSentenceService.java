package com.betta.eng.service;
import com.betta.eng.domain.EngSentence;
import com.betta.eng.domain.EngWord;
import com.betta.eng.domain.dojo.BatchAddSentences;
import com.betta.eng.domain.vo.SentenceVo;
import java.util.List;
/** 文章句子业务接口。 */
public interface IEngSentenceService {
    /** 根据 id 查询并返回句子。 */
    EngSentence selectEngSentenceById(Long id);
    /** 根据 sentence 条件查询并返回句子列表。 */
    List<EngSentence> selectEngSentenceList(EngSentence sentence);
    /** 新增 sentence 并返回影响行数。 */
    int insertEngSentence(EngSentence sentence);
    /** 更新 sentence 并返回影响行数。 */
    int updateEngSentence(EngSentence sentence);
    /** 批量删除 ids 并返回影响行数。 */
    int deleteEngSentenceByIds(Long[] ids);
    /** 删除 articleId 对应句子并返回影响行数。 */
    int deleteByArticle(Long articleId);
    /** 根据 sentence、播放关系和 username 查询句子。 */
    List<SentenceVo> selectPlayList(EngSentence sentence, boolean inPlayList, String username);
    /** 查询包含 word 的最多十个句子。 */
    List<SentenceVo> selectByWordTop10(EngWord word);
    /** 批量写入 request 中的多行句子并返回是否成功。 */
    boolean insertEngSentenceBatch(BatchAddSentences request);
}
