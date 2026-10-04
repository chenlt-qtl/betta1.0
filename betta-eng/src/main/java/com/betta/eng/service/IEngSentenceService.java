package com.betta.eng.service;
import com.betta.eng.domain.EngSentence;
import com.betta.eng.domain.EngSentenceWordRel;
import com.betta.eng.domain.EngWord;
import com.betta.eng.domain.dojo.BatchAddSentences;
import com.betta.eng.domain.dto.EngSentenceWordUpdateDto;
import com.betta.eng.domain.vo.EngSentenceWordOptionsVo;
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
    /** 查询当前用户句子的可选规范词及已选关系。 */
    EngSentenceWordOptionsVo selectWordOptions(Long sentenceId);
    /** 使用 request 中的规范词集合替换当前句子的关系。 */
    EngSentenceWordOptionsVo updateWordOptions(Long sentenceId, EngSentenceWordUpdateDto request);
    /** 查询当前文章内明确关联规范词的首个有效句子。 */
    EngSentenceWordRel selectFirstWordRelation(Long articleId, Long wordId);
    /** 跨当前用户全部文章查询规范词首条有效句子。 */
    EngSentenceWordRel selectFirstWordRelation(Long wordId);
    /** 批量写入 request 中的多行句子并返回是否成功。 */
    boolean insertEngSentenceBatch(BatchAddSentences request);
}
