package com.betta.robot.service.impl;

import com.betta.robot.domain.LlmCallRecord;
import com.betta.robot.mapper.LlmCallRecordMapper;
import com.betta.robot.service.ILlmCallRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 大模型调用记录服务实现
 *
 * @author betta
 */
@Slf4j
@Service
public class LlmCallRecordServiceImpl implements ILlmCallRecordService {

    @Autowired
    private LlmCallRecordMapper llmCallRecordMapper;

    @Override
    public Long createRecord(Long llmConfigId, String modelName, String provider, String prompt) {
        try {
            LlmCallRecord record = new LlmCallRecord();
            record.setLlmConfigId(llmConfigId);
            record.setModelName(modelName);
            record.setProvider(provider);
            record.setPrompt(prompt);
            record.setStatus("PENDING");
            record.setCreateTime(new java.util.Date());

            llmCallRecordMapper.insert(record);
            log.info("创建大模型调用记录成功，ID：{}", record.getId());
            return record.getId();
        } catch (Exception e) {
            log.error("创建大模型调用记录失败", e);
            return null;
        }
    }

    @Override
    public void updateSuccess(Long recordId, String responseContent, int duration) {
        try {
            LlmCallRecord record = new LlmCallRecord();
            record.setId(recordId);
            record.setResponseContent(responseContent);
            record.setStatus("SUCCESS");
            record.setDuration(duration);

            llmCallRecordMapper.update(record);
            log.info("更新大模型调用记录为成功状态，ID：{}", recordId);
        } catch (Exception e) {
            log.error("更新大模型调用记录失败，ID：{}", recordId, e);
        }
    }

    @Override
    public void updateFail(Long recordId, String errorMsg, int duration) {
        try {
            LlmCallRecord record = new LlmCallRecord();
            record.setId(recordId);
            record.setStatus("FAIL");
            record.setErrorMsg(errorMsg);
            record.setDuration(duration);

            llmCallRecordMapper.update(record);
            log.info("更新大模型调用记录为失败状态，ID：{}", recordId);
        } catch (Exception e) {
            log.error("更新大模型调用记录失败，ID：{}", recordId, e);
        }
    }
}
