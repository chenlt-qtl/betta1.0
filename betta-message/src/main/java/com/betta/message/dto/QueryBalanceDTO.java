package com.betta.message.dto;

import lombok.Data;

/**
 * 查询余额参数
 *
 * @author betta
 */
@Data
public class QueryBalanceDTO extends CommandDTO {

    /** 查询类型：all-所有账户, specific-指定账户 */
    private String queryType;

    /** 指定的账户名称（可选） */
    private String account;
}
