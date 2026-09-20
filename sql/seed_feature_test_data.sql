SET NAMES utf8mb4;
-- ===============================================
-- 功能测试种子数据（S4/A3/O2/D2）幂等：先删后插，使用高位专用ID，避免污染既有数据
-- 仅供本地演示库 yu-cjlujwc 功能验证使用
-- ===============================================

-- ---------- S4 学生毕业预审：student_id=101（wxl 用户），归属 major=1 / 2026 → plan 1 ----------
DELETE FROM sam_student WHERE student_id = 101;
INSERT INTO sam_student (student_id, student_no, student_name, gender, major_id, dept_id, class_id, enrollment_year, education_level, student_status, status, create_by, create_time)
VALUES (101, '2026010101', '测试学生', '0', 1, 201, 301, '2026', '本科', '0', '0', 'admin', NOW());

-- 课程属性标记（供英语/体育/分项学分口径）
UPDATE tpm_course_library SET course_category = 'PUBLIC_BASE'      WHERE course_id IN (9301, 9302);
UPDATE tpm_course_library SET course_category = 'MAJOR'            WHERE course_id IN (9303, 9306);
UPDATE tpm_course_library SET course_category = 'FOREIGN_LANGUAGE' WHERE course_id = 9304;
UPDATE tpm_course_library SET course_category = 'PE'               WHERE course_id = 9305;

-- 补充成绩：9304(外语)不及格、9305(体育)通过、9306(专业)通过；9301-9303 已存在为通过
DELETE FROM aem_grade_record WHERE grade_id IN (9401, 9402, 9403);
INSERT INTO aem_grade_record (grade_id, student_id, course_id, semester_id, is_pass, status, create_by, create_time)
VALUES (9401, 101, 9304, 1, '0', '0', 'admin', NOW()),
       (9402, 101, 9305, 1, '1', '0', 'admin', NOW()),
       (9403, 101, 9306, 1, '1', '0', 'admin', NOW());

-- plan 1 学分结构分项
DELETE FROM tpm_credit_structure WHERE struct_id BETWEEN 9501 AND 9504;
INSERT INTO tpm_credit_structure (struct_id, plan_id, credit_type, credit_type_name, required_credit, min_credit, status, del_flag, create_by, create_time)
VALUES (9501, 1, 'PUBLIC_BASE', '公共基础课', 30.0, 30.0, '0', '0', 'admin', NOW()),
       (9502, 1, 'MAJOR', '专业课', 60.0, 60.0, '0', '0', 'admin', NOW()),
       (9503, 1, 'FOREIGN_LANGUAGE', '外语课', 10.0, 10.0, '0', '0', 'admin', NOW()),
       (9504, 1, 'PE', '体育课', 4.0, 4.0, '0', '0', 'admin', NOW());

-- ---------- O2 会议室容量校验 + 占用日历 ----------
DELETE FROM oa_meeting_room WHERE room_id IN (4001, 4002);
INSERT INTO oa_meeting_room (room_id, room_name, room_location, capacity, equipment, admin_id, admin_name, status, create_by, create_time)
VALUES (4001, '第一会议室', '行政楼201', 5, '投影仪,白板', 1, 'admin', '0', 'admin', NOW()),
       (4002, '学术报告厅', '图书馆1楼', 50, '音响,大屏,录播', 1, 'admin', '0', 'admin', NOW());

DELETE FROM oa_meeting WHERE meeting_id IN (4101, 4102, 4103);
INSERT INTO oa_meeting (meeting_id, meeting_theme, room_id, room_name, start_time, end_time, organizer_id, organizer_name, meeting_type, meeting_status, content, status, create_by, create_time)
VALUES (4101, '教务周例会', 4001, '第一会议室', '2026-09-20 10:00:00', '2026-09-20 11:00:00', 1, 'admin', '0', '2', '教学安排沟通', '0', 'admin', NOW()),
       (4102, '学科建设研讨', 4001, '第一会议室', '2026-09-20 15:00:00', '2026-09-20 16:30:00', 1, 'admin', '0', '2', '学科建设', '0', 'admin', NOW()),
       (4103, '迎新工作部署', 4002, '学术报告厅', '2026-09-20 09:00:00', '2026-09-20 10:30:00', 1, 'admin', '0', '1', '迎新方案', '0', 'admin', NOW());

