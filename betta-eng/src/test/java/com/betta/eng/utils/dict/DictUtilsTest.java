package com.betta.eng.utils.dict;

import com.betta.common.exception.ServiceException;
import com.betta.eng.domain.EngWord;
import com.betta.eng.domain.vo.EngWordVo;
import com.betta.eng.mapper.EngWordMapper;
import java.lang.reflect.Proxy;
import java.util.List;

/** 本地数据库词典查询的独立回归入口。 */
public class DictUtilsTest
{
    /** 验证正式词、别名、无音频词和未收录词。 */
    public static void main(String[] args)
    {
        shouldReturnLocalWordWithoutAudio();
        shouldResolveAliasThroughMapper();
        shouldRejectMissingWord();
        shouldRejectWordWithoutDisplayableDefinition();
    }

    /** 数据库词条只要释义有效，即使没有音频也应直接返回。 */
    private static void shouldReturnLocalWordWithoutAudio()
    {
        DictUtils dictUtils = new DictUtils(mapperReturning(localWord("colour", "noun 颜色")));

        EngWordVo result = dictUtils.getWord("colour");

        assertEquals("colour", result.getWordName(), "应返回数据库正式词条");
    }

    /** Mapper 按别名返回正式词条时，门面应保持正式词头。 */
    private static void shouldResolveAliasThroughMapper()
    {
        DictUtils dictUtils = new DictUtils(mapperReturning(localWord("color", "noun 颜色")));

        EngWordVo result = dictUtils.getWord("colour");

        assertEquals("color", result.getWordName(), "别名应解析为正式词头");
    }

    /** 数据库没有正式词或别名时应直接报告未收录。 */
    private static void shouldRejectMissingWord()
    {
        assertMissing(new DictUtils(mapperReturning(null)), "missing");
    }

    /** 旧 JSON 或空释义不能作为可展示的本地词典结果。 */
    private static void shouldRejectWordWithoutDisplayableDefinition()
    {
        assertMissing(new DictUtils(mapperReturning(localWord("empty", null))), "empty");
        assertMissing(new DictUtils(mapperReturning(localWord("legacy", "[{\"part\":\"noun\"}]"))), "legacy");
    }

    private static void assertMissing(DictUtils dictUtils, String wordName)
    {
        try
        {
            dictUtils.getWord(wordName);
            throw new AssertionError("未收录词应抛出业务异常：" + wordName);
        }
        catch (ServiceException exception)
        {
            assertEquals("词典未查询到单词：" + wordName, exception.getMessage(), "异常信息应包含查询词");
        }
    }

    private static EngWord localWord(String wordName, String acceptation)
    {
        EngWord word = new EngWord();
        word.setId(7L);
        word.setWordName(wordName);
        word.setAcceptation(acceptation);
        return word;
    }

    private static EngWordMapper mapperReturning(EngWord word)
    {
        return (EngWordMapper) Proxy.newProxyInstance(EngWordMapper.class.getClassLoader(),
                new Class<?>[] { EngWordMapper.class }, (proxy, method, args) -> {
                    if ("selectEngWordByWordName".equals(method.getName()))
                    {
                        return word == null ? List.of() : List.of(word);
                    }
                    return null;
                });
    }

    private static void assertEquals(Object expected, Object actual, String message)
    {
        if (expected == null ? actual != null : !expected.equals(actual))
        {
            throw new AssertionError(message + "，期望=" + expected + "，实际=" + actual);
        }
    }
}
