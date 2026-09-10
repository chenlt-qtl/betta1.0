package com.betta.quartz.task;

import org.springframework.stereotype.Component;
import com.betta.system.service.INoteFileService;

/** 笔记图片清理调度入口，仅负责参数校验并委托文件服务。 */
@Component("noteImageCleanupTask")
public class NoteImageCleanupTask
{
    private final INoteFileService noteFileService;

    /**
     * 创建只负责调度委托的任务入口。
     *
     * @param noteFileService 笔记文件业务服务
     */
    public NoteImageCleanupTask(INoteFileService noteFileService)
    {
        this.noteFileService = noteFileService;
    }

    /**
     * 清理超过保留期的笔记回收图片。
     *
     * @param retentionDays 回收区保留天数，必须大于0
     */
    public void cleanupExpiredImages(Integer retentionDays)
    {
        if (retentionDays == null || retentionDays <= 0)
        {
            throw new IllegalArgumentException("图片回收站保留天数必须大于0");
        }
        noteFileService.cleanupExpiredNoteImages(retentionDays);
    }
}
