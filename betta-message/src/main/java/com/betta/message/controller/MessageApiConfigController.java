package com.betta.message.controller;

import com.betta.common.annotation.Log;
import com.betta.common.core.controller.BaseController;
import com.betta.common.core.domain.AjaxResult;
import com.betta.common.core.page.TableDataInfo;
import com.betta.common.enums.BusinessType;
import com.betta.common.utils.SecurityUtils;
import com.betta.message.domain.MessageApiConfig;
import com.betta.message.service.IMessageApiConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API配置管理
 *
 * @author betta
 */
@RestController
@RequestMapping("/message/api")
public class MessageApiConfigController extends BaseController {

    @Autowired
    private IMessageApiConfigService messageApiConfigService;

    /**
     * 查询配置列表
     *
     * @param config 查询条件
     * @return 分页列表
     */
    @PreAuthorize("@ss.hasPermi('message:api:list')")
    @GetMapping("/list")
    public TableDataInfo list(MessageApiConfig config) {
        startPage();
        List<MessageApiConfig> list = messageApiConfigService.selectList(config);
        return getDataTable(list);
    }

    /**
     * 获取配置详情
     *
     * @param id 主键
     * @return 配置
     */
    @PreAuthorize("@ss.hasPermi('message:api:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(messageApiConfigService.selectById(id));
    }

    /**
     * 新增配置
     *
     * @param config 配置
     * @return 结果
     */
    @PreAuthorize("@ss.hasPermi('message:api:add')")
    @Log(title = "API配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MessageApiConfig config) {
        config.setCreateBy(SecurityUtils.getUsername());
        return toAjax(messageApiConfigService.insert(config));
    }

    /**
     * 修改配置
     *
     * @param config 配置
     * @return 结果
     */
    @PreAuthorize("@ss.hasPermi('message:api:edit')")
    @Log(title = "API配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MessageApiConfig config) {
        config.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(messageApiConfigService.update(config));
    }

    /**
     * 删除配置
     *
     * @param id 主键
     * @return 结果
     */
    @PreAuthorize("@ss.hasPermi('message:api:remove')")
    @Log(title = "API配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(messageApiConfigService.deleteById(id));
    }
}
