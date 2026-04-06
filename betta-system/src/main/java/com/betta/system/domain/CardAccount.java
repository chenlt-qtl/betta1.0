package com.betta.system.domain;

import com.betta.common.annotation.Excel;
import com.betta.common.core.domain.BaseEntity;

/**
 * 卡账户对象 card_account
 *
 * @author betta
 */
public class CardAccount extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 人名 */
    @Excel(name = "人名")
    private String name;

    /** 当前剩余卡数 */
    @Excel(name = "当前剩余卡数")
    private Integer balance;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getBalance() {
        return balance;
    }

    public void setBalance(Integer balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "CardAccount{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", balance=" + balance +
                '}';
    }
}
