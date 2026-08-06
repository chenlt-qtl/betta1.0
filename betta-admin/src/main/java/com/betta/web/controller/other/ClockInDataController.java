package com.betta.web.controller.other;

import java.util.List;
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
import com.betta.other.domain.ClockInData;
import com.betta.other.service.IClockInDataService;
import com.betta.common.utils.poi.ExcelUtil;
import com.betta.common.core.page.TableDataInfo;

/**
 * 打卡数据Controller
 * 
 * @author chenlt
 * @date 2024-07-01
 */
@RestController
@RequestMapping("/other/clockInData")
public class ClockInDataController extends BaseController
{
    @Autowired
    private IClockInDataService clockInDataService;

    /**
     * 查询打卡数据列表
     */
    @PreAuthorize("@ss.hasPermi('other:clockInData:list')")
    @GetMapping("/list")
    public TableDataInfo list(ClockInData clockInData)
    {
        startPage();
        List<ClockInData> list = clockInDataService.selectClockInDataList(clockInData);
        return getDataTable(list);
    }

    /**
     * 导出打卡数据列表
     */
    @PreAuthorize("@ss.hasPermi('other:clockInData:export')")
    @Log(title = "打卡数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ClockInData clockInData)
    {
        List<ClockInData> list = clockInDataService.selectClockInDataList(clockInData);
        ExcelUtil<ClockInData> util = new ExcelUtil<>(ClockInData.class);
        util.exportExcel(response, list, "打卡数据数据");
    }

    /**
     * 获取打卡数据详细信息
     */
    @PreAuthorize("@ss.hasPermi('other:clockInData:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(clockInDataService.selectClockInDataById(id));
    }

    /**
     * 新增打卡数据
     */
    @PreAuthorize("@ss.hasPermi('other:clockInData:add')")
    @Log(title = "打卡数据", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ClockInData clockInData)
    {
        return toAjax(clockInDataService.insertClockInData(clockInData));
    }

    /**
     * 修改打卡数据
     */
    @PreAuthorize("@ss.hasPermi('other:clockInData:edit')")
    @Log(title = "打卡数据", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ClockInData clockInData)
    {
        return toAjax(clockInDataService.updateClockInData(clockInData));
    }

    /**
     * 删除打卡数据
     */
    @PreAuthorize("@ss.hasPermi('other:clockInData:remove')")
    @Log(title = "打卡数据", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(clockInDataService.deleteClockInDataByIds(ids));
    }
}
