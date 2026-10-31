-- =====================================================================================
-- 变更：V20261031.3 —— 四张热点表补组合二级索引（缺陷 P-1 / 架构 A-7）
-- 背景：CI 基线 121 张表合计仅 73 个二级索引，其中 84 张表零二级索引。选课
--       （tpm_selection_enrollment）、课表（tpm_schedule）、成绩（aem_grade_record）、
--       用户（sys_user）四张表在万级数据下即退化为全表扫描，并被 P-4 的 N+1 放大。
-- 做法：**按真实查询模式建索引，不照抄缺陷报告的字面清单**——盘点后有三条建议在本仓
--       物理上不成立或会改坏业务，已在本批纠正（详见下方「口径纠正」）：
--         ① tpm_schedule 根本没有 semester_id / teacher_id / class_id 列（学期与教师挂在
--            tpm_course_offering 上，经 offering_id 关联）→ 报告的
--            (semester_id, teacher_id)、(semester_id, class_id) 无法建立；
--         ② aem_grade_record 没有 offering_id 列 → 报告的 (offering_id, submit_status) 无法建立；
--         ③ sys_user 不能建 UNIQUE(user_name)：删除账号是软删（del_flag='2'）且不清空
--            user_name，而 checkUserNameUnique 只查 del_flag='0' —— 唯一键会让「删号后
--            重建同名账号」直接 1062。这里只建普通索引（真正的唯一性哪天要做，须沿用
--            V20261031_1 的「STORED 生成列 + 唯一索引」形态，只约束活跃行）。
-- 索引取舍：每条索引都写明"服务哪条语句"，避免冗余前缀重复建索引（写放大）。
-- 未纳入本批（余面）：tpm_course_offering(semester_id, teacher_id, status)、
--       brm_teacher、tpm_schedule_adjustment(approve_by, approve_status)、sys_user_role(role_id)，
--       以及 aem_grade_record 的「(student_id, course_id, semester_id, exam_type) 唯一键」——
--       后者是数据约束（P-2 家族）而非性能索引，且需先清洗存量重复，另批处理。
-- 影响表：tpm_selection_enrollment / tpm_schedule / aem_grade_record / sys_user（只加索引）
-- 幂等性：是（每条索引均按 information_schema.statistics 判存在，存在则跳过）
-- 回滚脚本：db/rollback/V20261031_3__add_hot_table_indexes.sql
-- 执行方式：mysql --host=127.0.0.1 --port=3306 -uroot -p yu-cjlujwc < db/migration/V20261031_3__add_hot_table_indexes.sql
--           （客户端参数写 --host= / --port=，不要写 -h127.0.0.1：会被当成主机名 127 报 Unknown MySQL server host）
-- 注意事项：
--   1) MySQL 8 加二级索引走 ALGORITHM=INPLACE，允许并发 DML，不锁写入；但万级表仍会
--      消耗 IO 与临时空间，建议避开选课高峰执行（本批实测见台账）。
--   2) 本脚本**不改任何数据**，回滚只需 DROP INDEX，因此可以先于应用版本单独上线。
-- =====================================================================================

-- ---------------------------------------------------------------------------
-- 0) 体检：执行前打印四张表的现有二级索引（用于执行前后对照，不是断言）
-- ---------------------------------------------------------------------------
SELECT table_name, index_name,
       GROUP_CONCAT(column_name ORDER BY seq_in_index) AS idx_cols,
       IF(non_unique = 0, 'UNIQUE', 'INDEX') AS kind
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN ('tpm_selection_enrollment', 'tpm_schedule', 'aem_grade_record', 'sys_user')
GROUP BY table_name, index_name, non_unique
ORDER BY table_name, index_name;

-- ---------------------------------------------------------------------------
-- 1) tpm_selection_enrollment（选课，写多读也多）
-- ---------------------------------------------------------------------------

-- 1.1 idx_enroll_offering (course_offering_id, result_status, del_flag)
--      服务：countEnrolledByOffering（准入前必调）、selectCountByOffering（!= '3' 走 range）、
--            countAdmittedByOffering、selectWaitlistByOffering（前缀收窄到单开课）。
--      列序：等值列 course_offering_id 打头（区分度最高，单开课行数 = 容量量级），
--            result_status 承接等值/范围，del_flag 垫尾使 count(1) 可走覆盖索引不回表。
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'tpm_selection_enrollment'
      AND index_name = 'idx_enroll_offering');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `tpm_selection_enrollment`
       ADD INDEX `idx_enroll_offering` (`course_offering_id`, `result_status`, `del_flag`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.2 idx_enroll_student (student_id, result_status, del_flag)
--      服务：selectByStudentAndRound（已选门数上限 / 时间与学分冲突的唯一数据源）、
--            countCompletedCourses（覆盖索引，不回表）。
--      说明：uk_enroll_active 以 round_id 打头，无法服务「只按学生查」的访问路径，
--            故另建 student_id 打头的索引；不要因为"唯一键里有 student_id"就跳过。
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'tpm_selection_enrollment'
      AND index_name = 'idx_enroll_student');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `tpm_selection_enrollment`
       ADD INDEX `idx_enroll_student` (`student_id`, `result_status`, `del_flag`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2) tpm_schedule（课表；建表时零二级索引，冲突检测全表扫描）
