-- =====================================================================================
-- 变更：V20261031.4 —— P-1 余面四表补二级索引（缺陷 P-1 余面 / 架构 A-7）
-- 背景：V20261031.3 已为「选课 / 课表 / 成绩 / 用户」四张最热表补 8 条组合索引，本脚本
--       收口台账登记的四张余面表——tpm_course_offering（开课）、brm_teacher（教师）、
--       tpm_schedule_adjustment（调停课）、sys_user_role（用户角色）。这四张表在本仓
--       库内实测**只有 PRIMARY**，无任何二级索引。
-- 做法：与 V20261031_3 同一纪律——**先核对真实列与 Mapper 语句，再定索引列序**，不照抄
--       缺陷报告的字面清单。本批据此推翻/修正了两条（详见下方「口径纠正」）。
--
-- 口径纠正（动手前逐条核对的结果）：
--   ① 报告的 `tpm_schedule_adjustment(approve_by, approve_status)` **前导列不可用**：
--      `approve_by` 在全部 Mapper 里只出现在 UPDATE 的 SET 子句（TpmScheduleAdjustmentMapper
--      L126），**没有任何 WHERE 过滤**——索引以只写不读的列打头，等于给写路径加负担却永不
--      被优化器采用。真正高频的两个谓词是「待审列表按 approve_status」与「门户我的申请按
--      create_by」（PortalAdjustmentController L65 / L125），故索引按这两条分别建。
--   ② 报告的 `tpm_course_offering(semester_id, teacher_id, status)` 列都存在，但**列序不对**：
--      本表最高频的教师侧访问（TpmScheduleMapper.selectTpmScheduleList 的
--      `and o.teacher_id = ?`、selectByTeacherAndTime）是「只给教师、不给学期」，
--      semester_id 打头就服务不了；故改为 teacher_id 打头，学期作第二列。
--   ③ 报告给 `sys_user_role` 只列了 (role_id)。本脚本建成 `(role_id, user_id)`：主键是
--      (user_id, role_id)，二级索引补上 user_id 即成**覆盖索引**（按角色取用户的 join 不回表），
--      且与主键不构成前缀冗余（列序相反）。
--   ④ `brm_teacher.teacher_code` **只建普通索引、不建 UNIQUE**：该表无 del_flag（删除是物理删），
--      看似可加唯一键，但应用层 insert/update **没有**工号唯一性校验（只有导入逐行校验），
--      唯一键会把「录入重复工号」从静默接受变成 1062 直抛，属 P-2 家族的数据约束改动，
--      需连同存量清洗与 DuplicateKeyException 转译一起做，不在本批。
--   另：`tpm_schedule_adjustment.schedule_id` 虽有过滤条件（XML L60），但全仓无调用方设置该
--      查询参数（只有 admin 列表页可传），本批不建索引；真需要时再按同一判据补。
--
-- 影响表：tpm_course_offering / brm_teacher / tpm_schedule_adjustment / sys_user_role（只加索引）
-- 幂等性：是（每条索引均按 information_schema.statistics 判存在，存在则跳过）
-- 回滚脚本：db/rollback/V20261031_4__add_remaining_hot_indexes.sql
-- 执行方式：mysql --host=127.0.0.1 --port=3306 -uroot -p yu-cjlujwc < db/migration/V20261031_4__add_remaining_hot_indexes.sql
--           （客户端参数写 --host= / --port=，不要写 -h127.0.0.1：会被当成主机名 127 报 Unknown MySQL server host）
-- 注意事项：
--   1) 本脚本**不改任何数据**，回滚只需 DROP INDEX，可先于应用版本单独上线。
--   2) 加二级索引走 ALGORITHM=INPLACE，允许并发 DML；但 tpm_course_offering 是选课高峰
--      的热表，建议避开选课时段执行（本批在合成数据量级上实测，见台账 V5.32）。
-- =====================================================================================

