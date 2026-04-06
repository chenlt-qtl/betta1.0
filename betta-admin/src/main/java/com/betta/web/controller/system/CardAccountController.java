package com.betta.web.controller.system;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
import com.betta.system.domain.CardAccount;
import com.betta.system.service.ICardAccountService;

/**
 * 卡账户 信息操作处理
 *
 * @author betta
 */
@RestController
@RequestMapping("/system/card/account")
public class CardAccountController extends BaseController {
    @Autowired
    private ICardAccountService cardAccountService;

    /**
     * 获取卡账户列表
     */
    @PreAuthorize("@ss.hasPermi('system:card:account:list')")
    @GetMapping("/list")
    public TableDataInfo list(CardAccount cardAccount) {
        startPage();
        List<CardAccount> list = cardAccountService.selectCardAccountList(cardAccount);
        return getDataTable(list);
    }

    @Log(title = "卡账户管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:card:account:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, CardAccount cardAccount) {
        List<CardAccount> list = cardAccountService.selectCardAccountList(cardAccount);
        ExcelUtil<CardAccount> util = new ExcelUtil<>(CardAccount.class);
        util.exportExcel(response, list, "卡账户数据");
    }

    /**
     * 根据账户编号获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:card:account:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(cardAccountService.selectCardAccountById(id));
    }

    /**
     * 新增卡账户
     */
    @PreAuthorize("@ss.hasPermi('system:card:account:add')")
    @Log(title = "卡账户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CardAccount cardAccount) {
        if (!cardAccountService.checkNameUnique(cardAccount)) {
            return error("新增卡账户'" + cardAccount.getName() + "'失败，人名已存在");
        }
        cardAccount.setCreateBy(getUsername());
        return toAjax(cardAccountService.insertCardAccount(cardAccount));
    }

    /**
     * 修改卡账户
     */
    @PreAuthorize("@ss.hasPermi('system:card:account:edit')")
    @Log(title = "卡账户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CardAccount cardAccount) {
        if (!cardAccountService.checkNameUnique(cardAccount)) {
            return error("修改卡账户'" + cardAccount.getName() + "'失败，人名已存在");
        }
        cardAccount.setUpdateBy(getUsername());
        return toAjax(cardAccountService.updateCardAccount(cardAccount));
    }

    /**
     * 删除卡账户
     */
    @PreAuthorize("@ss.hasPermi('system:card:account:remove')")
    @Log(title = "卡账户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cardAccountService.deleteCardAccountByIds(ids));
    }
}
