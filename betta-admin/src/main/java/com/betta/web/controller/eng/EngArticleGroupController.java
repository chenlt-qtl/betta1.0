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
import com.betta.eng.domain.EngArticleGroup;
import com.betta.eng.service.IEngArticleGroupService;

/** 英语文章分组控制器，只负责参数转发和统一响应。 */
@RestController
@RequestMapping("/eng/group")
public class EngArticleGroupController extends BaseController
{
    private final IEngArticleGroupService service;

    /** 创建分组控制器；service 负责全部分组业务。 */
    public EngArticleGroupController(IEngArticleGroupService service)
    {
        this.service = service;
    }

    /** 分页查询 group 条件对应分组。 */
    @GetMapping("/list")
    public TableDataInfo list(EngArticleGroup group)
    {
        startPage();
        List<EngArticleGroup> list = service.selectEngArticleGroupList(group);
        return getDataTable(list);
    }

    /** 将 group 条件对应分组导出为 Excel 响应。 */
    @PostMapping("/export")
    public void export(HttpServletResponse response, EngArticleGroup group)
    {
        new ExcelUtil<>(EngArticleGroup.class).exportExcel(response, service.selectEngArticleGroupList(group), "英语文章分组数据");
    }

    /** 查询 id 对应分组。 */
    @PreAuthorize("@ss.hasPermi('eng:group:query')")
    @GetMapping("/{id}")
    public AjaxResult info(@PathVariable Long id)
    {
        return success(service.selectEngArticleGroupById(id));
    }

    /** 新增 group。 */
    @PreAuthorize("@ss.hasPermi('eng:group:add')")
    @Log(title = "英语文章分组", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody EngArticleGroup group)
    {
        return toAjax(service.insertEngArticleGroup(group));
    }

    /** 更新 group。 */
    @PreAuthorize("@ss.hasPermi('eng:group:edit')")
    @Log(title = "英语文章分组", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody EngArticleGroup group)
    {
        return toAjax(service.updateEngArticleGroup(group));
    }

    /** 删除 ids 对应分组。 */
    @PreAuthorize("@ss.hasPermi('eng:group:remove')")
    @Log(title = "英语文章分组", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(service.deleteEngArticleGroupByIds(ids));
    }
}
