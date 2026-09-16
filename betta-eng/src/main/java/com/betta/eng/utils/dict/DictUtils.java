package com.betta.eng.utils.dict;

import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import com.betta.common.exception.ServiceException;
import com.betta.common.utils.StringUtils;
import com.betta.eng.domain.EngWord;
import com.betta.eng.domain.vo.EngWordVo;
import com.betta.eng.mapper.EngWordMapper;

/** 词典查询门面，统一返回包含中文释义的完整词典数据。 */
@Component
public class DictUtils
{
    private final EngWordMapper wordMapper;

    /** 创建只访问本地数据库的词典门面。 */
    public DictUtils(EngWordMapper wordMapper)
    {
        this.wordMapper = wordMapper;
    }

    /** 查询正式词头或别名；数据库无有效释义时直接报告未收录。 */
    public EngWordVo getWord(String wordName)
    {
        List<EngWord> localWords = wordMapper.selectEngWordByWordName(wordName);
        if (localWords == null || localWords.isEmpty() || !hasUsableDefinition(localWords.get(0)))
        {
            throw new ServiceException("词典未查询到单词：" + wordName);
        }
        EngWordVo result = new EngWordVo();
        BeanUtils.copyProperties(localWords.get(0), result);
        return result;
    }

    /** 只有可直接展示的非 JSON 释义才视为有效本地词典数据。 */
    private boolean hasUsableDefinition(EngWord word)
    {
        if (word == null || StringUtils.isEmpty(word.getAcceptation()))
        {
            return false;
        }
        String acceptation = word.getAcceptation().trim();
        return !acceptation.isEmpty() && !acceptation.startsWith("[") && !acceptation.startsWith("{");
    }
}
