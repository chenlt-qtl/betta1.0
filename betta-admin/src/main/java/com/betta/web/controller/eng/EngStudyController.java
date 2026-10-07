package com.betta.web.controller.eng;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.betta.common.annotation.RateLimiter;
import com.betta.common.enums.LimitType;
import com.betta.common.core.controller.BaseController;
import com.betta.common.core.domain.AjaxResult;
import com.betta.common.core.page.TableDataInfo;
import com.betta.common.exception.ServiceException;
import com.betta.eng.domain.EngWrongWord;
import com.betta.eng.domain.dto.EngChallengeCheckDto;
import com.betta.eng.domain.dto.EngChallengeSubmitDto;
import com.betta.eng.domain.dto.EngChallengeSettingUpdateDto;
import com.betta.eng.domain.dto.EngPronunciationAssessDto;
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
    public AjaxResult levels(@PathVariable Long articleId, HttpServletRequest request)
    {
        var levels = service.getArticleLevels(articleId);
        levels.setPronunciationEnabled(Boolean.TRUE.equals(levels.getPronunciationEnabled()) && isSecure(request));
        return success(levels);
    }

    /** 查询全局已学词和建议复习状态。 */
    @GetMapping("/review")
    public AjaxResult review()
    {
        return success(service.getReviewOverview());
    }

    /** 查询当前登录用户的普通单词测试题型设置。 */
    @GetMapping("/challenge/settings")
    public AjaxResult challengeSettings()
    {
        return success(service.getChallengeSetting());
    }

    /** 保存当前登录用户的普通单词测试题型设置。 */
    @PutMapping("/challenge/settings")
    public AjaxResult updateChallengeSettings(@RequestBody EngChallengeSettingUpdateDto request)
    {
        return success(service.updateChallengeSetting(request));
    }

    /** 按 NEW 或 REVIEW 模式获取不包含正确答案的固定词集挑战。 */
    @GetMapping("/challenge")
    public AjaxResult challenge(@RequestParam String mode,
            @RequestParam(required = false) Long articleId,
            @RequestParam(required = false) Integer levelNo,
            @RequestParam(required = false) String wordIds,
            HttpServletRequest request)
    {
        return success(service.getChallenge(mode, articleId, levelNo, parseWordIds(wordIds), isSecure(request)));
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

    /** 上传单词录音并返回可信跟读评分；同一来源每分钟最多二十次。 */
    @RateLimiter(key = "eng:pronunciation:", time = 60, count = 20, limitType = LimitType.IP)
    @PostMapping("/challenge/pronunciation")
    public AjaxResult pronunciation(@RequestParam String attemptId, @RequestParam String mode,
            @RequestParam(required = false) Long articleId,
            @RequestParam(required = false) Integer levelNo,
            @RequestParam String questionId, @RequestParam MultipartFile audio,
            HttpServletRequest servletRequest)
    {
        if (!isSecure(servletRequest))
            throw new ServiceException("当前为非安全访问环境，跟读评分需要 HTTPS");
        EngPronunciationAssessDto request = new EngPronunciationAssessDto();
        request.setAttemptId(attemptId); request.setMode(mode); request.setArticleId(articleId);
        request.setLevelNo(levelNo); request.setQuestionId(questionId); request.setAudio(audio);
        return success(service.assessPronunciation(request));
    }

    /** 提交 request 中的闯关答案并返回服务端计分结果。 */
    @PostMapping("/challenge/submit")
    public AjaxResult submit(@RequestBody EngChallengeSubmitDto request, HttpServletRequest servletRequest)
    {
        return success(service.submitChallenge(request, isSecure(servletRequest)));
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

    /**
     * 兼容 TLS 在反向代理终止的场景。生产环境仍须由网络层限制 8080 仅供内网访问，
     * 并由 Nginx 覆盖客户端传入的 X-Forwarded-Proto。
     */
    private boolean isSecure(HttpServletRequest request)
    {
        if (request.isSecure()) return true;
        if (!isTrustedProxyAddress(request.getRemoteAddr())) return false;
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto == null) return false;
        String firstValue = forwardedProto.split(",", 2)[0].trim();
        return "https".equalsIgnoreCase(firstValue);
    }

    /** 仅信任回环、私网和链路本地来源，避免公网直连通过伪造代理头启用跟读。 */
    private boolean isTrustedProxyAddress(String remoteAddress)
    {
        InetAddress address = parseLiteralAddress(remoteAddress);
        if (address == null) return false;
        if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()) return true;
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC;
    }

    /** 只解析字面量 IP，拒绝主机名以避免通过 DNS 结果建立代理信任。 */
    private InetAddress parseLiteralAddress(String value)
    {
        if (value == null || value.isBlank()) return null;
        try
        {
            if (value.indexOf(':') >= 0)
            {
                if (!value.matches("[0-9A-Fa-f:.]+")) return null;
                return InetAddress.getByName(value);
            }
            String[] parts = value.split("\\.", -1);
            if (parts.length != 4) return null;
            byte[] bytes = new byte[4];
            for (int index = 0; index < parts.length; index++)
            {
                if (parts[index].isEmpty() || !parts[index].chars().allMatch(Character::isDigit)) return null;
                int part = Integer.parseInt(parts[index]);
                if (part > 255) return null;
                bytes[index] = (byte) part;
            }
            return InetAddress.getByAddress(bytes);
        }
        catch (UnknownHostException | NumberFormatException exception)
        {
            return null;
        }
    }
}
