package com.betta.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.betta.system.domain.CardHistory;
import com.betta.system.mapper.CardHistoryMapper;
import com.betta.system.service.ICardHistoryService;

/**
 * 卡历史记录 服务层实现
 *
 * @author betta
 */
@Service
public class CardHistoryServiceImpl implements ICardHistoryService {
    @Autowired
    private CardHistoryMapper cardHistoryMapper;

    /**
     * 查询卡历史记录
     *
     * @param id 卡历史记录主键
     * @return 卡历史记录
     */
    @Override
    public CardHistory selectCardHistoryById(Long id) {
        return cardHistoryMapper.selectCardHistoryById(id);
    }

    /**
     * 查询卡历史记录列表
     *
     * @param cardHistory 卡历史记录
     * @return 卡历史记录
     */
    @Override
    public List<CardHistory> selectCardHistoryList(CardHistory cardHistory) {
        return cardHistoryMapper.selectCardHistoryList(cardHistory);
    }

    /**
     * 根据账户ID查询历史记录
     *
     * @param accountId 账户ID
     * @return 卡历史记录集合
     */
    @Override
    public List<CardHistory> selectCardHistoryByAccountId(Long accountId) {
        return cardHistoryMapper.selectCardHistoryByAccountId(accountId);
    }

    /**
     * 新增卡历史记录
     *
     * @param cardHistory 卡历史记录
     * @return 结果
     */
    @Override
    public int insertCardHistory(CardHistory cardHistory) {
        return cardHistoryMapper.insertCardHistory(cardHistory);
    }

    /**
     * 批量删除卡历史记录
     *
     * @param ids 需要删除的卡历史记录主键
     * @return 结果
     */
    @Override
    public int deleteCardHistoryByIds(Long[] ids) {
        return cardHistoryMapper.deleteCardHistoryByIds(ids);
    }

    /**
     * 删除卡历史记录信息
     *
     * @param id 卡历史记录主键
     * @return 结果
     */
    @Override
    public int deleteCardHistoryById(Long id) {
        return cardHistoryMapper.deleteCardHistoryById(id);
    }
}
