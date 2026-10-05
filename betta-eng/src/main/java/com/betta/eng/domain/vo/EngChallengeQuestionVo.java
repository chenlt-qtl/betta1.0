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
    /** 题型，包含知识题、独立拼写题以及可选的英文单词跟读题。 */
    private String type;
    /** 题目提示文本。 */
    private String prompt;
    /** 可选答案集合；选择题返回答案候选，拼写题返回十个候选字母。 */
    private List<String> options;
    /** 目标单词发音音频地址，各题型按需返回。 */
    private String audioUrl;
    /** 拼写题需要填写的字母数，其他题型为空。 */
    private Integer answerLength;
    /** 跟读题对应的规范单词，其他题型为空。 */
    private String word;
}
