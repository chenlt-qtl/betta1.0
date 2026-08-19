package com.betta.eng.service;
import com.betta.eng.domain.EngUserScore;
import com.betta.eng.domain.vo.EngUserScoreVo;
import java.util.List;
/** 用户单词成绩业务接口。 */
public interface IEngUserScoreService {
    /** 根据 score 条件查询当前用户成绩。 */
    List<EngUserScore> selectEngUserScoreList(EngUserScore score);
    /** 根据 score 条件查询当前用户单词明细。 */
    List<EngUserScoreVo> selectEngUserScoreVoList(EngUserScore score);
    /** 查询当前用户单词明细，并按 withSentence 决定是否补充例句。 */
    List<EngUserScoreVo> selectEngUserScoreVoList(EngUserScore score, boolean withSentence);
    /** 新增 score 并返回影响行数。 */
    int insertEngUserScore(EngUserScore score);
    /** 更新 score 并返回影响行数。 */
    int updateEngUserScore(EngUserScore score);
    /** 将 scores 作为增量批量更新当前用户成绩。 */
    void batchUpdate(List<EngUserScore> scores);
    /** 删除当前用户 ids 对应成绩并返回影响行数。 */
    int deleteEngUserScoreByIds(Long[] ids);
    /** 为 wordName 增加 delta 熟悉度。 */
    void updateEngUserScore(String wordName, int delta);
}
