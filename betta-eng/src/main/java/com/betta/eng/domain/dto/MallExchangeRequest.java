package com.betta.eng.domain.dto;

import lombok.Data;

/** 商城兑换请求，携带用户确认时看到的金币价格。 */
@Data
public class MallExchangeRequest
{
    /** 用户确认时看到的金币价格。 */
    private Long expectedCoinPrice;
}
