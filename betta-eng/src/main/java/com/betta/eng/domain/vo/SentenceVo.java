package com.betta.eng.domain.vo;

import com.betta.eng.domain.EngSentence;
import lombok.Data;

/**
 * 句子展示对象，补充所属文章和分组信息。
 */
@Data
public class SentenceVo extends EngSentence {
    private static final long serialVersionUID = 1L;
    /** 文章名称。 */
    private String articleName;
    /** 分组名称。 */
    private String groupName;
    /** 分组主键。 */
    private Long groupId;
}
