package com.betta.message.service.impl;

import com.betta.message.dto.AddCardDTO;
import com.betta.message.dto.CommandDTO;
import com.betta.message.dto.QueryBalanceDTO;
import com.betta.message.service.ILLMParseService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 解析实现：内置简单规则 + 可扩展为调用大模型 API
 * 示例规则：豆芽加卡20张 -> add_card(target=豆芽, quantity=20)；启动xxx任务 -> start_task(taskName=xxx)
 *
 * @author betta
 */
@Slf4j
@Service
public class LLMParseServiceImpl implements ILLMParseService {

    /** 加卡：xxx加卡N张 / xxx 加卡 N */
    private static final Pattern ADD_CARD_PATTERN = Pattern.compile("(豆芽|桐桐)\\s*(.*?)\\s*(加卡|加|消费|扣|扣卡)\\s*(\\d+)\\s*(?:张|元|张卡)");
    /** 启动任务：启动xxx / 马上跑xxx */
    private static final Pattern START_TASK_PATTERN = Pattern.compile("(?:启动|运行|执行|马上跑一下?)\\s*(.+)");
    /** 查询余额：有多少张卡 / 查询余额 / 卡数 */
    private static final Pattern QUERY_BALANCE_PATTERN = Pattern.compile("(?:现在|当前)?(?:有多少|剩余|查询|还剩|余额)?(?:多少张卡|多少卡|卡数|余额|张卡|多少元|元|是多少)");

    @Override
    public CommandDTO parse(String rawText) {
        if (StringUtils.isBlank(rawText)) {
            return unknown();
        }
        String text = rawText.trim();

        // 1) 加卡
        Matcher addCard = ADD_CARD_PATTERN.matcher(text);
        if (addCard.find()) {
            AddCardDTO dto = new AddCardDTO();
            dto.setIntent("add_card");
            dto.setAccount(addCard.group(1).trim());
            dto.setConent(addCard.group(2).trim());
            try {
                dto.setQuantity(Integer.parseInt(addCard.group(4)));
            } catch (NumberFormatException e) {
                dto.setQuantity(0);
            }
            String action = addCard.group(3).trim();
            if (action.equals("消费")||action.equals("扣")||action.equals("扣卡")) {
                dto.setQuantity(-1*dto.getQuantity());
            }
            return dto;
        }

        // 2) 查询余额
        Matcher queryBalance = QUERY_BALANCE_PATTERN.matcher(text);
        if (queryBalance.find()) {
            QueryBalanceDTO dto = new QueryBalanceDTO();
            dto.setIntent("query_balance");
            dto.setQueryType("all");
            return dto;
        }

        // 可在此处调用真实 LLM API，将 prompt 和 text 发送，解析返回的 JSON 为 CommandDTO
        // return callRealLLM(text);
        return unknown();
    }

    /**
     * 返回未知意图
     *
     * @return CommandDTO intent=unknown
     */
    private static CommandDTO unknown() {
        CommandDTO dto = new CommandDTO();
        dto.setIntent("unknown");
        return dto;
    }

    public static void main(String[] args) {
//        String text = "豆芽加卡20张";
//        System.out.println(ADD_CARD_PATTERN.matcher(text).find());
//        text = "今天天气怎么样";
//        System.out.println(ADD_CARD_PATTERN.matcher(text).find());
//        text = "今天天气怎么样,桐桐消费20元";
//        System.out.println(ADD_CARD_PATTERN.matcher(text).find());

        String text = "豆芽买笔加卡8张，今天天气不错";
        Matcher matcher = ADD_CARD_PATTERN.matcher(text);

        if (matcher.find()) { // 部分匹配用find()，千万别用matches()
            String name = matcher.group(1);    // 人名：豆芽
            String desc = matcher.group(2);    // 描述：买笔
            String action = matcher.group(3);  // 动作：加卡
            String num = matcher.group(4);     // 数字：8
            System.out.println(name + " | " + desc + " | " + action + " | " + num);
        }
    }
}
