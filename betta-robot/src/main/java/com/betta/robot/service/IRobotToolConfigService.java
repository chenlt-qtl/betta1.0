package com.betta.robot.service;

import com.betta.robot.domain.RobotToolConfig;

import java.util.List;
import java.util.Map;

/**
 * 工具配置Service接口
 * 
 * @author betta
 * @date 2026-04-11
 */
public interface IRobotToolConfigService 
{
    /**
     * 测试正则表达式
     * 
     * @param regexPattern 正则表达式
     * @param regexParamMap 捕获组参数映射（JSON格式）
     * @param testText 测试文本
     * @return 匹配结果
     */
    public Map<String, Object> testRegex(String regexPattern, String regexParamMap, String testText);

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
     * 批量删除工具配置
     * 
     * @param ids 需要删除的工具配置主键集合
     * @return 结果
     */
    public int deleteRobotToolConfigByIds(Long[] ids);

    /**
     * 删除工具配置信息
     * 
     * @param id 工具配置主键
     * @return 结果
     */
    public int deleteRobotToolConfigById(Long id);
}
