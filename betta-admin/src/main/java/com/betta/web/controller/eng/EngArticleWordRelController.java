package com.betta.web.controller.eng;

import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.betta.common.annotation.Log;
import com.betta.common.core.controller.BaseController;
import com.betta.common.core.domain.AjaxResult;
import com.betta.common.core.page.TableDataInfo;
import com.betta.common.enums.BusinessType;
import com.betta.eng.domain.EngArticleWordRel;
import com.betta.eng.service.IEngArticleWordRelService;

/** 文章单词关系控制器，只负责参数接收和服务调用。 */
@RestController
@RequestMapping("/eng/articleWordRel")
public class EngArticleWordRelController extends BaseController
{
    private final IEngArticleWordRelService service;

    /** 创建关系控制器；service 负责文章单词关系业务。 */
    public EngArticleWordRelController(IEngArticleWordRelService service)
    {
        this.service = service;
    }

    /** 分页查询 rel 条件对应关系。 */
    @GetMapping("/list")
    public TableDataInfo list(EngArticleWordRel rel)
    {
        startPage();
        List<EngArticleWordRel> list = service.selectEngArticleWordRelList(rel);
        return getDataTable(list);
    }

    /** 查询 id 对应关系。 */
    @GetMapping("/{id}")
    public AjaxResult info(@PathVariable Long id)
    {
        return success(service.selectEngArticleWordRelById(id));
    }

    /** 新增 rel。 */
    @Log(title = "文章单词关系", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody EngArticleWordRel rel)
    {
        return toAjax(service.insertEngArticleWordRel(rel));
    }

    /** 删除 ids 对应关系。 */
    @Log(title = "文章单词关系", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(service.deleteEngArticleWordRelByIds(ids));
    }
}
