package com.betta.message.service.impl;

import com.betta.common.utils.DateUtils;
import com.betta.common.utils.StringUtils;
import com.betta.message.domain.MessageChannelConfig;
import com.betta.message.mapper.MessageChannelConfigMapper;
import com.betta.message.service.IMessageChannelConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息通道配置 Service 实现
 *
 * @author betta
 */
@Service
public class MessageChannelConfigServiceImpl implements IMessageChannelConfigService {

    @Autowired
    private MessageChannelConfigMapper messageChannelConfigMapper;

    @Override
    public List<MessageChannelConfig> selectList(MessageChannelConfig config) {
        return messageChannelConfigMapper.selectList(config);
    }

    @Override
    public MessageChannelConfig selectById(Long id) {
        return messageChannelConfigMapper.selectById(id);
    }

    @Override
    public MessageChannelConfig selectOneEnabledByChannel(String channelType) {
        if (StringUtils.isEmpty(channelType)) {
            return null;
        }
        MessageChannelConfig query = new MessageChannelConfig();
        query.setChannelType(channelType);
        query.setStatus("0");
        List<MessageChannelConfig> list = messageChannelConfigMapper.selectList(query);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public int insert(MessageChannelConfig config) {
        config.setCreateTime(DateUtils.getNowDate());
        return messageChannelConfigMapper.insert(config);
    }

    @Override
    public int update(MessageChannelConfig config) {
        config.setUpdateTime(DateUtils.getNowDate());
        return messageChannelConfigMapper.update(config);
    }

    @Override
    public int deleteById(Long id) {
        return messageChannelConfigMapper.deleteById(id);
    }
}
