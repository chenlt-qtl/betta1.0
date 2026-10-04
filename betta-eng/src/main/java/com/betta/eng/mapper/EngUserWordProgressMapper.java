package com.betta.eng.mapper;

import com.betta.eng.domain.EngUserWordProgress;
import com.betta.eng.domain.vo.EngReviewWordVo;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 用户规范词全局进度数据访问。 */
public interface EngUserWordProgressMapper
{
    /** 幂等创建用户规范词进度占位。 */
    int ensureProgress(EngUserWordProgress progress);
    /** 锁定并查询单词进度。 */
    EngUserWordProgress selectForUpdate(@Param("userId") Long userId, @Param("wordId") Long wordId);
    /** 查询用户的单个规范词进度。 */
    EngUserWordProgress selectByUserAndWord(@Param("userId") Long userId, @Param("wordId") Long wordId);
    /** 更新已锁定的单词进度。 */
    int updateProgress(EngUserWordProgress progress);
    /** 批量查询已学规范词主键。 */
    List<Long> selectLearnedWordIds(@Param("userId") Long userId, @Param("wordIds") List<Long> wordIds);
    /** 查询用户全部可复习单词。 */
    List<EngReviewWordVo> selectReviewWords(Long userId);
    /** 统计用户已学规范词数。 */
    long countLearned(Long userId);
}
