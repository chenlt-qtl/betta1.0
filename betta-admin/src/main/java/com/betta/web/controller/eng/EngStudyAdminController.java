package com.betta.web.controller.eng;

import com.betta.common.core.controller.BaseController;
import com.betta.common.core.page.TableDataInfo;
import com.betta.eng.domain.EngStudyRecord;
import com.betta.eng.domain.vo.EngStudyAdminUserVo;
import com.betta.eng.service.IEngStudyAdminService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理员积分查询控制器，仅提供全员汇总和个人历史只读接口。 */
@RestController
@RequestMapping("/eng/study/admin/score")
public class EngStudyAdminController extends BaseController
{
    private final IEngStudyAdminService service;

    /** 创建管理员积分查询控制器。 */
    public EngStudyAdminController(IEngStudyAdminService service)
    {
        this.service = service;
    }

    /** 分页查询未删除用户的积分汇总。 */
    @PreAuthorize("@ss.hasPermi('eng:study:score:admin')")
    @GetMapping("/list")
    public TableDataInfo list(EngStudyAdminUserVo condition)
    {
        startPage();
        List<EngStudyAdminUserVo> list = service.selectUserScoreList(condition);
        return getDataTable(list);
    }

    /** 分页查询指定用户的完整积分历史，userId 为必填用户主键。 */
    @PreAuthorize("@ss.hasPermi('eng:study:score:admin')")
    @GetMapping("/history/list")
    public TableDataInfo historyList(@RequestParam Long userId)
    {
        startPage();
        List<EngStudyRecord> list = service.selectUserScoreHistory(userId);
        return getDataTable(list);
    }
}
