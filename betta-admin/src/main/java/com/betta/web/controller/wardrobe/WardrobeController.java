package com.betta.web.controller.wardrobe;

import java.util.List;
import com.github.pagehelper.PageHelper;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.betta.common.core.controller.BaseController;
import com.betta.common.core.domain.AjaxResult;
import com.betta.common.core.page.PageDomain;
import com.betta.common.core.page.TableDataInfo;
import com.betta.common.core.page.TableSupport;
import com.betta.other.domain.wardrobe.WardrobeFavoriteUpdate;
import com.betta.other.domain.wardrobe.WardrobeImage;
import com.betta.other.domain.wardrobe.WardrobeItem;
import com.betta.other.domain.wardrobe.WardrobeOutfit;
import com.betta.other.domain.wardrobe.WardrobePositionUpdate;
import com.betta.other.service.IWardrobeService;

/** 登录用户的私人衣橱和搭配接口。 */
@RestController
@RequestMapping("/wardrobe")
public class WardrobeController extends BaseController
{
    private final IWardrobeService service;

    /** 创建衣橱控制器。 */
    public WardrobeController(IWardrobeService service)
    {
        this.service = service;
    }

    /** 查询当前用户的单品。 */
    @GetMapping("/items")
    public TableDataInfo items(WardrobeItem query)
    {
        startPage();
        return getDataTable(service.listItems(query));
    }

    /** 获取当前用户的单品详情。 */
    @GetMapping("/items/{id}")
    public AjaxResult item(@PathVariable Long id)
    {
        return success(service.getItem(id));
    }

    /** 新增单品及其必填照片。 */
    @PostMapping(value = "/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult createItem(@RequestPart("item") WardrobeItem item, @RequestPart("image") MultipartFile image)
    {
        return success(service.createItem(item, image));
    }

    /** 修改单品；未上传图片时保留原图。 */
    @PutMapping(value = "/items/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult updateItem(@PathVariable Long id, @RequestPart("item") WardrobeItem item,
            @RequestPart(value = "image", required = false) MultipartFile image)
    {
        return success(service.updateItem(id, item, image));
    }

    /** 删除未被搭配引用的单品。 */
    @DeleteMapping("/items/{id}")
    public AjaxResult deleteItem(@PathVariable Long id)
    {
        service.deleteItem(id);
        return success();
    }

    /** 仅读取本人的单品照片，禁止共享缓存。 */
    @GetMapping("/items/{id}/image")
    public ResponseEntity<byte[]> itemImage(@PathVariable Long id)
    {
        return privateImage(service.getItemImage(id));
    }

    /** 查询当前用户的搭配。 */
    @GetMapping("/outfits")
    public TableDataInfo outfits()
    {
        PageDomain page = TableSupport.buildPageRequest();
        // 搭配位置依赖固定的服务端顺序，客户端排序参数不能覆盖该顺序。
        PageHelper.startPage(page.getPageNum(), page.getPageSize(), "").setReasonable(page.getReasonable());
        return getDataTable(service.listOutfits());
    }

    /** 获取搭配及单品顺序。 */
    @GetMapping("/outfits/{id}")
    public AjaxResult outfit(@PathVariable Long id)
    {
        return success(service.getOutfit(id));
    }

    /** 新增搭配和可选效果照。 */
    @PostMapping(value = "/outfits", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult createOutfit(@RequestPart("outfit") WardrobeOutfit outfit,
            @RequestPart(value = "image", required = false) MultipartFile image)
    {
        return success(service.createOutfit(outfit, image));
    }

    /** 修改搭配，并按 imageAction 保留、替换或移除效果照。 */
    @PutMapping(value = "/outfits/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult updateOutfit(@PathVariable Long id, @RequestPart("outfit") WardrobeOutfit outfit,
            @RequestPart(value = "image", required = false) MultipartFile image)
    {
        return success(service.updateOutfit(id, outfit, image));
    }

    /** 删除当前用户的搭配。 */
    @DeleteMapping("/outfits/{id}")
    public AjaxResult deleteOutfit(@PathVariable Long id)
    {
        service.deleteOutfit(id);
        return success();
    }

    /** 设置搭配收藏。 */
    @PutMapping("/outfits/{id}/favorite")
    public AjaxResult favorite(@PathVariable Long id, @RequestBody WardrobeFavoriteUpdate update)
    {
        service.updateFavorite(id, update == null ? null : update.getFavorite());
        return success();
    }

    /** 仅读取本人的搭配效果照，禁止共享缓存。 */
    @GetMapping("/outfits/{id}/image")
    public ResponseEntity<byte[]> outfitImage(@PathVariable Long id)
    {
        return privateImage(service.getOutfitImage(id));
    }

    /** 将本人搭配在完整列表中上移或下移一位。 */
    @PutMapping("/outfits/{id}/position")
    public AjaxResult moveOutfit(@PathVariable Long id, @RequestBody WardrobePositionUpdate update)
    {
        return success(service.moveOutfit(id, update == null ? null : update.getDirection()));
    }

    /** 根据手动温度、场合及创建时间推荐已有搭配。 */
    @GetMapping("/recommendations")
    public AjaxResult recommendations(@RequestParam(required = false) Double temperature,
            @RequestParam(required = false) String occasion)
    {
        List<WardrobeOutfit> outfits = service.recommend(temperature, occasion);
        return success(outfits);
    }

    private ResponseEntity<byte[]> privateImage(WardrobeImage image)
    {
        if (image == null)
        {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(image.getContentType())).body(image.getContent());
    }
}
