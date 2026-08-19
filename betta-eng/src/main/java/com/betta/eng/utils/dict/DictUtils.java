package com.betta.eng.utils.dict;

import org.springframework.stereotype.Component;
import com.betta.eng.domain.vo.EngWordVo;

/** 词典查询门面，保持先爱词霸、失败后有道的调用语义。 */
@Component
public class DictUtils
{
    private final IcibaUtils icibaUtils;
    private final YouDaoUtils youDaoUtils;

    /** 创建词典门面；参数依次为主查询与回退查询。 */
    public DictUtils(IcibaUtils icibaUtils, YouDaoUtils youDaoUtils)
    {
        this.icibaUtils = icibaUtils;
        this.youDaoUtils = youDaoUtils;
    }

    /** 查询 wordName；主词典失败时使用有道回退并返回结果。 */
    public EngWordVo getWord(String wordName)
    {
        try
        {
            return icibaUtils.getWord(wordName);
        }
        catch (RuntimeException ignored)
        {
            return youDaoUtils.getWord(wordName);
        }
    }
}
