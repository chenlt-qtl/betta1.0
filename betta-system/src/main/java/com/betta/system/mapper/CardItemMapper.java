package com.betta.system.mapper;

import java.util.List;
import com.betta.system.domain.CardItem;

/**
 * 卡预设项 数据层
 *
 * @author betta
 */
public interface CardItemMapper {
    /**
     * 查询卡预设项
     *
     * @param id 卡预设项主键
     * @return 卡预设项
     */
    public CardItem selectCardItemById(Long id);

    /**
     * 查询卡预设项列表
     *
     * @param cardItem 卡预设项
     * @return 卡预设项集合
     */
    public List<CardItem> selectCardItemList(CardItem cardItem);

    /**
     * 根据类型查询卡预设项
     *
     * @param type 类型
     * @return 卡预设项集合
     */
    public List<CardItem> selectCardItemByType(Integer type);

    /**
     * 新增卡预设项
     *
     * @param cardItem 卡预设项
     * @return 结果
     */
    public int insertCardItem(CardItem cardItem);

    /**
     * 修改卡预设项
     *
     * @param cardItem 卡预设项
     * @return 结果
     */
    public int updateCardItem(CardItem cardItem);

    /**
     * 删除卡预设项
     *
     * @param id 卡预设项主键
     * @return 结果
     */
    public int deleteCardItemById(Long id);

    /**
     * 批量删除卡预设项
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCardItemByIds(Long[] ids);
}
