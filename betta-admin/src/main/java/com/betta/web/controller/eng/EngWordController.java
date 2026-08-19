package com.betta.web.controller.eng;

import java.util.List;
import java.util.Map;
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
import com.betta.eng.domain.EngWord;
import com.betta.eng.service.IEngWordService;

/** 英语单词控制器，只负责参数接收、分页、服务调用和响应。 */
@RestController
@RequestMapping("/eng/word")
public class EngWordController extends BaseController
{
    private final IEngWordService service;

    /** 创建单词控制器；service 负责单词聚合业务。 */
    public EngWordController(IEngWordService service)
    {
        this.service = service;
    }

    /** 分页查询 word 条件对应单词。 */
    @GetMapping("/list")
    public TableDataInfo list(EngWord word)
    {
        startPage();
        return getDataTable(service.selectEngWordList(word));
    }

    /** 查询 articleId 关联单词。 */
    @GetMapping("/list/{articleId}")
    public TableDataInfo articleWords(@PathVariable Long articleId)
    {
        startPage();
        return getDataTable(service.selectWordListByArticle(articleId));
    }

    /** 分页查询当前用户生词。 */
    @GetMapping("/new")
    public TableDataInfo newWords(EngWord word)
    {
        startPage();
        return getDataTable(service.selectNewList(word));
    }

    /** 查询 id 对应单词。 */
    @GetMapping("/{id}")
    public AjaxResult id(@PathVariable Long id)
    {
        return success(service.selectEngWordById(id));
    }

    /** 按 wordName 查询单词聚合详情。 */
    @GetMapping
    public AjaxResult info(String wordName)
    {
        return success(service.getWordVo(wordName));
    }

    /** 从外部词典查询 wordName。 */
    @GetMapping("/api/{wordName}")
    public AjaxResult api(@PathVariable String wordName)
    {
        return success(service.getWordFromApi(wordName));
    }

    /** 新增 word。 */
    @PreAuthorize("@ss.hasPermi('eng:word:add')")
    @Log(title = "单词", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody EngWord word)
    {
        return toAjax(service.addEngWord(word));
    }

    /** 用 body.words 全量更新 articleId 的文章单词。 */
    @PostMapping("/{articleId}")
    public AjaxResult updateArticle(@PathVariable Long articleId, @RequestBody Map<String, List<String>> body)
    {
        service.updateByArticle(body.get("words"), articleId);
        return success();
    }

    /** 为 articleId 新增 wordName。 */
    @PostMapping("/{articleId}/{wordName}")
    public AjaxResult addArticleWord(@PathVariable Long articleId, @PathVariable String wordName)
    {
        service.addArticleWord(articleId, wordName);
        return success();
    }

    /** 更新 word。 */
    @PutMapping
    public AjaxResult edit(@RequestBody EngWord word)
    {
        return toAjax(service.updateEngWord(word));
    }

    /** 删除 ids 对应单词。 */
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        service.deleteEngWordByIds(ids);
        return success();
    }
}
