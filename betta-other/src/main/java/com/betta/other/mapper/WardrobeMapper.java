package com.betta.other.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.betta.other.domain.wardrobe.WardrobeImage;
import com.betta.other.domain.wardrobe.WardrobeItem;
import com.betta.other.domain.wardrobe.WardrobeOutfit;

/** 衣橱数据访问，所有业务对象查询均限定所属用户。 */
public interface WardrobeMapper
{
    List<WardrobeItem> selectItems(WardrobeItem query);
    WardrobeItem selectItem(@Param("userId") Long userId, @Param("id") Long id);
    Long lockItem(@Param("userId") Long userId, @Param("id") Long id);
    int insertItem(WardrobeItem item);
    int updateItem(WardrobeItem item);
    int deleteItem(@Param("userId") Long userId, @Param("id") Long id);
    int countItemReferences(@Param("id") Long id);
    WardrobeImage selectItemImage(@Param("userId") Long userId, @Param("id") Long id);
    int upsertItemImage(@Param("id") Long id, @Param("image") WardrobeImage image);

    List<WardrobeOutfit> selectOutfits(@Param("userId") Long userId);
    WardrobeOutfit selectOutfit(@Param("userId") Long userId, @Param("id") Long id);
    Long lockOutfit(@Param("userId") Long userId, @Param("id") Long id);
    List<WardrobeItem> selectOutfitItems(@Param("userId") Long userId, @Param("id") Long id);
    int insertOutfit(WardrobeOutfit outfit);
    int updateOutfit(WardrobeOutfit outfit);
    int updateFavorite(@Param("userId") Long userId, @Param("id") Long id, @Param("favorite") Boolean favorite);
    int deleteOutfit(@Param("userId") Long userId, @Param("id") Long id);
    int deleteOutfitItems(@Param("id") Long id);
    int insertOutfitItem(@Param("outfitId") Long outfitId, @Param("itemId") Long itemId, @Param("sortOrder") int sortOrder);
    WardrobeImage selectOutfitImage(@Param("userId") Long userId, @Param("id") Long id);
    int upsertOutfitImage(@Param("id") Long id, @Param("image") WardrobeImage image);
    int deleteOutfitImage(@Param("id") Long id);

    /** 按 ID 固定顺序锁定当前用户的搭配，供完整顺序调整使用。 */
    List<WardrobeOutfit> lockUserOutfits(@Param("userId") Long userId);
    /** 仅改变排序值，保留搭配业务更新时间。 */
    int updateOutfitSortOrder(@Param("userId") Long userId, @Param("id") Long id, @Param("sortOrder") long sortOrder);
}
