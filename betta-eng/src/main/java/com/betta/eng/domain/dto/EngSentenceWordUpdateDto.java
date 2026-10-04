package com.betta.eng.domain.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 句子选词保存请求，wordIds 始终为规范词主键。 */
@Data
public class EngSentenceWordUpdateDto
{
    private List<Long> wordIds = new ArrayList<>();
}
