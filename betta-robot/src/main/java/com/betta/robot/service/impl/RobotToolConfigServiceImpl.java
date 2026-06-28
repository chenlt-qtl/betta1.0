package com.betta.robot.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.betta.common.utils.DateUtils;
import com.betta.robot.domain.RobotToolConfig;
import com.betta.robot.mapper.RobotToolConfigMapper;
import com.betta.robot.service.IRobotToolConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * 测试正则表达式
     * 
     * @param regexPattern 正则表达式
     * @param regexParamMap 捕获组参数映射（JSON格式）
     * @param testText 测试文本
     * @return 匹配结果
     */
    @Override
    public Map<String, Object> testRegex(String regexPattern, String regexParamMap, String testText)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("matched", false);
        result.put("groups", new HashMap<>());
        result.put("params", new HashMap<>());

        if (regexPattern == null || regexPattern.trim().isEmpty()) {
            result.put("error", "正则表达式不能为空");
            return result;
        }

        try {
            Pattern pattern = Pattern.compile(regexPattern);
            Matcher matcher = pattern.matcher(testText);

            if (matcher.matches()) {
                result.put("matched", true);

                // 提取所有捕获组
                Map<String, String> groups = new HashMap<>();
                groups.put("0", matcher.group(0)); // 整个匹配
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    groups.put(String.valueOf(i), matcher.group(i));
                }
                result.put("groups", groups);

                // 根据映射生成参数
                if (regexParamMap != null && !regexParamMap.trim().isEmpty()) {
                    try {
                        JSONObject paramMapping = JSON.parseObject(regexParamMap);
                        Map<String, String> params = new HashMap<>();
                        for (String key : paramMapping.keySet()) {
                            String groupName = paramMapping.getString(key);
                            String groupValue = groups.get(key);
                            if (groupValue != null) {
                                params.put(groupName, groupValue);
                            }
                        }
                        result.put("params", params);
                    } catch (Exception e) {
                        result.put("error", "参数映射JSON格式错误: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            result.put("error", "正则表达式错误: " + e.getMessage());
        }

        return result;
    }

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
