package com.betta.eng.domain.mall;

import com.betta.common.core.domain.BaseEntity;
import lombok.Data;

/** 商品兑换记录，保存兑换时商品快照以避免商品删除影响历史展示。 */
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
    /** 原商品主键，商品删除后仍保留其值。 */
    private Long productId;
    /** 兑换时商品名称快照。 */
    private String productName;
    /** 兑换时商品图片快照。 */
    private String productImages;
    /** 兑换时金币价格快照。 */
    private Long coinPrice;
}