-- ---------------------------------------------------------------------------

-- 2.1 idx_schedule_offering (offering_id, week_day, start_period)
--      服务：selectByOfferingIds、selectTpmScheduleList、selectSchedulesBySemester、
--            selectStudentScheduleList、selectStudentScheduleSlotsBySemester 等经
--            offering_id 关联/过滤的语句；同时把 `order by week_day, start_period`
--            变成索引有序扫描（省 filesort）。
--      列序：offering_id 等值打头，随后两列直接承接排序，故不再单独建 (offering_id)。
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'tpm_schedule'
      AND index_name = 'idx_schedule_offering');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `tpm_schedule`
       ADD INDEX `idx_schedule_offering` (`offering_id`, `week_day`, `start_period`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2.2 idx_schedule_classroom (classroom_id, week_day, start_period)
--      服务：selectByClassroomAndTime / ...WithInfo / ...TimeRange（教室冲突检测），
--            以及排课引擎按教室回填时间片。
--      说明：时间重叠条件是 `end_period >= ? and start_period <= ?`，等值列 + week_day
--            先把候选集压到「同一教室同一天」，start_period 再收窄上界；end_period
--            参与过滤但不入索引（它在重叠判定里是下界，进了也不会再缩小范围）。
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'tpm_schedule'
      AND index_name = 'idx_schedule_classroom');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `tpm_schedule`
       ADD INDEX `idx_schedule_classroom` (`classroom_id`, `week_day`, `start_period`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 3) aem_grade_record（成绩；现有唯一二级索引 idx_grade_submit_status 只覆盖审核流）
-- ---------------------------------------------------------------------------

-- 3.1 idx_grade_student_semester (student_id, semester_id)
--      服务：selectByStudentAndSemester、selectAemGradeRecordListForPortal（门户成绩页，
--            以及 GPA / 学分统计的取数入口）。排序 `semester_id desc` 可由索引反向扫描。
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'aem_grade_record'
      AND index_name = 'idx_grade_student_semester');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `aem_grade_record`
       ADD INDEX `idx_grade_student_semester` (`student_id`, `semester_id`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3.2 idx_grade_semester_course (semester_id, course_id)
--      服务：selectBySemester（按学期整表取数）、期次级成绩统计与及格率聚合、
--            以及管理端「学期 + 课程」组合筛选。
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'aem_grade_record'
      AND index_name = 'idx_grade_semester_course');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `aem_grade_record`
       ADD INDEX `idx_grade_semester_course` (`semester_id`, `course_id`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 4) sys_user（用户；建表时零二级索引，登录链路每次全表扫描）
-- ---------------------------------------------------------------------------

-- 4.1 idx_user_name (user_name, del_flag)
--      服务：selectUserByUserName（登录 + 每次令牌续期取用户）、checkUserNameUnique。
--      注意：**不是 UNIQUE**——软删除不清 user_name，唯一键会挡住「删号后重建同名账号」。
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_user'
      AND index_name = 'idx_user_name');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `sys_user`
       ADD INDEX `idx_user_name` (`user_name`, `del_flag`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4.2 idx_user_dept_status (dept_id, status)
--      服务：selectUserList 的部门树过滤（`u.dept_id = ? or u.dept_id in (子树)`，
--            OR 两侧都可走本索引）+ 账号状态筛选；也服务按部门统计人员。
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_user'
      AND index_name = 'idx_user_dept_status');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `sys_user`
       ADD INDEX `idx_user_dept_status` (`dept_id`, `status`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 5) 校验：8 条索引全部存在，且打印各自列序（期望 hot_index_count = 8）
-- ---------------------------------------------------------------------------
SELECT
    COUNT(DISTINCT index_name) AS hot_index_count,
    GROUP_CONCAT(DISTINCT CONCAT(table_name, '.', index_name) ORDER BY table_name, index_name) AS found
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND index_name IN ('idx_enroll_offering', 'idx_enroll_student',
                     'idx_schedule_offering', 'idx_schedule_classroom',
                     'idx_grade_student_semester', 'idx_grade_semester_course',
                     'idx_user_name', 'idx_user_dept_status');
