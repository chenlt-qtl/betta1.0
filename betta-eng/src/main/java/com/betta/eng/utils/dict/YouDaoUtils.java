package com.betta.eng.utils.dict;

import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.betta.common.constant.Constants;
import com.betta.common.exception.ServiceException;
import com.betta.common.utils.StringUtils;
import com.betta.common.utils.http.HttpUtils;
import com.betta.eng.domain.vo.EngWordVo;

/** 有道公开词典查询适配器，解析单词释义作为爱词霸失败时的回退。 */
@Component
public class YouDaoUtils
{
    private static final String URL = "http://dict.youdao.com/suggest";

    /** 查询 wordName 并返回词典对象；无有效响应时抛出可读业务异常。 */
    public EngWordVo getWord(String wordName)
    {
        String body = HttpUtils.sendGet(URL, "q=" + wordName + "&num=1&doctype=json", Constants.UTF8);
        if (StringUtils.isEmpty(body))
        {
            throw new ServiceException("有道词典服务暂不可用");
        }
        try
        {
            JSONObject root = JSON.parseObject(body);
            JSONObject data = root.getJSONObject("data");
            JSONArray entries = data == null ? null : data.getJSONArray("entries");
            if (entries == null || entries.isEmpty())
            {
                throw new ServiceException("有道词典未查询到单词：" + wordName);
            }
            JSONObject item = entries.getJSONObject(0);
            EngWordVo word = new EngWordVo();
            word.setWordName(wordName);
            word.setAcceptation(item.getString("explain"));
            return word;
        }
        catch (ServiceException exception)
        {
            throw exception;
        }
        catch (Exception exception)
        {
            throw new ServiceException("解析有道词典失败：" + exception.getMessage());
        }
    }
}
