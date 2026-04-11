package com.betta.robot.controller;

import com.betta.common.annotation.Log;
import com.betta.common.core.controller.BaseController;
import com.betta.common.core.domain.AjaxResult;
import com.betta.common.core.page.TableDataInfo;
import com.betta.common.enums.BusinessType;
import com.betta.common.utils.SecurityUtils;
import com.betta.robot.domain.MessageLlmConfig;
import com.betta.robot.service.IMessageLlmConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 大模型配置管理
 *
 * @author betta
 */
@RestController
@RequestMapping("/message/llm")
public class MessageLlmConfigController extends BaseController {

    @Autowired
    private IMessageLlmConfigService messageLlmConfigService;

    /**
     * 查询配置列表
     *
     * @param config 查询条件
     * @return 分页列表
     */
    @PreAuthorize("@ss.hasPermi('message:llm:list')")
    @GetMapping("/list")
    public TableDataInfo list(MessageLlmConfig config) {
        startPage();
        List<MessageLlmConfig> list = messageLlmConfigService.selectList(config);
        return getDataTable(list);
    }

    /**
     * 获取配置详情
     *
     * @param id 主键
     * @return 配置
     */
    @PreAuthorize("@ss.hasPermi('message:llm:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(messageLlmConfigService.selectById(id));
    }

    /**
     * 新增配置
     *
     * @param config 配置
     * @return 结果
     */
    @PreAuthorize("@ss.hasPermi('message:llm:add')")
    @Log(title = "大模型配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MessageLlmConfig config) {
        config.setCreateBy(SecurityUtils.getUsername());
        return toAjax(messageLlmConfigService.insert(config));
    }

    /**
     * 修改配置
     *
     * @param config 配置
     * @return 结果
     */
    @PreAuthorize("@ss.hasPermi('message:llm:edit')")
    @Log(title = "大模型配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MessageLlmConfig config) {
        config.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(messageLlmConfigService.update(config));
    }

    /**
     * 删除配置
     *
     * @param id 主键
     * @return 结果
     */
    @PreAuthorize("@ss.hasPermi('message:llm:remove')")
    @Log(title = "大模型配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(messageLlmConfigService.deleteById(id));
    }
}
