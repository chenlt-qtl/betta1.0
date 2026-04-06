package com.betta.system.mapper;

import java.util.List;
import com.betta.system.domain.CardAccount;

/**
 * 卡账户 数据层
 *
 * @author betta
 */
public interface CardAccountMapper {
    /**
     * 查询卡账户
     *
     * @param id 卡账户主键
     * @return 卡账户
     */
    public CardAccount selectCardAccountById(Long id);

    /**
     * 根据名称查询卡账户
     *
     * @param name 人名
     * @return 卡账户
     */
    public CardAccount selectCardAccountByName(String name);

    /**
     * 查询卡账户列表
     *
     * @param cardAccount 卡账户
     * @return 卡账户集合
     */
    public List<CardAccount> selectCardAccountList(CardAccount cardAccount);

    /**
     * 新增卡账户
     *
     * @param cardAccount 卡账户
     * @return 结果
     */
    public int insertCardAccount(CardAccount cardAccount);

    /**
     * 修改卡账户
     *
     * @param cardAccount 卡账户
     * @return 结果
     */
    public int updateCardAccount(CardAccount cardAccount);

    /**
     * 删除卡账户
     *
     * @param id 卡账户主键
     * @return 结果
     */
    public int deleteCardAccountById(Long id);

    /**
     * 批量删除卡账户
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCardAccountByIds(Long[] ids);
}
