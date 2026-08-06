package com.betta.other.service.impl;

import com.betta.common.annotation.CreateByScope;
import com.betta.common.utils.DateUtils;
import com.betta.other.domain.ClockInData;
import com.betta.other.mapper.ClockInDataMapper;
import com.betta.other.service.IClockInDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 打卡数据Service业务层处理
 *
 * @author chenlt
 * @date 2024-07-01
 */
@Service
public class ClockInDataServiceImpl implements IClockInDataService {
    @Autowired
    private ClockInDataMapper clockInDataMapper;

    /**
     * 查询打卡数据
     *
     * @param id 打卡数据主键
     * @return 打卡数据
     */
    @Override
    public ClockInData selectClockInDataById(Long id) {
        return clockInDataMapper.selectClockInDataById(id);
    }

    /**
     * 查询打卡数据列表
     *
     * @param clockInData 打卡数据
     * @return 打卡数据
     */
    @Override
    @CreateByScope("")
    public List<ClockInData> selectClockInDataList(ClockInData clockInData) {
        return clockInDataMapper.selectClockInDataList(clockInData);
    }

    /**
     * 新增打卡数据
     *
     * @param clockInData 打卡数据
     * @return 结果
     */
    @Override
    public int insertClockInData(ClockInData clockInData) {
        clockInData.setCreateTime(DateUtils.getNowDate());
        return clockInDataMapper.insertClockInData(clockInData);
    }

    /**
     * 修改打卡数据
     *
     * @param clockInData 打卡数据
     * @return 结果
     */
    @Override
    public int updateClockInData(ClockInData clockInData) {
        clockInData.setUpdateTime(DateUtils.getNowDate());
        return clockInDataMapper.updateClockInData(clockInData);
    }

    /**
     * 批量删除打卡数据
     *
     * @param ids 需要删除的打卡数据主键
     * @return 结果
     */
    @Override
    public int deleteClockInDataByIds(Long[] ids) {
        return clockInDataMapper.deleteClockInDataByIds(ids);
    }

    /**
     * 删除打卡数据信息
     *
     * @param id 打卡数据主键
     * @return 结果
     */
    @Override
    public int deleteClockInDataById(Long id) {
        return clockInDataMapper.deleteClockInDataById(id);
    }
}