-- ---------------------------------------------------------------------------
-- 0) 体检：执行前打印四张表的现有索引（用于前后对照，不是断言）
-- ---------------------------------------------------------------------------
SELECT table_name, index_name,
       GROUP_CONCAT(column_name ORDER BY seq_in_index) AS idx_cols,
       IF(non_unique = 0, 'UNIQUE', 'INDEX') AS kind
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN ('tpm_course_offering', 'brm_teacher', 'tpm_schedule_adjustment', 'sys_user_role')
GROUP BY table_name, index_name, non_unique
ORDER BY table_name, index_name;

-- ---------------------------------------------------------------------------
-- 1) tpm_course_offering（开课计划；选课/排课/课表三条链路共同的驱动表）
-- ---------------------------------------------------------------------------

-- 1.1 idx_offering_semester_status (semester_id, del_flag, offering_status, status)
--      服务：selectTpmCourseOfferingList（管理端开课列表：学期 + 开课状态 + 状态）、
--            selectTpmCourseOfferingListForPortal（门户可选课列表，同前缀 + order by offering_id）、
--            selectOfferingsToSchedule（排课引擎取「已确认且未排课」的开课：学期 + 状态 + 开课状态）、
--            selectActiveCourseIdsBySemester（`offering_status <> '2'` 走 range，前三列已把范围压到单个学期）。
--      列序：学期是这四条语句共同的等值首列；del_flag 次之（全表恒为等值 '0'，且放在 range 列之前
--            才能被利用）；offering_status 承接等值/范围；status 垫尾。
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'tpm_course_offering'
      AND index_name = 'idx_offering_semester_status');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `tpm_course_offering`
       ADD INDEX `idx_offering_semester_status` (`semester_id`, `del_flag`, `offering_status`, `status`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.2 idx_offering_teacher (teacher_id, semester_id)
--      服务：selectTpmCourseOfferingList 的教师筛选、TpmScheduleMapper.selectTpmScheduleList
--            的 `and o.teacher_id = ?`（门户「我的课表」/ 教师课表）、selectByTeacherAndTime（教师冲突检测）。
--      说明：这些语句里教师条件是**唯一能大幅收窄的谓词**（其余只有 del_flag），因此 teacher_id
--            必须打头；semester_id 作第二列承接「教师 + 学期」的组合，两条都用得上。
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'tpm_course_offering'
      AND index_name = 'idx_offering_teacher');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `tpm_course_offering`
       ADD INDEX `idx_offering_teacher` (`teacher_id`, `semester_id`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2) brm_teacher（教师；建表时零二级索引，工号/用户两条等值访问全表扫描）
-- ---------------------------------------------------------------------------

-- 2.1 idx_teacher_code (teacher_code)
--      服务：selectBrmTeacherByTeacherCode（导入时**逐行**校验工号唯一 → 典型 N+1，
--            Excel 一次导入 N 行就是 N 次全表扫描）；按工号查教师、外部系统对接。
--      注意：**普通索引不是 UNIQUE**，理由见头注 ④。
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'brm_teacher'
      AND index_name = 'idx_teacher_code');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `brm_teacher`
       ADD INDEX `idx_teacher_code` (`teacher_code`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2.2 idx_teacher_user (user_id)
--      服务：selectBrmTeacherByUserId（门户登录后按当前用户取教师档案，每次进门户都要走）、
--            updateBrmTeacherByUserId / deleteBrmTeacherByUserId，
--            以及 SysUserMapper 的 `left join brm_teacher bt on u.user_id = bt.user_id`（用户列表）。
--      说明：user_id 允许 NULL（未绑定系统账号的教师），普通索引，不能建 UNIQUE。
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'brm_teacher'
      AND index_name = 'idx_teacher_user');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `brm_teacher`
       ADD INDEX `idx_teacher_user` (`user_id`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2.3 idx_teacher_dept_status (dept_id, status)
--      服务：selectBrmTeacherList 的部门树数据范围（`t.dept_id = ? or t.dept_id in (子树)`，
--            OR 两侧都能走本索引）+ 状态筛选；selectTeacherPool（`status = '0' and dept_id = ?`，
--            排课引擎的教师候选池）。
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'brm_teacher'
      AND index_name = 'idx_teacher_dept_status');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `brm_teacher`
       ADD INDEX `idx_teacher_dept_status` (`dept_id`, `status`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 3) tpm_schedule_adjustment（调停课申请；门户与移动端审批两条入口）
