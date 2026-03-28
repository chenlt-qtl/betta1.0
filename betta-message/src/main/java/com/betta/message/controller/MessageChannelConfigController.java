package com.betta.message.controller;

import com.betta.common.annotation.Log;
import com.betta.common.core.controller.BaseController;
import com.betta.common.core.domain.AjaxResult;
import com.betta.common.core.page.TableDataInfo;
import com.betta.common.enums.BusinessType;
import com.betta.common.utils.SecurityUtils;
import com.betta.message.domain.MessageChannelConfig;
import com.betta.message.service.IMessageChannelConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息通道配置（飞书/微信）管理
 *
 * @author betta
 */
@RestController
@RequestMapping("/message/config")
public class MessageChannelConfigController extends BaseController {

    @Autowired
    private IMessageChannelConfigService messageChannelConfigService;

    /**
     * 查询通道配置列表
     *
     * @param config 查询条件
     * @return 分页列表
     */
    @PreAuthorize("@ss.hasPermi('message:config:list')")
    @GetMapping("/list")
    public TableDataInfo list(MessageChannelConfig config) {
        startPage();
        List<MessageChannelConfig> list = messageChannelConfigService.selectList(config);
        return getDataTable(list);
    }

    /**
     * 根据 ID 获取通道配置详情
     *
     * @param id 主键
     * @return 配置
     */
    @PreAuthorize("@ss.hasPermi('message:config:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(messageChannelConfigService.selectById(id));
    }

    /**
     * 新增通道配置
     *
     * @param config 配置
     * @return 结果
     */
    @PreAuthorize("@ss.hasPermi('message:config:add')")
    @Log(title = "消息通道配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MessageChannelConfig config) {
        config.setCreateBy(SecurityUtils.getUsername());
        return toAjax(messageChannelConfigService.insert(config));
    }

    /**
     * 修改通道配置
     *
     * @param config 配置
     * @return 结果
     */
    @PreAuthorize("@ss.hasPermi('message:config:edit')")
    @Log(title = "消息通道配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MessageChannelConfig config) {
        config.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(messageChannelConfigService.update(config));
    }

    /**
     * 删除通道配置
     *
     * @param id 主键
     * @return 结果
     */
    @PreAuthorize("@ss.hasPermi('message:config:remove')")
    @Log(title = "消息通道配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(messageChannelConfigService.deleteById(id));
    }
}
