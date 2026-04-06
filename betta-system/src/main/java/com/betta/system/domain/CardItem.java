package com.betta.system.domain;

import com.betta.common.annotation.Excel;
import com.betta.common.core.domain.BaseEntity;

/**
 * 卡预设项对象 card_item
 *
 * @author betta
 */
public class CardItem extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 类型(1-加卡项 2-扣卡项) */
    @Excel(name = "类型", readConverterExp = "1=加卡项,2=扣卡项")
    private Integer type;

    /** 变动值 */
    @Excel(name = "变动值")
    private Integer value;

    /** 名称 */
    @Excel(name = "名称")
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "CardItem{" +
                "id=" + id +
                ", type=" + type +
                ", value=" + value +
                ", name='" + name + '\'' +
                '}';
    }
}
