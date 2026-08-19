package com.betta.web.controller.eng;

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

    /** 根据 articleId 查询当前用户文章最好进度。 */
    @GetMapping("/progress/{articleId}")
    public AjaxResult progress(@PathVariable Long articleId)
    {
        return success(service.getProgress(articleId));
    }

    /** 根据 articleId 获取不包含正确答案的文章挑战。 */
    @GetMapping("/challenge/{articleId}")
    public AjaxResult challenge(@PathVariable Long articleId)
    {
        return success(service.getChallenge(articleId));
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
}
