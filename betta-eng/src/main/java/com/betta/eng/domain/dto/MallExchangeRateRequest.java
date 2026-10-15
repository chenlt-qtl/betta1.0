package com.betta.eng.domain.dto;

import java.math.BigDecimal;
import lombok.Data;

/** 人民币兑换金币汇率更新请求。 */
@Data
public class MallExchangeRateRequest
{
    /** 一元人民币可兑换的金币数。 */
    private BigDecimal rate;
}
