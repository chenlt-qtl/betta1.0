package com.betta.eng.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.betta.common.utils.StringUtils;
import com.betta.eng.domain.EngSentence;

/** 英语句子时间格式工具，统一跟读音频片段格式。 */
public final class EngUtils
{
    private static final Pattern TIME_PATTERN = Pattern.compile("^(?:\\[)?(\\d+):(\\d+)");

    /** 工具类禁止实例化。 */
    private EngUtils()
    {
    }

    /** 为 sentence 生成或规范化 mp3Time；无可解析时间时保持原值。 */
    public static void genMp3Time(EngSentence sentence)
    {
        if (sentence == null)
        {
            return;
        }
        String value = sentence.getMp3Time();
        if (StringUtils.isEmpty(value) && StringUtils.isNotEmpty(sentence.getContent()))
        {
            Matcher matcher = TIME_PATTERN.matcher(sentence.getContent());
            if (matcher.find())
            {
                value = matcher.group(1) + ":" + matcher.group(2) + ","
                        + (sentence.getContent().length() < 60 ? 5 : 8);
            }
        }
        sentence.setMp3Time(transMp3Time(value));
    }

    /** 将 value 从“分:秒,时长”转换为“秒数,时长”并返回。 */
    public static String transMp3Time(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return value;
        }
        String[] parts = value.split(",", -1);
        if (parts.length < 2)
        {
            return value;
        }
        Matcher matcher = TIME_PATTERN.matcher(parts[0]);
        if (!matcher.find())
        {
            return value;
        }
        return (Integer.parseInt(matcher.group(1)) * 60 + Integer.parseInt(matcher.group(2))) + "," + parts[1];
    }
}
