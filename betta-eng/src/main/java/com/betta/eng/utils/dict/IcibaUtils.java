package com.betta.eng.utils.dict;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.betta.common.constant.Constants;
import com.betta.common.exception.ServiceException;
import com.betta.common.utils.StringUtils;
import com.betta.common.utils.http.HttpUtils;
import com.betta.eng.domain.EngIcibaSentence;
import com.betta.eng.domain.vo.EngWordVo;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 爱词霸词典查询适配器，使用其 JSON 接口解析音标、释义与例句。
 */
@Component
public class IcibaUtils
{
    private static final String URL = "https://dict-mobile.iciba.com/interface/index.php";

    /**
     * 查询 wordName 并返回词典信息。
     *
     * @param wordName 规范化英文单词
     * @return 词典单词详情
     */
    public EngWordVo getWord(String wordName)
    {
        try
        {
            String body = HttpUtils.sendGet(URL, "c=word&m=getsuggest&nums=1&is_need_mean=1&word=" + wordName,
                    Constants.UTF8);
            if (StringUtils.isEmpty(body))
            {
                throw new ServiceException("爱词霸词典未返回数据");
            }
            return parse(body, wordName);
        }
        catch (ServiceException exception)
        {
            throw exception;
        }
        catch (Exception exception)
        {
            throw new ServiceException("解析爱词霸词典失败：" + exception.getMessage());
        }
    }

    /** 解析 data 对应 JSON；wordName 为查询词，返回词典对象。 */
    private EngWordVo parse(String data, String wordName)
    {
        JSONObject root = JSONObject.parseObject(data);
        JSONArray messages = root.getJSONArray("message");
        if (messages == null || messages.isEmpty())
        {
            throw new ServiceException("爱词霸未查询到单词：" + wordName);
        }
        JSONObject item = messages.getJSONObject(0);
        EngWordVo word = new EngWordVo();
        word.setWordName(wordName);
        word.setPhonetics(firstNotBlank(item.getString("ph_am"), item.getString("ph_en")));
        word.setPhMp3(firstNotBlank(item.getString("ph_am_mp3"), item.getString("ph_en_mp3")));
        word.setAcceptation(parseMeans(item));
        word.setIcibaSentenceList(parseSentences(item));
        if (StringUtils.isEmpty(word.getAcceptation()))
        {
            throw new ServiceException("爱词霸未返回有效释义：" + wordName);
        }
        return word;
    }

    /** 解析 item 中词性与释义并返回拼接文本。 */
    private String parseMeans(JSONObject item)
    {
        JSONArray parts = item.getJSONArray("parts");
        if (parts == null)
        {
            return item.getString("means");
        }
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < parts.size(); index++)
        {
            JSONObject part = parts.getJSONObject(index);
            String type = part.getString("part");
            JSONArray means = part.getJSONArray("means");
            if (StringUtils.isNotEmpty(type))
            {
                result.append(type).append(' ');
            }
            if (means != null)
            {
                result.append(String.join("；", means.toJavaList(String.class)));
            }
            result.append('|');
        }
        return result.toString();
    }

    /** 解析 item 中可选例句并返回集合。 */
    private List<EngIcibaSentence> parseSentences(JSONObject item)
    {
        List<EngIcibaSentence> result = new ArrayList<>();
        JSONArray sentences = item.getJSONArray("sentence");
        if (sentences == null)
        {
            return result;
        }
        for (int index = 0; index < sentences.size(); index++)
        {
            JSONObject source = sentences.getJSONObject(index);
            EngIcibaSentence sentence = new EngIcibaSentence();
            sentence.setOrig(source.getString("Network_en"));
            sentence.setTrans(source.getString("Network_cn"));
            result.add(sentence);
        }
        return result;
    }

    /** 返回 first 与 second 中首个非空文本。 */
    private String firstNotBlank(String first, String second)
    {
        return StringUtils.isNotEmpty(first) ? first : second;
    }
}
