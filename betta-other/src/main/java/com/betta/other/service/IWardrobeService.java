package com.betta.other.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import com.betta.other.domain.wardrobe.WardrobeImage;
import com.betta.other.domain.wardrobe.WardrobeItem;
import com.betta.other.domain.wardrobe.WardrobeOutfit;

/** 个人衣橱与搭配的业务接口。 */
public interface IWardrobeService
{
    List<WardrobeItem> listItems(WardrobeItem query);
    WardrobeItem getItem(Long id);
    WardrobeItem createItem(WardrobeItem item, MultipartFile image);
    WardrobeItem updateItem(Long id, WardrobeItem item, MultipartFile image);
    void deleteItem(Long id);
    WardrobeImage getItemImage(Long id);
    List<WardrobeOutfit> listOutfits();
    WardrobeOutfit getOutfit(Long id);
    WardrobeOutfit createOutfit(WardrobeOutfit outfit, MultipartFile image);
    WardrobeOutfit updateOutfit(Long id, WardrobeOutfit outfit, MultipartFile image);
    void deleteOutfit(Long id);
    void updateFavorite(Long id, Boolean favorite);
    WardrobeImage getOutfitImage(Long id);
    /** 调整本人搭配的全量顺序，返回是否实际移动。 */
    boolean moveOutfit(Long id, String direction);
    List<WardrobeOutfit> recommend(Double temperature, String occasion);
}
