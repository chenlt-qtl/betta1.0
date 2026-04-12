package com.betta.robot.dto;

import lombok.Data;

/**
 * 加卡参数
 *
 * @author betta
 */
@Data
public class AddCardDTO extends CommandDTO{

    /** 对象/名称，如 豆芽 */
    private String account;

    /** 动作，如 加卡、扣卡、消费 */
    private String action;

    /** 数量 */
    private Integer quantity;
    /** 内容 */
    private String conent;
}
