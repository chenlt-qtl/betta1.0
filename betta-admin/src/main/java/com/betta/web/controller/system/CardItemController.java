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
import com.betta.system.domain.CardItem;
import com.betta.system.service.ICardItemService;

/**
 * 卡预设项 信息操作处理
 *
 * @author betta
 */
@RestController
@RequestMapping("/system/card/item")
public class CardItemController extends BaseController {
    @Autowired
    private ICardItemService cardItemService;

    /**
     * 获取卡预设项列表
     */
    @PreAuthorize("@ss.hasPermi('system:card:item:list')")
    @GetMapping("/list")
    public TableDataInfo list(CardItem cardItem) {
        startPage();
        List<CardItem> list = cardItemService.selectCardItemList(cardItem);
        return getDataTable(list);
    }

    /**
     * 根据类型查询卡预设项
     */
    @PreAuthorize("@ss.hasPermi('system:card:item:list')")
    @GetMapping("/type/{type}")
    public AjaxResult listByType(@PathVariable Integer type) {
        return success(cardItemService.selectCardItemByType(type));
    }

    /**
     * 导出卡预设项列表
     */
    @Log(title = "卡预设项", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:card:item:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, CardItem cardItem) {
        List<CardItem> list = cardItemService.selectCardItemList(cardItem);
        ExcelUtil<CardItem> util = new ExcelUtil<>(CardItem.class);
        util.exportExcel(response, list, "卡预设项数据");
    }

    /**
     * 根据编号获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:card:item:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(cardItemService.selectCardItemById(id));
    }

    /**
     * 新增卡预设项
     */
    @PreAuthorize("@ss.hasPermi('system:card:item:add')")
    @Log(title = "卡预设项", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CardItem cardItem) {
        cardItem.setCreateBy(getUsername());
        return toAjax(cardItemService.insertCardItem(cardItem));
    }

    /**
     * 修改卡预设项
     */
    @PreAuthorize("@ss.hasPermi('system:card:item:edit')")
    @Log(title = "卡预设项", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CardItem cardItem) {
        cardItem.setUpdateBy(getUsername());
        return toAjax(cardItemService.updateCardItem(cardItem));
    }

    /**
     * 删除卡预设项
     */
    @PreAuthorize("@ss.hasPermi('system:card:item:remove')")
    @Log(title = "卡预设项", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cardItemService.deleteCardItemByIds(ids));
    }
}
