package com.betta.robot.controller;

import com.betta.common.core.controller.BaseController;
import com.betta.common.core.page.TableDataInfo;
import com.betta.robot.domain.MessageRecord;
import com.betta.robot.service.IMessageRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 消息记录查询
 *
 * @author betta
 */
@RestController
@RequestMapping("/message/record")
public class MessageRecordController extends BaseController {

    @Autowired
    private IMessageRecordService messageRecordService;

    /**
     * 查询消息记录列表（分页）
     *
     * @param record 查询条件
     * @return 分页列表
     */
    @PreAuthorize("@ss.hasPermi('message:record:list')")
    @GetMapping("/list")
    public TableDataInfo list(MessageRecord record) {
        startPage();
        List<MessageRecord> list = messageRecordService.selectList(record);
        return getDataTable(list);
    }
}
