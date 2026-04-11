package com.betta.robot.controller;

import java.util.List;

import com.betta.robot.domain.RobotToolConfig;
import com.betta.robot.service.IRobotToolConfigService;
import jakarta.servlet.http.HttpServletResponse;
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
 * 工具配置Controller
 * 
 * @author betta
 * @date 2026-04-11
 */
@RestController
@RequestMapping("/tool/config")
public class RobotToolConfigController extends BaseController
{
    @Autowired
    private IRobotToolConfigService robotToolConfigService;

    /**
     * 查询工具配置列表
     */
    @PreAuthorize("@ss.hasPermi('system:config:list')")
    @GetMapping("/list")
    public TableDataInfo list(RobotToolConfig robotToolConfig)
    {
        startPage();
        List<RobotToolConfig> list = robotToolConfigService.selectRobotToolConfigList(robotToolConfig);
        return getDataTable(list);
    }

    /**
     * 导出工具配置列表
     */
    @PreAuthorize("@ss.hasPermi('system:config:export')")
    @Log(title = "工具配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, RobotToolConfig robotToolConfig)
    {
        List<RobotToolConfig> list = robotToolConfigService.selectRobotToolConfigList(robotToolConfig);
        ExcelUtil<RobotToolConfig> util = new ExcelUtil<RobotToolConfig>(RobotToolConfig.class);
        util.exportExcel(response, list, "工具配置数据");
    }

    /**
     * 获取工具配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:config:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(robotToolConfigService.selectRobotToolConfigById(id));
    }

    /**
     * 新增工具配置
     */
    @PreAuthorize("@ss.hasPermi('system:config:add')")
    @Log(title = "工具配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody RobotToolConfig robotToolConfig)
    {
        return toAjax(robotToolConfigService.insertRobotToolConfig(robotToolConfig));
    }

    /**
     * 修改工具配置
     */
    @PreAuthorize("@ss.hasPermi('system:config:edit')")
    @Log(title = "工具配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody RobotToolConfig robotToolConfig)
    {
        return toAjax(robotToolConfigService.updateRobotToolConfig(robotToolConfig));
    }

    /**
     * 删除工具配置
     */
    @PreAuthorize("@ss.hasPermi('system:config:remove')")
    @Log(title = "工具配置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(robotToolConfigService.deleteRobotToolConfigByIds(ids));
    }
}
