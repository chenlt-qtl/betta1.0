-- 每日 03:00 执行笔记图片标记清理；重复执行迁移时更新既有任务而不重复插入。
UPDATE sys_job
SET job_name = '笔记图片定时清理',
    job_group = 'DEFAULT',
    cron_expression = '0 0 3 * * ?',
    misfire_policy = '3',
    concurrent = '1',
    status = '0',
    update_by = 'admin',
    update_time = SYSDATE(),
    remark = '回收孤立图片并清理超过30天的系统回收版本'
WHERE invoke_target = 'noteImageCleanupTask.cleanupExpiredImages(30)';

INSERT INTO sys_job
    (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status,
     create_by, create_time, update_by, update_time, remark)
SELECT '笔记图片定时清理', 'DEFAULT', 'noteImageCleanupTask.cleanupExpiredImages(30)',
       '0 0 3 * * ?', '3', '1', '0', 'admin', SYSDATE(), '', NULL,
       '回收孤立图片并清理超过30天的系统回收版本'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_job
    WHERE invoke_target = 'noteImageCleanupTask.cleanupExpiredImages(30)'
);
