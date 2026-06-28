package com.betta.robot.service.impl;

import com.betta.robot.domain.MessageLlmConfig;
import com.betta.robot.mapper.MessageLlmConfigMapper;
import com.betta.robot.service.IMessageLlmConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 大模型配置 Service 实现
 *
 * @author betta
 */
@Service
public class MessageLlmConfigServiceImpl implements IMessageLlmConfigService {

    @Autowired
    private MessageLlmConfigMapper messageLlmConfigMapper;

    @Override
    public List<MessageLlmConfig> selectList(MessageLlmConfig config) {
        return messageLlmConfigMapper.selectList(config);
    }

    @Override
    public MessageLlmConfig selectById(Long id) {
        return messageLlmConfigMapper.selectById(id);
    }

    @Override
    public int insert(MessageLlmConfig config) {
        return messageLlmConfigMapper.insert(config);
    }

    @Override
    public int update(MessageLlmConfig config) {
        return messageLlmConfigMapper.update(config);
    }

    @Override
    public int deleteById(Long id) {
        return messageLlmConfigMapper.deleteById(id);
    }
}
