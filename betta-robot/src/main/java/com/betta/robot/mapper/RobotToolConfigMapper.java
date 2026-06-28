package com.betta.robot.mapper;

import com.betta.robot.domain.RobotToolConfig;

import java.util.List;

/**
 * 工具配置Mapper接口
 * 
 * @author betta
 * @date 2026-04-11
 */
public interface RobotToolConfigMapper 
{
    /**
     * 查询工具配置
     * 
     * @param id 工具配置主键
     * @return 工具配置
     */
    public RobotToolConfig selectRobotToolConfigById(Long id);

    /**
     * 查询工具配置列表
     * 
     * @param robotToolConfig 工具配置
     * @return 工具配置集合
     */
    public List<RobotToolConfig> selectRobotToolConfigList(RobotToolConfig robotToolConfig);

    /**
     * 新增工具配置
     * 
     * @param robotToolConfig 工具配置
     * @return 结果
     */
    public int insertRobotToolConfig(RobotToolConfig robotToolConfig);

    /**
     * 修改工具配置
     * 
     * @param robotToolConfig 工具配置
     * @return 结果
     */
    public int updateRobotToolConfig(RobotToolConfig robotToolConfig);

    /**
     * 删除工具配置
     * 
     * @param id 工具配置主键
     * @return 结果
     */
    public int deleteRobotToolConfigById(Long id);

    /**
     * 批量删除工具配置
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteRobotToolConfigByIds(Long[] ids);
}
