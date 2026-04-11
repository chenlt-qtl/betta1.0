package com.betta.robot.mapper;

import com.betta.robot.domain.MessageChannelConfig;

import java.util.List;

/**
 * 消息通道配置 Mapper
 *
 * @author betta
 */
public interface MessageChannelConfigMapper {

    /**
     * 查询列表
     *
     * @param config 查询条件
     * @return 列表
     */
    List<MessageChannelConfig> selectList(MessageChannelConfig config);

    /**
     * 根据ID查询
     *
     * @param id 主键
     * @return 配置
     */
    MessageChannelConfig selectById(Long id);

    /**
     * 新增
     *
     * @param config 配置
     * @return 影响行数
     */
    int insert(MessageChannelConfig config);

    /**
     * 更新
     *
     * @param config 配置
     * @return 影响行数
     */
    int update(MessageChannelConfig config);

    /**
     * 删除
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);
}
