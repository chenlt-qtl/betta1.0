package com.betta.eng.domain.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 句子选词弹窗数据。 */
@Data
public class EngSentenceWordOptionsVo
{
    private Long sentenceId;
    private Long articleId;
    private List<EngSentenceSegmentVo> segments = new ArrayList<>();
}
