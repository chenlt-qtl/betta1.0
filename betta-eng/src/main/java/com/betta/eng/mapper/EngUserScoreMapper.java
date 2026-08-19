package com.betta.eng.mapper;

import com.betta.eng.domain.EngUserScore;
import com.betta.eng.domain.vo.EngUserScoreVo;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 用户单词熟悉度数据访问接口。 */
public interface EngUserScoreMapper {
    /** 查询成绩列表；score 为筛选条件，返回成绩集合。 */
    List<EngUserScore> selectEngUserScoreList(EngUserScore score);
    /** 查询带单词信息的成绩；score 为筛选条件，username 为登录名，返回展示集合。 */
    List<EngUserScoreVo> selectEngUserScoreVo(@Param("score") EngUserScore score, @Param("username") String username);
    /** 按用户与单词查询成绩；wordName 为单词、user 为用户，返回成绩。 */
    EngUserScore getByWordName(@Param("wordName") String wordName, @Param("user") String user);
    /** 新增成绩；score 为待写入实体，返回影响行数。 */
    int insertEngUserScore(EngUserScore score);
    /** 修改成绩；score 为待更新实体，返回影响行数。 */
    int updateEngUserScore(EngUserScore score);
    /** 批量删除当前用户成绩；ids 为主键数组、user 为用户名，返回影响行数。 */
    int deleteEngUserScoreByIds(@Param("ids") Long[] ids, @Param("user") String user);
}
