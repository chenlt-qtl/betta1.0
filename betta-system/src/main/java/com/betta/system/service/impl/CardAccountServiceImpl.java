package com.betta.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.betta.common.constant.UserConstants;
import com.betta.common.utils.StringUtils;
import com.betta.system.domain.CardAccount;
import com.betta.system.mapper.CardAccountMapper;
import com.betta.system.service.ICardAccountService;

/**
 * 卡账户 服务层实现
 *
 * @author betta
 */
@Service
public class CardAccountServiceImpl implements ICardAccountService {
    @Autowired
    private CardAccountMapper cardAccountMapper;

    /**
     * 查询卡账户
     *
     * @param id 卡账户主键
     * @return 卡账户
     */
    @Override
    public CardAccount selectCardAccountById(Long id) {
        return cardAccountMapper.selectCardAccountById(id);
    }

    /**
     * 根据名称查询卡账户
     *
     * @param name 人名
     * @return 卡账户
     */
    @Override
    public CardAccount selectCardAccountByName(String name) {
        return cardAccountMapper.selectCardAccountByName(name);
    }

    /**
     * 查询卡账户列表
     *
     * @param cardAccount 卡账户
     * @return 卡账户
     */
    @Override
    public List<CardAccount> selectCardAccountList(CardAccount cardAccount) {
        return cardAccountMapper.selectCardAccountList(cardAccount);
    }

    /**
     * 新增卡账户
     *
     * @param cardAccount 卡账户
     * @return 结果
     */
    @Override
    public int insertCardAccount(CardAccount cardAccount) {
        return cardAccountMapper.insertCardAccount(cardAccount);
    }

    /**
     * 修改卡账户
     *
     * @param cardAccount 卡账户
     * @return 结果
     */
    @Override
    public int updateCardAccount(CardAccount cardAccount) {
        return cardAccountMapper.updateCardAccount(cardAccount);
    }

    /**
     * 批量删除卡账户
     *
     * @param ids 需要删除的卡账户主键
     * @return 结果
     */
    @Override
    public int deleteCardAccountByIds(Long[] ids) {
        return cardAccountMapper.deleteCardAccountByIds(ids);
    }

    /**
     * 删除卡账户信息
     *
     * @param id 卡账户主键
     * @return 结果
     */
    @Override
    public int deleteCardAccountById(Long id) {
        return cardAccountMapper.deleteCardAccountById(id);
    }

    /**
     * 校验人名是否唯一
     *
     * @param cardAccount 卡账户信息
     * @return 结果
     */
    @Override
    public boolean checkNameUnique(CardAccount cardAccount) {
        Long accountId = StringUtils.isNull(cardAccount.getId()) ? -1L : cardAccount.getId();
        CardAccount info = cardAccountMapper.selectCardAccountByName(cardAccount.getName());
        if (StringUtils.isNotNull(info) && info.getId().longValue() != accountId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
}
