package com.betta.eng.service;
import com.betta.eng.domain.EngStudyRecordWord;
import com.betta.eng.domain.EngWrongWord;
import com.betta.eng.domain.dto.EngChallengeCheckDto;
import com.betta.eng.domain.dto.EngChallengeSubmitDto;
import com.betta.eng.domain.dto.EngPronunciationAssessDto;
import com.betta.eng.domain.vo.EngPronunciationAssessmentVo;
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
    /** 按 NEW、REVIEW、PRONUNCIATION 或 SPELLING 模式构建固定词集挑战。 */
    EngChallengeVo getChallenge(String mode, Long articleId, Integer levelNo, List<Long> wordIds);
    /** 按当前访问环境是否允许跟读，构建不含明文答案的固定词集挑战。 */
    EngChallengeVo getChallenge(String mode, Long articleId, Integer levelNo, List<Long> wordIds,
            boolean pronunciationAllowed);
    /** 兼容无法在前端校验摘要的环境，校验单题答案且不写入学习数据。 */
    EngChallengeResultVo.ResultItem checkChallengeAnswer(EngChallengeCheckDto request);
    /** 校验并评测一条跟读录音，可信结果由服务端缓存。 */
    EngPronunciationAssessmentVo assessPronunciation(EngPronunciationAssessDto request);
    /** 校验并提交 request，返回计分及逐题结果。 */
    EngChallengeResultVo submitChallenge(EngChallengeSubmitDto request);
    /** 按当前访问环境是否允许跟读，校验并提交 request。 */
    EngChallengeResultVo submitChallenge(EngChallengeSubmitDto request, boolean pronunciationAllowed);
    /** 按 wrongWord 条件查询当前用户错词。 */
    List<EngWrongWord> selectWrongWordList(EngWrongWord wrongWord);
    /** 将当前用户 id 对应错词标记掌握并返回影响行数。 */
    int markWrongWordMastered(Long id);
    /** 查询当前用户指定测试记录的规范词明细。 */
    List<EngStudyRecordWord> selectRecordWords(Long recordId);
}
