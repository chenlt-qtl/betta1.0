package com.betta.robot.service;

import com.betta.common.constant.UserConstants;
import com.betta.common.core.domain.entity.SysUser;
import com.betta.common.exception.ServiceException;
import com.betta.eng.domain.mall.MallExchangeRecord;
import com.betta.eng.mapper.EngCoinWalletMapper;
import com.betta.eng.mapper.MallExchangeRecordMapper;
import com.betta.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 机器人金币扣减服务，负责用户唯一性校验和钱包原子扣减。 */
@Service
public class CoinDeductService {

    private final SysUserMapper userMapper;
    private final EngCoinWalletMapper coinWalletMapper;
    private final MallExchangeRecordMapper exchangeRecordMapper;

    /** 创建金币扣减服务。 */
    public CoinDeductService(SysUserMapper userMapper, EngCoinWalletMapper coinWalletMapper,
                            MallExchangeRecordMapper exchangeRecordMapper) {
        this.userMapper = userMapper;
        this.coinWalletMapper = coinWalletMapper;
        this.exchangeRecordMapper = exchangeRecordMapper;
    }

    /**
     * 按昵称扣减金币并返回最新余额。
     *
     * @param nickname 用户昵称
     * @param content 消费内容
     * @param coinCost 扣减数量，必须为正整数
     * @return 扣减后的金币余额
     */
    @Transactional
    public long deductCoin(String nickname, String content, long coinCost) {
        if (coinCost <= 0) {
            throw new ServiceException("金币数量必须为正整数");
        }

        List<SysUser> users = userMapper.selectUsersByNickName(nickname);
        if (users == null || users.isEmpty()) {
            throw new ServiceException("用户不存在");
        }
        if (users.size() > 1) {
            throw new ServiceException("用户昵称不唯一");
        }

        SysUser user = users.get(0);
        if (!UserConstants.NORMAL.equals(user.getStatus())) {
            throw new ServiceException("用户已停用");
        }

        // 条件更新同时覆盖钱包不存在和余额不足，避免并发扣减产生负余额。
        if (coinWalletMapper.decreaseCoinBalance(user.getUserId(), coinCost, "robot") == 0) {
            throw new ServiceException("金币余额不足");
        }
        long balance = coinWalletMapper.selectCoinBalance(user.getUserId());

        MallExchangeRecord record = new MallExchangeRecord();
        record.setUserId(user.getUserId());
        // 商品主键 0 表示机器人录入的普通消费，不关联商城商品。
        record.setProductId(0L);
        record.setProductName(content);
        record.setProductImages("");
        record.setCoinPrice(coinCost);
        record.setCreateBy("robot");
        if (exchangeRecordMapper.insertExchangeRecord(record) != 1) {
            throw new ServiceException("金币消费记录保存失败");
        }
        return balance;
    }
}