-- ---------- A3 监考派发：多教师 + 课程任课回避 + 考场座位 ----------
DELETE FROM brm_teacher WHERE teacher_id BETWEEN 2 AND 7;
INSERT INTO brm_teacher (teacher_id, teacher_code, teacher_name, dept_id, status, create_by, create_time)
VALUES (2, 'T1002', '张老师', 202, '0', 'admin', NOW()),
       (3, 'T1003', '李老师', 203, '0', 'admin', NOW()),
       (4, 'T1004', '王老师', 201, '0', 'admin', NOW()),
       (5, 'T1005', '赵老师', 202, '0', 'admin', NOW()),
       (6, 'T1006', '孙老师', 203, '0', 'admin', NOW()),
       (7, 'T1007', '周老师', 204, '0', 'admin', NOW());
UPDATE brm_teacher SET dept_id = 201 WHERE teacher_id = 1;

-- 考试 9001 关联课程 9301（其任课教师=teacher 1，应被硬回避；其院系 201 应被软回避）
UPDATE aem_exam_plan SET course_id = 9301 WHERE exam_id = 9001;
DELETE FROM tpm_course_offering WHERE offering_id = 9321;
INSERT INTO tpm_course_offering (offering_id, semester_id, course_id, teacher_id, status, del_flag, create_by, create_time)
VALUES (9321, 1, 9301, 1, '0', '0', 'admin', NOW());

-- 考试 9001 座位编排（3 个考场）
DELETE FROM aem_exam_seat WHERE exam_id = 9001;
INSERT INTO aem_exam_seat (seat_id, exam_id, classroom_id, student_id, seat_number, status, create_by, create_time)
VALUES (9601, 9001, 5001, 101, 1, '0', 'admin', NOW()),
       (9602, 9001, 5001, 102, 2, '0', 'admin', NOW()),
       (9603, 9001, 5002, 103, 1, '0', 'admin', NOW()),
       (9604, 9001, 5002, 104, 2, '0', 'admin', NOW()),
       (9605, 9001, 5003, 105, 1, '0', 'admin', NOW()),
       (9606, 9001, 5003, 106, 2, '0', 'admin', NOW());

-- ---------- D2 同步可靠性：外部系统 + 接口 + 任务（指向不可达端点以演示失败告警+批次留痕） ----------
DELETE FROM dis_external_system WHERE system_id = 7001;
INSERT INTO dis_external_system (system_id, system_name, system_code, system_type, base_url, auth_type, status, create_by, create_time)
VALUES (7001, '本地演示源', 'DEMO_LOCAL', 'HTTP', 'http://127.0.0.1:59999', 'TOKEN', '0', 'admin', NOW());

DELETE FROM dis_interface_config WHERE interface_id = 3001;
INSERT INTO dis_interface_config (interface_id, system_id, interface_name, interface_code, request_method, request_path, request_template, timeout_seconds, retry_count, status, create_by, create_time)
VALUES (3001, 7001, '课程增量同步接口', 'COURSE_SYNC_API', 'GET', 'http://127.0.0.1:59999/sync/course?since=${watermark}', NULL, 5, 1, '0', 'admin', NOW());

DELETE FROM dis_sync_task WHERE task_id = 5001;
INSERT INTO dis_sync_task (task_id, task_name, task_code, system_id, interface_id, cron_expression, execute_count, fail_count, sync_mode, last_watermark, status, create_by, create_time)
VALUES (5001, '课程数据增量同步', 'SYNC_COURSE_DEMO', 7001, 3001, NULL, 0, 0, '1', NULL, '0', 'admin', NOW());
