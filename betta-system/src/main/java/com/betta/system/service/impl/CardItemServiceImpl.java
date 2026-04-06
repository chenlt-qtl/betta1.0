package com.betta.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.betta.system.domain.CardItem;
import com.betta.system.mapper.CardItemMapper;
import com.betta.system.service.ICardItemService;

/**
 * 卡预设项 服务层实现
 *
 * @author betta
 */
@Service
public class CardItemServiceImpl implements ICardItemService {
    @Autowired
    private CardItemMapper cardItemMapper;

    /**
     * 查询卡预设项
     *
     * @param id 卡预设项主键
     * @return 卡预设项
     */
    @Override
    public CardItem selectCardItemById(Long id) {
        return cardItemMapper.selectCardItemById(id);
    }

    /**
     * 查询卡预设项列表
     *
     * @param cardItem 卡预设项
     * @return 卡预设项
     */
    @Override
    public List<CardItem> selectCardItemList(CardItem cardItem) {
        return cardItemMapper.selectCardItemList(cardItem);
    }

    /**
     * 根据类型查询卡预设项
     *
     * @param type 类型
     * @return 卡预设项集合
     */
    @Override
    public List<CardItem> selectCardItemByType(Integer type) {
        return cardItemMapper.selectCardItemByType(type);
    }

    /**
     * 新增卡预设项
     *
     * @param cardItem 卡预设项
     * @return 结果
     */
    @Override
    public int insertCardItem(CardItem cardItem) {
        return cardItemMapper.insertCardItem(cardItem);
    }

    /**
     * 修改卡预设项
     *
     * @param cardItem 卡预设项
     * @return 结果
     */
    @Override
    public int updateCardItem(CardItem cardItem) {
        return cardItemMapper.updateCardItem(cardItem);
    }

    /**
     * 批量删除卡预设项
     *
     * @param ids 需要删除的卡预设项主键
     * @return 结果
     */
    @Override
    public int deleteCardItemByIds(Long[] ids) {
        return cardItemMapper.deleteCardItemByIds(ids);
    }

    /**
     * 删除卡预设项信息
     *
     * @param id 卡预设项主键
     * @return 结果
     */
    @Override
    public int deleteCardItemById(Long id) {
        return cardItemMapper.deleteCardItemById(id);
    }
}
