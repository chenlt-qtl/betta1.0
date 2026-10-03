package com.betta.eng.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 商品兑换结果。 */
@Data
@AllArgsConstructor
public class MallExchangeResultVo
{
    /** 新增兑换记录主键。 */
    private Long recordId;
    /** 本次消耗金币。 */
    private Long coinCost;
    /** 兑换完成后的金币余额。 */
    private Long coinBalance;
}
