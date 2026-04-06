package com.betta.web.controller.system;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.betta.common.annotation.Log;
import com.betta.common.core.controller.BaseController;
import com.betta.common.core.domain.AjaxResult;
import com.betta.common.core.page.TableDataInfo;
import com.betta.common.enums.BusinessType;
import com.betta.common.utils.poi.ExcelUtil;
import com.betta.system.domain.CardHistory;
import com.betta.system.service.ICardHistoryService;

/**
 * 卡历史记录 信息操作处理
 *
 * @author betta
 */
@RestController
@RequestMapping("/system/card/history")
public class CardHistoryController extends BaseController {
    @Autowired
    private ICardHistoryService cardHistoryService;

    /**
     * 获取卡历史记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:card:history:list')")
    @GetMapping("/list")
    public TableDataInfo list(CardHistory cardHistory) {
        startPage();
        List<CardHistory> list = cardHistoryService.selectCardHistoryList(cardHistory);
        return getDataTable(list);
    }

    /**
     * 根据账户ID查询历史记录
     */
    @PreAuthorize("@ss.hasPermi('system:card:history:list')")
    @GetMapping("/account/{accountId}")
    public TableDataInfo listByAccount(@PathVariable Long accountId) {
        startPage();
        List<CardHistory> list = cardHistoryService.selectCardHistoryByAccountId(accountId);
        return getDataTable(list);
    }

    /**
     * 导出卡历史记录列表
     */
    @Log(title = "卡历史记录", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:card:history:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, CardHistory cardHistory) {
        List<CardHistory> list = cardHistoryService.selectCardHistoryList(cardHistory);
        ExcelUtil<CardHistory> util = new ExcelUtil<>(CardHistory.class);
        util.exportExcel(response, list, "卡历史记录数据");
    }

    /**
     * 根据编号获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:card:history:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(cardHistoryService.selectCardHistoryById(id));
    }

    /**
     * 删除卡历史记录
     */
    @PreAuthorize("@ss.hasPermi('system:card:history:remove')")
    @Log(title = "卡历史记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cardHistoryService.deleteCardHistoryByIds(ids));
    }
}
