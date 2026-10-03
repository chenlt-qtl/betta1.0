package com.betta.eng.mapper;

import com.betta.eng.domain.EngDailyTestWord;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 每日测试单词数据访问接口。 */
public interface EngDailyTestWordMapper {
    /** 查询用户指定日期全站已完成的单词主键。 */
    List<Long> selectCompletedWordIds(@Param("userId") Long userId, @Param("studyDate") LocalDate studyDate);

    /** 统计用户指定文章和日期已完成的新词数。 */
    int countNewWords(@Param("userId") Long userId, @Param("articleId") Long articleId,
            @Param("studyDate") LocalDate studyDate);

    /** 登记一个待提交的每日测试单词，唯一约束负责并发去重。 */
    int insertDailyTestWord(EngDailyTestWord dailyTestWord);

    /** 将本轮占位记录关联到最终学习记录。 */
    int bindStudyRecord(@Param("userId") Long userId, @Param("articleId") Long articleId,
            @Param("studyDate") LocalDate studyDate, @Param("wordIds") List<Long> wordIds,
            @Param("studyRecordId") Long studyRecordId);
}
