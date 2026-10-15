package com.betta.web.controller.eng;

import java.math.BigDecimal;
import java.util.List;
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
import com.betta.common.annotation.RepeatSubmit;
import com.betta.common.core.controller.BaseController;
import com.betta.common.core.domain.AjaxResult;
import com.betta.common.core.page.TableDataInfo;
import com.betta.common.enums.BusinessType;
import com.betta.eng.domain.dto.MallExchangeRateRequest;
import com.betta.eng.domain.dto.MallExchangeRequest;
import com.betta.eng.domain.mall.MallExchangeRecord;
import com.betta.eng.domain.mall.MallProduct;
import com.betta.eng.service.IMallService;

/** 商城控制器，提供登录用户商城接口和管理员维护接口。 */
@RestController
@RequestMapping("/mall")
public class MallController extends BaseController
{
    private final IMallService mallService;

    /** 创建商城控制器。 */
    public MallController(IMallService mallService)
    {
        this.mallService = mallService;
    }

    /** 登录用户分页查看可兑换商品。 */
    @GetMapping("/store/products")
    public TableDataInfo storeProducts(MallProduct product)
    {
        startPage();
        return getDataTable(mallService.selectProductList(product));
    }

    /** 登录用户查看商品详情。 */
    @GetMapping("/store/products/{id}")
    public AjaxResult storeProduct(@PathVariable Long id)
    {
        return success(mallService.selectProductById(id));
    }

    /** 登录用户查看本人金币余额。 */
    @GetMapping("/store/balance")
    public AjaxResult balance()
    {
        return success(mallService.getCurrentCoinBalance());
    }

    /** 登录用户兑换一件商品。 */
    @RepeatSubmit
    @Log(title = "金币商城兑换", businessType = BusinessType.INSERT)
    @PostMapping("/store/exchanges/{productId}")
    public AjaxResult exchange(@PathVariable Long productId, @RequestBody MallExchangeRequest request)
    {
        return success(mallService.exchange(productId, request == null ? null : request.getExpectedCoinPrice()));
    }

    /** 登录用户分页查看本人兑换记录。 */
    @GetMapping("/store/exchanges")
    public TableDataInfo currentExchanges(MallExchangeRecord record)
    {
        startPage();
        return getDataTable(mallService.selectCurrentExchangeList(record));
    }

    /** 管理员分页查询商品。 */
    @PreAuthorize("@ss.hasPermi('mall:product:list')")
    @GetMapping("/product/list")
    public TableDataInfo productList(MallProduct product)
    {
        startPage();
        List<MallProduct> list = mallService.selectProductList(product);
        return getDataTable(list);
    }

    /** 管理员查询商品详情。 */
    @PreAuthorize("@ss.hasPermi('mall:product:query')")
    @GetMapping("/product/{id}")
    public AjaxResult productInfo(@PathVariable Long id)
    {
        return success(mallService.selectProductById(id));
    }

    /** 管理员新增商品。 */
    @PreAuthorize("@ss.hasPermi('mall:product:add')")
    @Log(title = "商城商品", businessType = BusinessType.INSERT)
    @PostMapping("/product")
    public AjaxResult addProduct(@RequestBody MallProduct product)
    {
        return success(mallService.insertProduct(product));
    }

    /** 管理员批量新增商品，单次最多五十条。 */
    @PreAuthorize("@ss.hasPermi('mall:product:add')")
    @Log(title = "商城商品", businessType = BusinessType.INSERT)
    @PostMapping("/product/batch")
    public AjaxResult addProductBatch(@RequestBody List<MallProduct> products)
    {
        return toAjax(mallService.insertProductBatch(products));
    }

    /** 管理员查询人民币兑金币汇率。 */
    @PreAuthorize("@ss.hasPermi('mall:product:list')")
    @GetMapping("/product/exchange-rate")
    public AjaxResult exchangeRate()
    {
        return success(mallService.getExchangeRate());
    }

    /** 管理员更新人民币兑金币汇率。 */
    @PreAuthorize("@ss.hasPermi('mall:product:rate')")
    @Log(title = "商城汇率", businessType = BusinessType.UPDATE)
    @PutMapping("/product/exchange-rate")
    public AjaxResult updateExchangeRate(@RequestBody MallExchangeRateRequest request)
    {
        BigDecimal rate = request == null ? null : request.getRate();
        return toAjax(mallService.updateExchangeRate(rate));
    }

    /** 管理员更新商品。 */
    @PreAuthorize("@ss.hasPermi('mall:product:edit')")
    @Log(title = "商城商品", businessType = BusinessType.UPDATE)
    @PutMapping("/product")
    public AjaxResult editProduct(@RequestBody MallProduct product)
    {
        return toAjax(mallService.updateProduct(product));
    }

    /** 管理员批量硬删除商品。 */
    @PreAuthorize("@ss.hasPermi('mall:product:remove')")
    @Log(title = "商城商品", businessType = BusinessType.DELETE)
    @DeleteMapping("/product/{ids}")
    public AjaxResult removeProduct(@PathVariable Long[] ids)
    {
        return toAjax(mallService.deleteProductByIds(ids));
    }

    /** 管理员分页查看全部兑换记录。 */
    @PreAuthorize("@ss.hasPermi('mall:exchange:list')")
    @GetMapping("/exchange/list")
    public TableDataInfo exchangeList(MallExchangeRecord record)
    {
        startPage();
        return getDataTable(mallService.selectExchangeList(record));
    }
}
