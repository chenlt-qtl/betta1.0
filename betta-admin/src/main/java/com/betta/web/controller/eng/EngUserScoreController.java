package com.betta.web.controller.eng;

import java.util.List;
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
import com.betta.eng.domain.EngUserScore;
import com.betta.eng.domain.vo.EngUserScoreVo;
import com.betta.eng.service.IEngUserScoreService;

/** 用户单词成绩控制器，只负责参数接收和调用服务。 */
@RestController
@RequestMapping("/eng/score")
public class EngUserScoreController extends BaseController
{
    private final IEngUserScoreService service;

    /** 创建成绩控制器；两个参数分别用于成绩处理和示例句补充。 */
    public EngUserScoreController(IEngUserScoreService service)
    {
        this.service = service;
    }

    /** 分页查询 score 条件对应当前用户成绩。 */
    @GetMapping("/list")
    public TableDataInfo list(EngUserScore score)
    {
        startPage();
        return getDataTable(service.selectEngUserScoreList(score));
    }

    /** 查询 score 明细，可按 withSentence 补充示例句。 */
    @GetMapping("/list/user")
    public TableDataInfo user(EngUserScoreVo score, boolean withSentence)
    {
        startPage();
        return getDataTable(service.selectEngUserScoreVoList(score, withSentence));
    }

    /** 新增 score。 */
    @Log(title = "用户成绩", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody EngUserScore score)
    {
        return toAjax(service.insertEngUserScore(score));
    }

    /** 更新 score。 */
    @Log(title = "用户成绩", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody EngUserScore score)
    {
        return toAjax(service.updateEngUserScore(score));
    }

    /** 批量累计 scores 中的熟悉度增量。 */
    @PutMapping("/batch")
    public AjaxResult batch(@RequestBody List<EngUserScore> scores)
    {
        service.batchUpdate(scores);
        return success();
    }

    /** 删除 ids 对应当前用户成绩。 */
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(service.deleteEngUserScoreByIds(ids));
    }
}
