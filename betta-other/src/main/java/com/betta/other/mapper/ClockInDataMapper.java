package com.betta.other.mapper;

import java.util.List;
import com.betta.other.domain.ClockInData;

/**
 * 打卡数据Mapper接口
 * 
 * @author chenlt
 * @date 2024-07-01
 */
public interface ClockInDataMapper 
{
    /**
     * 查询打卡数据
     * 
     * @param id 打卡数据主键
     * @return 打卡数据
     */
    public ClockInData selectClockInDataById(Long id);

    /**
     * 查询打卡数据列表
     * 
     * @param clockInData 打卡数据
     * @return 打卡数据集合
     */
    public List<ClockInData> selectClockInDataList(ClockInData clockInData);

    /**
     * 新增打卡数据
     * 
     * @param clockInData 打卡数据
     * @return 结果
     */
    public int insertClockInData(ClockInData clockInData);

    /**
     * 修改打卡数据
     * 
     * @param clockInData 打卡数据
     * @return 结果
     */
    public int updateClockInData(ClockInData clockInData);

    /**
     * 删除打卡数据
     * 
     * @param id 打卡数据主键
     * @return 结果
     */
    public int deleteClockInDataById(Long id);

    /**
     * 批量删除打卡数据
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteClockInDataByIds(Long[] ids);
}
