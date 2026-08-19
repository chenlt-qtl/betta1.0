package com.betta.eng.mapper;

import com.betta.eng.domain.EngArticleGroup;
import java.util.List;

/** 英语文章分组数据访问接口。 */
public interface EngArticleGroupMapper {
    /** 根据主键查询分组；id 为分组主键，返回分组详情。 */
    EngArticleGroup selectEngArticleGroupById(Long id);
    /** 查询分组列表；group 为筛选条件，返回分组集合。 */
    List<EngArticleGroup> selectEngArticleGroupList(EngArticleGroup group);
    /** 新增分组；group 为待写入实体，返回影响行数。 */
    int insertEngArticleGroup(EngArticleGroup group);
    /** 修改分组；group 为待更新实体，返回影响行数。 */
    int updateEngArticleGroup(EngArticleGroup group);
    /** 批量删除分组；ids 为主键数组，返回影响行数。 */
    int deleteEngArticleGroupByIds(Long[] ids);
    /** 统计分组下文章数；id 为分组主键，返回文章数量。 */
    int countArticleByGroupId(Long id);
}
