package com.betta.eng.domain.mall;

import com.betta.common.core.domain.BaseEntity;
import lombok.Data;

/** 金币消费记录，保存商城兑换或机器人普通消费的商品快照。 */
@Data
public class MallExchangeRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 兑换记录主键。 */
    private Long id;
    /** 兑换用户主键。 */
    private Long userId;
    /** 用户账号，仅用于管理端展示与筛选。 */
    private String userName;
    /** 用户昵称，仅用于管理端展示。 */
    private String nickName;
    /** 原商品主键，值为 0 时表示机器人录入的普通消费。 */
    private Long productId;
    /** 兑换时商品名称快照。 */
    private String productName;
    /** 兑换时商品图片快照；机器人普通消费使用空字符串。 */
    private String productImages;
    /** 兑换时金币价格快照。 */
    private Long coinPrice;
}
