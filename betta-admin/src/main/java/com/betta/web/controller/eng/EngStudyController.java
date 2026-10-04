package com.betta.web.controller.eng;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.betta.common.core.controller.BaseController;
import com.betta.common.core.domain.AjaxResult;
import com.betta.common.core.page.TableDataInfo;
import com.betta.common.exception.ServiceException;
import com.betta.eng.domain.EngWrongWord;
import com.betta.eng.domain.dto.EngChallengeCheckDto;
import com.betta.eng.domain.dto.EngChallengeSubmitDto;
import com.betta.eng.service.IEngStudyService;

/**
 * 游戏化英语学习控制器，只接收参数、调用学习服务并返回统一响应。
 */
@RestController
@RequestMapping("/eng/study")
public class EngStudyController extends BaseController
{
    private final IEngStudyService service;

    /**
     * 创建学习控制器。
     *
     * @param service 游戏化学习业务服务
     */
    public EngStudyController(IEngStudyService service)
    {
        this.service = service;
    }

    /** 查询当前用户学习汇总。 */
    @GetMapping("/summary")
    public AjaxResult summary()
    {
        return success(service.getSummary());
    }

    /** 查询文章永久关卡地图。 */
    @GetMapping("/articles/{articleId}/levels")
    public AjaxResult levels(@PathVariable Long articleId)
    {
        return success(service.getArticleLevels(articleId));
    }

    /** 查询全局已学词和建议复习状态。 */
    @GetMapping("/review")
    public AjaxResult review()
    {
        return success(service.getReviewOverview());
    }

    /** 按 NEW 或 REVIEW 模式获取不包含正确答案的固定词集挑战。 */
    @GetMapping("/challenge")
    public AjaxResult challenge(@RequestParam String mode,
            @RequestParam(required = false) Long articleId,
            @RequestParam(required = false) Integer levelNo,
            @RequestParam(required = false) String wordIds)
    {
        return success(service.getChallenge(mode, articleId, levelNo, parseWordIds(wordIds)));
    }

    /**
     * 即时校验 request 中的单题答案并返回正确状态及正确答案。
     *
     * @param request 单题判题请求，包含文章主键、题目标识和当前答案
     * @return 统一响应，其中 data 为单题判定结果
     */
    @PostMapping("/challenge/check")
    public AjaxResult check(@RequestBody EngChallengeCheckDto request)
    {
        // 参数合法性与题目归属由业务层统一校验，控制器仅转发请求并封装响应。
        return success(service.checkChallengeAnswer(request));
    }

    /** 提交 request 中的闯关答案并返回服务端计分结果。 */
    @PostMapping("/challenge/submit")
    public AjaxResult submit(@RequestBody EngChallengeSubmitDto request)
    {
        return success(service.submitChallenge(request));
    }

    /**
     * 分页查询当前用户错词。
     *
     * @param mastered 可选掌握状态
     * @param wordName 可选单词文本
     * @return 分页错词数据
     */
    @GetMapping("/wrong/list")
    public TableDataInfo wrongList(@RequestParam(required = false) Boolean mastered,
            @RequestParam(required = false) String wordName)
    {
        EngWrongWord condition = new EngWrongWord();
        condition.setMastered(mastered == null ? null : (mastered ? 1 : 0));
        condition.setWordName(wordName);
        startPage();
        List<EngWrongWord> list = service.selectWrongWordList(condition);
        return getDataTable(list);
    }

    /** 将 id 对应且归属当前用户的错词标记为已掌握。 */
    @PutMapping("/wrong/mastered/{id}")
    public AjaxResult mastered(@PathVariable Long id)
    {
        return toAjax(service.markWrongWordMastered(id));
    }

    /** 查询当前用户一次测试对应的规范词明细。 */
    @GetMapping("/records/{recordId}/words")
    public AjaxResult recordWords(@PathVariable Long recordId)
    {
        return success(service.selectRecordWords(recordId));
    }

    /** 将 REVIEW 主动选择的逗号主键解析为有序去重集合。 */
    private List<Long> parseWordIds(String value)
    {
        if (value == null || value.isBlank()) return List.of();
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        try
        {
            for (String item : value.split(",")) ids.add(Long.valueOf(item.trim()));
        }
        catch (NumberFormatException exception)
        {
            throw new ServiceException("复习单词主键格式错误");
        }
        return new ArrayList<>(ids);
    }
}
