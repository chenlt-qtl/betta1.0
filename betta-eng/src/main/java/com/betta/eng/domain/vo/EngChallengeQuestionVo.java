package com.betta.eng.domain.vo;

import java.util.List;
import lombok.Data;

/**
 * 闯关题目展示对象；该对象刻意不包含正确答案，避免挑战接口泄露答案。
 */
@Data
public class EngChallengeQuestionVo {
    /** 题目标识，格式为类型与数据主键的组合。 */
    private String questionId;
    /** 题型，目前支持看词选中文、看中文选英文、句子挖空选词和句子挖空填词。 */
    private String type;
    /** 题目提示文本。 */
    private String prompt;
    /** 可选答案集合，两类选择题及句子挖空选词题返回候选项，句子挖空填词题为空。 */
    private List<String> options;
    /** 单词发音音频地址，看中文选英文题和句子填词题按需返回。 */
    private String audioUrl;
    /** 填词题正确单词的字母数，其他题型为空。 */
    private Integer answerLength;
}
