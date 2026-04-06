package com.betta.system.mapper;

import java.util.List;
import com.betta.system.domain.CardHistory;

/**
 * 卡历史记录 数据层
 *
 * @author betta
 */
public interface CardHistoryMapper {
    /**
     * 查询卡历史记录
     *
     * @param id 卡历史记录主键
     * @return 卡历史记录
     */
    public CardHistory selectCardHistoryById(Long id);

    /**
     * 查询卡历史记录列表
     *
     * @param cardHistory 卡历史记录
     * @return 卡历史记录集合
     */
    public List<CardHistory> selectCardHistoryList(CardHistory cardHistory);

    /**
     * 根据账户ID查询历史记录
     *
     * @param accountId 账户ID
     * @return 卡历史记录集合
     */
    public List<CardHistory> selectCardHistoryByAccountId(Long accountId);

    /**
     * 新增卡历史记录
     *
     * @param cardHistory 卡历史记录
     * @return 结果
     */
    public int insertCardHistory(CardHistory cardHistory);

    /**
     * 删除卡历史记录
     *
     * @param id 卡历史记录主键
     * @return 结果
     */
    public int deleteCardHistoryById(Long id);

    /**
     * 批量删除卡历史记录
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCardHistoryByIds(Long[] ids);
}
