package com.betta.message.service.impl;

import com.betta.message.domain.MessageApiConfig;
import com.betta.message.mapper.MessageApiConfigMapper;
import com.betta.message.service.IMessageApiConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * API配置 Service 实现
 *
 * @author betta
 */
@Service
public class MessageApiConfigServiceImpl implements IMessageApiConfigService {

    @Autowired
    private MessageApiConfigMapper messageApiConfigMapper;

    @Override
    public List<MessageApiConfig> selectList(MessageApiConfig config) {
        return messageApiConfigMapper.selectList(config);
    }

    @Override
    public MessageApiConfig selectById(Long id) {
        return messageApiConfigMapper.selectById(id);
    }

    @Override
    public int insert(MessageApiConfig config) {
        return messageApiConfigMapper.insert(config);
    }

    @Override
    public int update(MessageApiConfig config) {
        return messageApiConfigMapper.update(config);
    }

    @Override
    public int deleteById(Long id) {
        return messageApiConfigMapper.deleteById(id);
    }
}
