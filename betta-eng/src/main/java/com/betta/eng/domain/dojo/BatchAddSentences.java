package com.betta.eng.domain.dojo;

import lombok.Data;

/**
 * 批量新增句子请求，按换行拆分句子文本并归属到指定文章。
 */
@Data
public class BatchAddSentences {
    /** 多行句子文本。 */
    private String sentenceStr;
    /** 目标文章主键。 */
    private Long articleId;
}
