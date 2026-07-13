-- ===============================================
-- 教务系统升级 阶段四：学业预警定时任务配置
-- 内容：新增Quartz定时任务（学业预警批量生成）
-- 创建时间：2026-07-13
-- ===============================================

-- 学业预警批量生成定时任务（每日凌晨2:00执行）
INSERT INTO sys_job(job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status)
VALUES (200, '学业预警生成', 'DEFAULT', 'academicWarningTask.generateWarnings(1L)', '0 0 2 * * ?', '1', '1', '1');
