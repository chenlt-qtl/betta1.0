package com.betta.eng.service;
import com.betta.eng.domain.EngStudyRecordWord;
import com.betta.eng.domain.EngWrongWord;
import com.betta.eng.domain.dto.EngChallengeCheckDto;
import com.betta.eng.domain.dto.EngChallengeSubmitDto;
import com.betta.eng.domain.vo.EngChallengeResultVo;
import com.betta.eng.domain.vo.EngChallengeVo;
import com.betta.eng.domain.vo.EngStudySummaryVo;
import com.betta.eng.domain.vo.EngArticleLevelMapVo;
import com.betta.eng.domain.vo.EngReviewOverviewVo;
import java.util.List;
/** 游戏化学习业务接口，所有数据均按当前登录用户隔离。 */
public interface IEngStudyService {
    /** 查询当前用户学习统计并返回汇总。 */
    EngStudySummaryVo getSummary();
    /** 查询当前用户 articleId 的最好进度。 */
    EngArticleLevelMapVo getArticleLevels(Long articleId);
    EngReviewOverviewVo getReviewOverview();
    /** 按模式构建固定规范词集合的无答案挑战。 */
    EngChallengeVo getChallenge(String mode, Long articleId, Integer levelNo, List<Long> wordIds);
    /** 校验 request 中的单题答案并返回即时判定结果，不写入学习数据。 */
    EngChallengeResultVo.ResultItem checkChallengeAnswer(EngChallengeCheckDto request);
    /** 校验并提交 request，返回计分及逐题结果。 */
    EngChallengeResultVo submitChallenge(EngChallengeSubmitDto request);
    /** 按 wrongWord 条件查询当前用户错词。 */
    List<EngWrongWord> selectWrongWordList(EngWrongWord wrongWord);
    /** 将当前用户 id 对应错词标记掌握并返回影响行数。 */
    int markWrongWordMastered(Long id);
    /** 查询当前用户指定测试记录的规范词明细。 */
    List<EngStudyRecordWord> selectRecordWords(Long recordId);
}
