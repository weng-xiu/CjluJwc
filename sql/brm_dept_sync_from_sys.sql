-- =====================================================================
-- 院系管理 与 部门管理 数据整合脚本
-- 目的：将 sys_dept（部门管理，已含长江大学真实院系结构）灌入
--       brm_department（院系管理），保留同一 dept_id，
--       使各业务表（sam_student / brm_major / brm_teacher /
--       tpm_training_plan / portal.publish_dept_id / sys_student 等）
--       对 dept_id 的引用无需任何改动（零风险）。
-- 说明：dept_code 为 brm_department 特有 NOT NULL 字段，
--       sys_dept 无该列，按 dept_id 生成编码 D0100 形式。
-- 可重复执行：仅插入 brm_department 中尚不存在的 dept_id。
-- =====================================================================

INSERT INTO brm_department (
    dept_id, parent_id, ancestors, dept_code, dept_name,
    leader, phone, email, order_num, status, del_flag,
    create_by, create_time, update_by, update_time, remark
)
SELECT
    s.dept_id,
    s.parent_id,
    s.ancestors,
    CONCAT('D', LPAD(s.dept_id, 4, '0'))            AS dept_code,
    s.dept_name,
    s.leader,
    s.phone,
    s.email,
    s.order_num,
    s.status,
    '0'                                             AS del_flag,
    COALESCE(NULLIF(s.create_by, ''), 'admin')      AS create_by,
    COALESCE(s.create_time, NOW())                  AS create_time,
    s.update_by,
    s.update_time,
    '由 sys_dept 同步导入'                          AS remark
FROM sys_dept s
WHERE s.del_flag = '0'
  AND s.dept_id NOT IN (SELECT d.dept_id FROM brm_department d);
