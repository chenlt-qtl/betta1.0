package com.betta.eng.service;
import com.betta.eng.domain.EngArticleWordRel;
import java.util.List;
/** 文章单词关系业务接口。 */
public interface IEngArticleWordRelService {
    /** 根据 id 查询并返回关系。 */
    EngArticleWordRel selectEngArticleWordRelById(Long id);
    /** 根据 rel 条件查询并返回关系列表。 */
    List<EngArticleWordRel> selectEngArticleWordRelList(EngArticleWordRel rel);
    /** 新增 rel 并返回影响行数。 */
    int insertEngArticleWordRel(EngArticleWordRel rel);
    /** 批量删除 ids 并返回影响行数。 */
    int deleteEngArticleWordRelByIds(Long[] ids);
    /** 删除 articleId 的全部关系并返回影响行数。 */
    int deleteByArticle(Long articleId);
}
