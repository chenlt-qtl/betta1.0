package com.betta.web.controller.menstrual;

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
import com.betta.other.domain.menstrual.MenstrualRecord;
import com.betta.other.service.IMenstrualService;

/** 当前登录用户的月经记录接口。 */
@RestController
@RequestMapping("/menstrual")
public class MenstrualController extends BaseController
{
    private final IMenstrualService service;

    /** 创建记录控制器。 */
    public MenstrualController(IMenstrualService service)
    {
        this.service = service;
    }

    /** 查询历史记录。 */
    @GetMapping("/records")
    public AjaxResult records()
    {
        return success(service.listRecords());
    }

    /** 新增记录。 */
    @PostMapping("/records")
    public AjaxResult create(@RequestBody MenstrualRecord record)
    {
        return success(service.createRecord(record));
    }

    /** 更新记录。 */
    @PutMapping("/records/{id}")
    public AjaxResult update(@PathVariable Long id, @RequestBody MenstrualRecord record)
    {
        return success(service.updateRecord(id, record));
    }

    /** 删除记录。 */
    @DeleteMapping("/records/{id}")
    public AjaxResult delete(@PathVariable Long id)
    {
        service.deleteRecord(id);
        return success();
    }

    /** 获取历史统计与预测。 */
    @GetMapping("/summary")
    public AjaxResult summary()
    {
        return success(service.summary());
    }
}
