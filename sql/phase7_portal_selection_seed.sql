-- ============================================================
-- phase7_portal_selection_seed.sql
-- 移动端门户演示种子数据（学生 wxl user_id=101）
-- 覆盖 选课/成绩/课表/预警 四页
-- 特性：固定 93xx 主键 + INSERT IGNORE，可重复执行
-- 前置：phase6_portal_mobile_seed.sql（教室 9001-9004）；semester_id=1；teacher_id=1；campus_id=1
-- 执行：mysql -uroot -p --default-character-set=utf8mb4 -D yu-cjlujwc -e "source .../phase7_portal_selection_seed.sql"
-- ============================================================

-- ---------- 1. 课程库 tpm_course_library ----------
INSERT IGNORE INTO tpm_course_library
(course_id, course_code, course_name, credit, theory_hours, practice_hours, total_hours, course_type, plan_id, status, create_by, create_time)
VALUES
(9301, 'MATH101', '高等数学',   5.0, 80, 0,  80, '0', 1, '0', 'admin', NOW()),
(9302, 'ENG101',  '大学英语',   4.0, 64, 0,  64, '0', 1, '0', 'admin', NOW()),
(9303, 'CS201',   '数据结构',   4.0, 48, 16, 64, '0', 1, '0', 'admin', NOW()),
(9304, 'CS301',   '计算机网络', 3.0, 40, 8,  48, '0', 1, '0', 'admin', NOW()),
(9305, 'CS302',   '操作系统',   4.0, 48, 16, 64, '0', 1, '0', 'admin', NOW()),
(9306, 'MATH102', '线性代数',   3.0, 48, 0,  48, '0', 1, '0', 'admin', NOW());

-- ---------- 2. 选课轮次 tpm_selection_round（round_status=1 进行中） ----------
INSERT IGNORE INTO tpm_selection_round
(round_id, semester_id, round_name, round_order, start_time, end_time, max_courses_per_student, round_status, status, del_flag, create_by, create_time)
VALUES
(9311, 1, '2026上学期第1轮选课', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 8, '1', '0', '0', 'admin', NOW());

-- ---------- 3. 开课计划 tpm_course_offering（offering_status=1 已确认） ----------
INSERT IGNORE INTO tpm_course_offering
(offering_id, semester_id, course_id, teacher_id, campus_id, class_count, max_students, offering_status, status, del_flag, create_by, create_time)
VALUES
(9321, 1, 9301, 1, 1, 2, 60, '1', '0', '0', 'admin', NOW()),
(9322, 1, 9302, 1, 1, 2, 60, '1', '0', '0', 'admin', NOW()),
(9323, 1, 9303, 1, 1, 2, 80, '1', '0', '0', 'admin', NOW()),
(9324, 1, 9304, 1, 1, 1, 80, '1', '0', '0', 'admin', NOW()),
(9325, 1, 9305, 1, 1, 1, 60, '1', '0', '0', 'admin', NOW()),
(9326, 1, 9306, 1, 1, 1, 60, '1', '0', '0', 'admin', NOW());

-- ---------- 4. 排课 tpm_schedule（每门课一条，供学生课表展示） ----------
INSERT IGNORE INTO tpm_schedule
(schedule_id, offering_id, classroom_id, week_day, start_period, end_period, start_week, end_week, schedule_type, status, del_flag, create_by, create_time)
VALUES
(9331, 9321, 9001, 1, 1, 2, 1, 16, '0', '0', '0', 'admin', NOW()),
(9332, 9322, 9002, 2, 3, 4, 1, 16, '0', '0', '0', 'admin', NOW()),
(9333, 9323, 9003, 3, 1, 2, 1, 16, '0', '0', '0', 'admin', NOW()),
(9334, 9324, 9004, 4, 5, 6, 1, 16, '0', '0', '0', 'admin', NOW()),
(9335, 9325, 9001, 5, 1, 2, 1, 16, '0', '0', '0', 'admin', NOW()),
(9336, 9326, 9002, 2, 1, 2, 1, 16, '0', '0', '0', 'admin', NOW());

