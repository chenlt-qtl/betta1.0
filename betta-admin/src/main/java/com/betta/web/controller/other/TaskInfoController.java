package com.betta.web.controller.other;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;

import com.betta.other.domain.TaskInfo;
import com.betta.other.service.ITaskInfoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.betta.common.annotation.Log;
import com.betta.common.core.controller.BaseController;
import com.betta.common.core.domain.AjaxResult;
import com.betta.common.enums.BusinessType;
import com.betta.common.utils.poi.ExcelUtil;
import com.betta.common.core.page.TableDataInfo;

/**
 * 任务Controller
 * 
 * @author chenlt
 * @date 2025-01-10
 */
@RestController
@RequestMapping("/other/task")
public class TaskInfoController extends BaseController
{
    @Autowired
    private ITaskInfoService taskInfoService;

    /**
     * 查询任务列表
     */
    @PreAuthorize("@ss.hasPermi('other:task:list')")
    @GetMapping("/list")
    public TableDataInfo list(TaskInfo taskInfo)
    {
        startPage();
        List<TaskInfo> list = taskInfoService.selectTaskInfoList(taskInfo);
        return getDataTable(list);
    }

    /**
     * 导出任务列表
     */
    @PreAuthorize("@ss.hasPermi('other:task:export')")
    @Log(title = "任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaskInfo taskInfo)
    {
        List<TaskInfo> list = taskInfoService.selectTaskInfoList(taskInfo);
        ExcelUtil<TaskInfo> util = new ExcelUtil<>(TaskInfo.class);
        util.exportExcel(response, list, "任务数据");
    }

    /**
     * 获取任务详细信息
     */
    @PreAuthorize("@ss.hasPermi('other:task:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(taskInfoService.selectTaskInfoById(id));
    }

    /**
     * 新增任务
     */
    @PreAuthorize("@ss.hasPermi('other:task:add')")
    @Log(title = "任务", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TaskInfo taskInfo)
    {
        return toAjax(taskInfoService.insertTaskInfo(taskInfo));
    }

    /**
     * 修改任务
     */
    @PreAuthorize("@ss.hasPermi('other:task:edit')")
    @Log(title = "任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TaskInfo taskInfo)
    {
        return toAjax(taskInfoService.updateTaskInfo(taskInfo));
    }

    /**
     * 删除任务
     */
    @PreAuthorize("@ss.hasPermi('other:task:remove')")
    @Log(title = "任务", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(taskInfoService.deleteTaskInfoByIds(ids));
    }
}
