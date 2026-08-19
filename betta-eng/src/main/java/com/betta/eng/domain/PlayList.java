package com.betta.eng.domain;

import com.betta.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 用户播放列表实体，保存逗号分隔的文章或句子主键集合。
 */
@Data
public class PlayList extends BaseEntity {
    private static final long serialVersionUID = 1L;
    /** 播放列表主键。 */
    private Long id;
    /** 所属用户名。 */
    private String userName;
    /** 逗号分隔的播放项主键。 */
    private String sentenceIds;
}
