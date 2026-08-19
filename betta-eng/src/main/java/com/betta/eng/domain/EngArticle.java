package com.betta.eng.domain;

import com.betta.common.annotation.Excel;
import com.betta.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 英语文章实体，承载文章基础信息及文章详情中的句子集合。
 */
@Data
public class EngArticle extends BaseEntity {
    private static final long serialVersionUID = 1L;
    /** 文章主键。 */
    private Long id;
    /** 所属分组主键。 */
    private Long groupId;
    /** 所属分组名称，仅用于查询展示。 */
    private String groupName;
    /** 封面图片地址。 */
    @Excel(name = "图片位置")
    private String picture;
    /** 文章音频地址。 */
    @Excel(name = "音频位置")
    private String mp3;
    /** 文章标题。 */
    @Excel(name = "标题")
    private String title;
    /** 人工备注。 */
    @Excel(name = "手工注释")
    private String comment;
    /** 文章状态。 */
    private String status;
}
