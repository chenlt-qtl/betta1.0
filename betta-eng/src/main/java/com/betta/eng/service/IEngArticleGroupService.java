package com.betta.eng.service;
import com.betta.eng.domain.EngArticleGroup;
import java.util.List;
/** 文章分组业务接口。 */
public interface IEngArticleGroupService {
    /** 根据 id 查询并返回分组。 */
    EngArticleGroup selectEngArticleGroupById(Long id);
    /** 根据 group 条件查询并返回分组列表。 */
    List<EngArticleGroup> selectEngArticleGroupList(EngArticleGroup group);
    /** 新增 group 并返回影响行数。 */
    int insertEngArticleGroup(EngArticleGroup group);
    /** 更新 group 并返回影响行数。 */
    int updateEngArticleGroup(EngArticleGroup group);
    /** 删除 ids 对应空分组并返回影响行数。 */
    int deleteEngArticleGroupByIds(Long[] ids);
}
