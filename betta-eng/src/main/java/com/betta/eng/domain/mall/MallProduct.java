package com.betta.eng.domain.mall;

import com.betta.common.core.domain.BaseEntity;
import lombok.Data;

/** 商城商品实体，图片地址按英文逗号分隔并保持展示顺序。 */
@Data
public class MallProduct extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 商品主键。 */
    private Long id;
    /** 商品名称。 */
    private String name;
    /** 一至五张商品图片地址，使用英文逗号分隔。 */
    private String images;
    /** Markdown 格式商品描述。 */
    private String description;
    /** 兑换所需金币。 */
    private Long coinPrice;
}
