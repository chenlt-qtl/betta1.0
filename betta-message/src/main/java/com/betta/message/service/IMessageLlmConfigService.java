package com.betta.message.service;

import com.betta.message.domain.MessageLlmConfig;

import java.util.List;

/**
 * 大模型配置 Service 接口
 *
 * @author betta
 */
public interface IMessageLlmConfigService {

    /**
     * 查询列表
     *
     * @param config 查询条件
     * @return 列表
     */
    List<MessageLlmConfig> selectList(MessageLlmConfig config);

    /**
     * 根据ID查询
     *
     * @param id 主键
     * @return 配置
     */
    MessageLlmConfig selectById(Long id);

    /**
     * 新增
     *
     * @param config 配置
     * @return 影响行数
     */
    int insert(MessageLlmConfig config);

    /**
     * 更新
     *
     * @param config 配置
     * @return 影响行数
     */
    int update(MessageLlmConfig config);

    /**
     * 删除
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);
}
