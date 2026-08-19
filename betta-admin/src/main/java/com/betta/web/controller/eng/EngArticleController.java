package com.betta.web.controller.eng;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.betta.common.annotation.Log;
import com.betta.common.core.controller.BaseController;
import com.betta.common.core.domain.AjaxResult;
import com.betta.common.core.page.TableDataInfo;
import com.betta.common.enums.BusinessType;
import com.betta.common.utils.poi.ExcelUtil;
import com.betta.eng.domain.EngArticle;
import com.betta.eng.service.IEngArticleService;

/** 英语文章控制器，只负责接参、分页、服务调用与响应。 */
@RestController
@RequestMapping("/eng/article")
public class EngArticleController extends BaseController
{
    private final IEngArticleService articleService;

    /** 创建文章控制器；三个参数分别负责文章、句子和单词业务。 */
    public EngArticleController(IEngArticleService articleService)
    {
        this.articleService = articleService;
    }

    /** 分页查询 article 条件对应文章。 */
    @PreAuthorize("@ss.hasPermi('eng:article:list')")
    @GetMapping("/list")
    public TableDataInfo list(EngArticle article)
    {
        startPage();
        List<EngArticle> list = articleService.selectEngArticleList(article);
        return getDataTable(list);
    }

    /** 将 article 条件对应文章导出为 Excel 响应。 */
    @PostMapping("/export")
    public void exportList(HttpServletResponse response, EngArticle article)
    {
        new ExcelUtil<>(EngArticle.class).exportExcel(response, articleService.selectEngArticleList(article), "英语文章数据");
    }

    /** 查询 id 对应文章。 */
    @PreAuthorize("@ss.hasPermi('eng:article:query')")
    @GetMapping("/{id}")
    public AjaxResult info(@PathVariable Long id)
    {
        return success(articleService.selectEngArticleById(id));
    }

    /** 新增 article 并返回含主键实体。 */
    @PreAuthorize("@ss.hasPermi('eng:article:add')")
    @Log(title = "英语文章", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody EngArticle article)
    {
        return success(articleService.insertEngArticle(article));
    }

    /** 更新 article。 */
    @PreAuthorize("@ss.hasPermi('eng:article:edit')")
    @Log(title = "英语文章", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody EngArticle article)
    {
        return toAjax(articleService.updateEngArticle(article));
    }

    /** 删除 id 对应文章。 */
    @PreAuthorize("@ss.hasPermi('eng:article:remove')")
    @Log(title = "英语文章", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        return toAjax(articleService.deleteEngArticleById(id));
    }

    /** 按 article、inPlayList 和 username 查询播放文章。 */
    @GetMapping("/list/play")
    public TableDataInfo play(EngArticle article, boolean inPlayList, String username)
    {
        startPage();
        return getDataTable(articleService.selectPlayList(article, inPlayList, username));
    }

    /** 导出 id 对应文章的句子和单词纯文本数组。 */
    @GetMapping("/export/{id}")
    public AjaxResult export(@PathVariable Long id)
    {
        return success(articleService.exportArticle(id));
    }

    /** 查询当前用户正在学习的文章。 */
    @GetMapping("/current")
    public AjaxResult current()
    {
        return success(articleService.getCurrent());
    }
}
