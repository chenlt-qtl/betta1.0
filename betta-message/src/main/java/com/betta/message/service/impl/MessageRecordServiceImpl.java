package com.betta.message.service.impl;

import com.betta.message.domain.MessageRecord;
import com.betta.message.mapper.MessageRecordMapper;
import com.betta.message.service.IMessageRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息记录 Service 实现
 *
 * @author betta
 */
@Service
public class MessageRecordServiceImpl implements IMessageRecordService {

    @Autowired
    private MessageRecordMapper messageRecordMapper;

    @Override
    public List<MessageRecord> selectList(MessageRecord record) {
        return messageRecordMapper.selectList(record);
    }

    @Override
    public MessageRecord selectById(Long id) {
        return messageRecordMapper.selectById(id);
    }

    @Override
    public int insert(MessageRecord record) {
        if (record.getCreateTime() == null) {
            record.setCreateTime(new java.util.Date());
        }
        return messageRecordMapper.insert(record);
    }

    @Override
    public int update(MessageRecord record) {
        return messageRecordMapper.update(record);
    }
}
