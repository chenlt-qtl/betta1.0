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
    /** 数量 */
    private Integer quantity;
    /** 内容 */
    private String conent;
}
