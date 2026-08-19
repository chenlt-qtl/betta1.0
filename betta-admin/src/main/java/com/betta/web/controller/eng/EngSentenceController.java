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
import com.betta.eng.domain.EngSentence;
import com.betta.eng.domain.dojo.BatchAddSentences;
import com.betta.eng.service.IEngSentenceService;

/** 英语句子控制器，只负责参数、分页、服务调用和响应。 */
@RestController
@RequestMapping("/eng/sentence")
public class EngSentenceController extends BaseController
{
    private final IEngSentenceService service;

    /** 创建句子控制器；service 负责所有句子业务。 */
    public EngSentenceController(IEngSentenceService service)
    {
        this.service = service;
    }

    /** 分页查询 sentence 条件对应句子。 */
    @PreAuthorize("@ss.hasPermi('eng:sentence:list')")
    @GetMapping("/list")
    public TableDataInfo list(EngSentence sentence)
    {
        startPage();
        List<EngSentence> list = service.selectEngSentenceList(sentence);
        return getDataTable(list);
    }

    /** 将 sentence 条件对应句子导出为 Excel 响应。 */
    @PostMapping("/export")
    public void export(HttpServletResponse response, EngSentence sentence)
    {
        new ExcelUtil<>(EngSentence.class).exportExcel(response, service.selectEngSentenceList(sentence), "英语文章句子数据");
    }

    /** 按 sentence、inPlayList 和 username 查询播放句子。 */
    @GetMapping("/list/play")
    public TableDataInfo play(EngSentence sentence, boolean inPlayList, String username)
    {
        startPage();
        return getDataTable(service.selectPlayList(sentence, inPlayList, username));
    }

    /** 查询 id 对应句子。 */
    @GetMapping("/{id}")
    public AjaxResult info(@PathVariable Long id)
    {
        return success(service.selectEngSentenceById(id));
    }

    /** 新增 sentence。 */
    @PreAuthorize("@ss.hasPermi('eng:sentence:add')")
    @Log(title = "文章句子", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody EngSentence sentence)
    {
        return toAjax(service.insertEngSentence(sentence));
    }

    /** 批量新增 request 中的多行句子。 */
    @PreAuthorize("@ss.hasPermi('eng:sentence:add')")
    @Log(title = "批量添加句子", businessType = BusinessType.INSERT)
    @PostMapping("/batch")
    public AjaxResult batch(@RequestBody BatchAddSentences request)
    {
        return toAjax(service.insertEngSentenceBatch(request));
    }

    /** 更新 sentence。 */
    @PreAuthorize("@ss.hasPermi('eng:sentence:edit')")
    @Log(title = "文章句子", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody EngSentence sentence)
    {
        return toAjax(service.updateEngSentence(sentence));
    }

    /** 删除 ids 对应句子。 */
    @PreAuthorize("@ss.hasPermi('eng:sentence:remove')")
    @Log(title = "文章句子", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(service.deleteEngSentenceByIds(ids));
    }
}
