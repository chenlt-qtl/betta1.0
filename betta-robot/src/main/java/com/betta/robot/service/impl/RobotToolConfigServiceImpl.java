package com.betta.robot.service.impl;

import java.util.List;
import com.betta.common.utils.DateUtils;
import com.betta.robot.domain.RobotToolConfig;
import com.betta.robot.mapper.RobotToolConfigMapper;
import com.betta.robot.service.IRobotToolConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 工具配置Service业务层处理
 * 
 * @author betta
 * @date 2026-04-11
 */
@Service
public class RobotToolConfigServiceImpl implements IRobotToolConfigService
{
    @Autowired
    private RobotToolConfigMapper robotToolConfigMapper;

    /**
     * 查询工具配置
     * 
     * @param id 工具配置主键
     * @return 工具配置
     */
    @Override
    public RobotToolConfig selectRobotToolConfigById(Long id)
    {
        return robotToolConfigMapper.selectRobotToolConfigById(id);
    }

    /**
     * 查询工具配置列表
     * 
     * @param robotToolConfig 工具配置
     * @return 工具配置
     */
    @Override
    public List<RobotToolConfig> selectRobotToolConfigList(RobotToolConfig robotToolConfig)
    {
        return robotToolConfigMapper.selectRobotToolConfigList(robotToolConfig);
    }

    /**
     * 新增工具配置
     * 
     * @param robotToolConfig 工具配置
     * @return 结果
     */
    @Override
    public int insertRobotToolConfig(RobotToolConfig robotToolConfig)
    {
        robotToolConfig.setCreateTime(DateUtils.getNowDate());
        return robotToolConfigMapper.insertRobotToolConfig(robotToolConfig);
    }

    /**
     * 修改工具配置
     * 
     * @param robotToolConfig 工具配置
     * @return 结果
     */
    @Override
    public int updateRobotToolConfig(RobotToolConfig robotToolConfig)
    {
        robotToolConfig.setUpdateTime(DateUtils.getNowDate());
        return robotToolConfigMapper.updateRobotToolConfig(robotToolConfig);
    }

    /**
     * 批量删除工具配置
     * 
     * @param ids 需要删除的工具配置主键
     * @return 结果
     */
    @Override
    public int deleteRobotToolConfigByIds(Long[] ids)
    {
        return robotToolConfigMapper.deleteRobotToolConfigByIds(ids);
    }

    /**
     * 删除工具配置信息
     * 
     * @param id 工具配置主键
     * @return 结果
     */
    @Override
    public int deleteRobotToolConfigById(Long id)
    {
        return robotToolConfigMapper.deleteRobotToolConfigById(id);
    }
}
