package com.betta.robot.mapper;

import com.betta.robot.domain.LlmCallRecord;

/**
 * 大模型调用记录 Mapper 接口
 *
 * @author betta
 */
public interface LlmCallRecordMapper {

    /**
     * 新增
     *
     * @param record 记录
     * @return 影响行数
     */
    int insert(LlmCallRecord record);

    /**
     * 更新
     *
     * @param record 记录
     * @return 影响行数
     */
    int update(LlmCallRecord record);
}
