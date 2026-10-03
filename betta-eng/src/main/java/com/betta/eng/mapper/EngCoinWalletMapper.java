package com.betta.eng.mapper;

import org.apache.ibatis.annotations.Param;

/** 用户金币钱包数据访问接口。 */
public interface EngCoinWalletMapper {
    /** 查询用户金币余额；钱包尚未创建时返回零。 */
    long selectCoinBalance(Long userId);

    /** 原子增加用户金币；钱包不存在时创建，返回影响行数。 */
    int increaseCoinBalance(@Param("userId") Long userId, @Param("coinReward") long coinReward,
            @Param("username") String username);

    /** 余额充足时原子扣减金币；钱包不存在或余额不足时返回零。 */
    int decreaseCoinBalance(@Param("userId") Long userId, @Param("coinCost") long coinCost,
            @Param("username") String username);
}
