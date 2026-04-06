package com.betta.system.domain;

import com.betta.common.annotation.Excel;
import com.betta.common.core.domain.BaseEntity;

/**
 * 卡历史记录对象 card_history
 *
 * @author betta
 */
public class CardHistory extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 关联 card_account.id */
    @Excel(name = "账户ID")
    private Long accountId;

    /** 变动值(正数为加卡,负数为扣卡) */
    @Excel(name = "变动值")
    private Integer changeValue;

    /** 变动后剩余 */
    @Excel(name = "变动后剩余")
    private Integer remainValue;

    /** 原因/内容 */
    @Excel(name = "原因")
    private String content;

    /** 账户名称(非数据库字段,用于显示) */
    @Excel(name = "账户名称")
    private String accountName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Integer getChangeValue() {
        return changeValue;
    }

    public void setChangeValue(Integer changeValue) {
        this.changeValue = changeValue;
    }

    public Integer getRemainValue() {
        return remainValue;
    }

    public void setRemainValue(Integer remainValue) {
        this.remainValue = remainValue;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public String toString() {
        return "CardHistory{" +
                "id=" + id +
                ", accountId=" + accountId +
                ", changeValue=" + changeValue +
                ", remainValue=" + remainValue +
                ", content='" + content + '\'' +
                '}';
    }
}
