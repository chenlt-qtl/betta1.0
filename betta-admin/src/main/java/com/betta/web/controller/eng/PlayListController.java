package com.betta.web.controller.eng;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.betta.common.core.controller.BaseController;
import com.betta.common.core.domain.AjaxResult;
import com.betta.common.core.page.TableDataInfo;
import com.betta.eng.domain.PlayList;
import com.betta.eng.service.IPlayListService;

/** 播放列表控制器，只负责参数接收、当前用户条件和服务调用。 */
@RestController
@RequestMapping("/eng/playList")
public class PlayListController extends BaseController
{
    private final IPlayListService service;

    /** 创建播放列表控制器；service 负责用户隔离与数据库调用。 */
    public PlayListController(IPlayListService service)
    {
        this.service = service;
    }

    /** 分页查询 playList 条件对应列表。 */
    @GetMapping("/list")
    public TableDataInfo list(PlayList playList)
    {
        startPage();
        return getDataTable(service.selectPlayListList(playList));
    }

    /** 查询当前用户的播放列表。 */
    @GetMapping("/list/user")
    public AjaxResult user()
    {
        return success(service.selectCurrentUserPlayList());
    }

    /** 查询 id 对应当前用户播放列表。 */
    @GetMapping("/{id}")
    public AjaxResult info(@PathVariable Long id)
    {
        return success(service.selectPlayListById(id));
    }

    /** 新增 playList。 */
    @PostMapping
    public AjaxResult add(@RequestBody PlayList playList)
    {
        return toAjax(service.insertPlayList(playList));
    }

    /** 更新 playList。 */
    @PutMapping
    public AjaxResult edit(@RequestBody PlayList playList)
    {
        return toAjax(service.updatePlayList(playList));
    }

    /** 删除 ids 对应当前用户播放列表。 */
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(service.deletePlayListByIds(ids));
    }
}
