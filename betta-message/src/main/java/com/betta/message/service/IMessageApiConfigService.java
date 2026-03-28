package com.betta.message.service;

import com.betta.message.domain.MessageApiConfig;

import java.util.List;

/**
 * API配置 Service 接口
 *
 * @author betta
 */
public interface IMessageApiConfigService {

    /**
     * 查询列表
     *
     * @param config 查询条件
     * @return 列表
     */
    List<MessageApiConfig> selectList(MessageApiConfig config);

    /**
     * 根据ID查询
     *
     * @param id 主键
     * @return 配置
     */
    MessageApiConfig selectById(Long id);

    /**
     * 新增
     *
     * @param config 配置
     * @return 影响行数
     */
    int insert(MessageApiConfig config);

    /**
     * 更新
     *
     * @param config 配置
     * @return 影响行数
     */
    int update(MessageApiConfig config);

    /**
     * 删除
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);
}