-- ---------- 5. 选课记录 tpm_selection_enrollment（学生101已选中前4门 result_status=1） ----------
INSERT IGNORE INTO tpm_selection_enrollment
(enroll_id, round_id, student_id, course_offering_id, select_time, lottery_result, result_status, status, del_flag, create_by, create_time)
VALUES
(9341, 9311, 101, 9321, DATE_SUB(NOW(), INTERVAL 2 DAY), '1', '1', '0', '0', 'admin', NOW()),
(9342, 9311, 101, 9322, DATE_SUB(NOW(), INTERVAL 2 DAY), '1', '1', '0', '0', 'admin', NOW()),
(9343, 9311, 101, 9323, DATE_SUB(NOW(), INTERVAL 2 DAY), '1', '1', '0', '0', 'admin', NOW()),
(9344, 9311, 101, 9324, DATE_SUB(NOW(), INTERVAL 2 DAY), '1', '1', '0', '0', 'admin', NOW());

-- ---------- 6. 成绩记录 aem_grade_record（学生101已完成3门 exam_type=0 正考） ----------
INSERT IGNORE INTO aem_grade_record
(grade_id, student_id, course_id, semester_id, exam_type, regular_score, exam_score, total_score, grade_point, grade_level, is_pass, is_reviewed, status, create_by, create_time)
VALUES
(9351, 101, 9301, 1, '0', 85.0, 88.0, 87.0, 3.7, 'B', '1', '0', '0', 'admin', NOW()),
(9352, 101, 9302, 1, '0', 90.0, 92.0, 91.5, 4.0, 'A', '1', '0', '0', 'admin', NOW()),
(9353, 101, 9303, 1, '0', 70.0, 78.0, 75.0, 2.7, 'C', '1', '0', '0', 'admin', NOW());

-- ---------- 7. 学业预警 sam_warning（学生101，多类型/多级别） ----------
INSERT IGNORE INTO sam_warning
(warning_id, student_id, semester_id, warning_type, warning_level, warning_reason, warning_date, is_resolved, status, create_by, create_time)
VALUES
(9361, 101, 1, '0', '0', '平均绩点偏低（GPA 3.5），请注意学习状态',   DATE_SUB(NOW(), INTERVAL 5 DAY), '0', '0', 'admin', NOW()),
(9362, 101, 1, '1', '1', '本学期已获学分未达进度要求，请及时选课补课', DATE_SUB(NOW(), INTERVAL 5 DAY), '0', '0', 'admin', NOW()),
(9363, 101, 1, '2', '0', '个别课程出勤率偏低，请注意课堂考勤',         DATE_SUB(NOW(), INTERVAL 3 DAY), '0', '0', 'admin', NOW());

-- ---------- 校验 ----------
SELECT 'course_lib' t, COUNT(*) c FROM tpm_course_library        WHERE course_id         BETWEEN 9300 AND 9399
UNION ALL SELECT 'round', COUNT(*)       FROM tpm_selection_round        WHERE round_id        BETWEEN 9300 AND 9399
UNION ALL SELECT 'offering', COUNT(*)    FROM tpm_course_offering        WHERE offering_id     BETWEEN 9300 AND 9399
UNION ALL SELECT 'schedule', COUNT(*)    FROM tpm_schedule               WHERE schedule_id     BETWEEN 9300 AND 9399
UNION ALL SELECT 'enrollment', COUNT(*)  FROM tpm_selection_enrollment   WHERE enroll_id       BETWEEN 9300 AND 9399
UNION ALL SELECT 'grade', COUNT(*)       FROM aem_grade_record           WHERE grade_id        BETWEEN 9300 AND 9399
UNION ALL SELECT 'warning', COUNT(*)     FROM sam_warning                WHERE warning_id      BETWEEN 9300 AND 9399;