-- ---------------------------------------------------------------------------

-- 3.1 idx_adjust_appr_status (approve_status, del_flag, create_time)
--      服务：PortalAdjustmentController.pendingList（固定 `approve_status='0'`，移动端审批待办）、
--            管理端列表按审批状态筛选、`selectTpmScheduleAdjustmentList` 的 `order by create_time desc`。
--      列序：两个等值列打头，create_time 垫尾，使「待审 + 未删 + 按提交时间倒序」可走索引反向
--            扫描。**实测结果（V5.32，scratch 库 40,000 行）**：`PortalAdjustmentController.pendingList`
--            带 5 个 LEFT JOIN 时仍出现 `Backward index scan` 且 `Using filesort` 消失——能否免排序
--            取决于**驱动表能否保序**（过滤列与排序列同属一条索引），与后面挂几个 join 无关。
--      说明：**不用报告的 (approve_by, approve_status)** —— approve_by 只写不读，见头注 ①。
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'tpm_schedule_adjustment'
      AND index_name = 'idx_adjust_appr_status');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `tpm_schedule_adjustment`
       ADD INDEX `idx_adjust_appr_status` (`approve_status`, `del_flag`, `create_time`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3.2 idx_adjust_creator (create_by, del_flag, create_time)
--      服务：PortalAdjustmentController.list 的 `a.create_by = #{createByExact}`（教师端「我的调停课申请」，
--            非 admin 一律强制本人）+ 撤销/审批时的归属校验列表；同样承接 create_time desc 排序。
--      说明：applicant 是展示昵称且为 LIKE 模糊检索，**不能**替代 create_by 作索引列。
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'tpm_schedule_adjustment'
      AND index_name = 'idx_adjust_creator');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `tpm_schedule_adjustment`
       ADD INDEX `idx_adjust_creator` (`create_by`, `del_flag`, `create_time`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 4) sys_user_role（用户-角色关联；主键 (user_id, role_id) 服务不了「按角色查用户」）
-- ---------------------------------------------------------------------------

-- 4.1 idx_user_role_role (role_id, user_id)
--      服务：countUserRoleByRoleId（删除角色前的「已分配,不能删除」判定）、
--            deleteUserRoleInfos（按角色批量解除授权）、
--            SysMenuMapper 的 `left join sys_user_role ur on rm.role_id = ur.role_id`（角色菜单树）、
--            SysUserMapper 的 `inner join sys_user_role ur on ... and ur.role_id = #{roleId}`。
--      列序：role_id 打头才能从「全索引扫描主键」变成 ref 定位；再带 user_id 使其成为覆盖索引
--            （上述语句只需要 user_id），避免回表。与 PRIMARY(user_id, role_id) 列序相反，不构成前缀冗余。
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_user_role'
      AND index_name = 'idx_user_role_role');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `sys_user_role`
       ADD INDEX `idx_user_role_role` (`role_id`, `user_id`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 5) 校验：8 条索引全部存在（期望 remaining_hot_index = 8）
-- ---------------------------------------------------------------------------
SELECT
    COUNT(DISTINCT index_name) AS remaining_hot_index,
    GROUP_CONCAT(DISTINCT CONCAT(table_name, '.', index_name) ORDER BY table_name, index_name) AS found
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND index_name IN ('idx_offering_semester_status', 'idx_offering_teacher',
                     'idx_teacher_code', 'idx_teacher_user', 'idx_teacher_dept_status',
                     'idx_adjust_appr_status', 'idx_adjust_creator',
                     'idx_user_role_role');
