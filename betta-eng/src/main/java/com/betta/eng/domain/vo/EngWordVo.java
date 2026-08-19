package com.betta.eng.domain.vo;

import com.betta.eng.domain.EngIcibaSentence;
import com.betta.eng.domain.EngWord;
import java.util.List;
import lombok.Data;

/**
 * 单词详情展示对象，聚合文章关系、词典例句和自定义例句。
 */
@Data
public class EngWordVo extends EngWord {
    private static final long serialVersionUID = 1L;
    /** 当前文章关系主键。 */
    private Long relId;
    /** 词典例句集合。 */
    private List<EngIcibaSentence> icibaSentenceList;
    /** 用户文章中的自定义例句集合。 */
    private List<SentenceVo> sentenceList;
}
