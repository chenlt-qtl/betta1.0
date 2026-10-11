package com.betta.robot.service;

import com.betta.common.constant.UserConstants;
import com.betta.common.core.domain.entity.SysUser;
import com.betta.common.exception.ServiceException;
import com.betta.eng.mapper.EngCoinWalletMapper;
import com.betta.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 机器人金币扣减服务，负责用户唯一性校验和钱包原子扣减。 */
@Service
public class CoinDeductService {

    private final SysUserMapper userMapper;
    private final EngCoinWalletMapper coinWalletMapper;

    /** 创建金币扣减服务。 */
    public CoinDeductService(SysUserMapper userMapper, EngCoinWalletMapper coinWalletMapper) {
        this.userMapper = userMapper;
        this.coinWalletMapper = coinWalletMapper;
    }

    /**
     * 按昵称扣减金币并返回最新余额。
     *
     * @param nickname 用户昵称
     * @param coinCost 扣减数量，必须为正整数
     * @return 扣减后的金币余额
     */
    @Transactional
    public long deductCoin(String nickname, long coinCost) {
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
        return coinWalletMapper.selectCoinBalance(user.getUserId());
    }
}
