package com.betta.message.service;

import com.betta.message.domain.MessageRecord;

import java.util.List;

/**
 * 消息记录 Service 接口
 *
 * @author betta
 */
public interface IMessageRecordService {

    /**
     * 查询列表
     *
     * @param record 查询条件
     * @return 列表
     */
    List<MessageRecord> selectList(MessageRecord record);

    /**
     * 根据ID查询
     *
     * @param id 主键
     * @return 记录
     */
    MessageRecord selectById(Long id);

    /**
     * 新增
     *
     * @param record 记录
     * @return 影响行数
     */
    int insert(MessageRecord record);

    /**
     * 更新
     *
     * @param record 记录
     * @return 影响行数
     */
    int update(MessageRecord record);
}
