-- ============================================================
-- G4 CI 基线 · 合成测试账号与业务锚点行（静态维护，非导出产物）
-- 口径：全部为合成/脱敏数据，不含任何真实个人信息（id_card/手机号/邮箱置 NULL）。
--       与 bin/seed_p5_test_accounts.sql、testdata/README.md 的账号约定一致：
--       admin(user 1) / weng(user 100, user_name=2001, teacher 100) / wxl(user 101, student 101)
--       密码统一 admin123（RuoYi 公开默认 BCrypt 散列）。
-- 幂等：先 DELETE 后 INSERT，可重复执行。
-- CI 初始化顺序：01-schema → 02-meta → 本文件 → sql/phase7_portal_selection_seed.sql → testdata/reset-all.sql
-- ============================================================
SET NAMES utf8mb4;

-- ---------- 1. 系统账号 ----------
DELETE FROM sys_user WHERE user_id IN (1, 100, 101);
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_by, create_time, remark) VALUES
(1,   101,  'admin', '教务管理员',  '00', NULL, NULL, '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', NOW(), 'admin', NOW(), 'CI 合成超管'),
(100, NULL, '2001',  'weng(测试)', '00', NULL, NULL, '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '', NULL, 'admin', NOW(), 'CI 合成教师'),
(101, 201,  'wxl',   'wxl(测试)',  '00', NULL, NULL, '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '', NULL, 'admin', NOW(), 'CI 合成学生');

DELETE FROM sys_user_role WHERE user_id IN (1, 100, 101);
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1), (100, 6), (101, 7);

DELETE FROM sys_user_post WHERE user_id IN (1, 100, 101);
INSERT INTO sys_user_post (user_id, post_id) VALUES (1, 1);

-- ---------- 2. 业务档案锚点 ----------
DELETE FROM brm_teacher WHERE teacher_id IN (1, 100);
INSERT INTO brm_teacher (teacher_id, teacher_code, teacher_name, user_id, gender, dept_id, title, status, create_by, create_time) VALUES
(1,   '2001', 'weng(测试)', 100, '0', 201, NULL,   '0', 'admin', NOW()),
(100, 'T100', 'weng(测试)', 100, '1', 101, '教授', '0', 'admin', NOW());

DELETE FROM sam_student WHERE student_id IN (101, 10001);
INSERT INTO sam_student (student_id, student_no, student_name, gender, major_id, class_id, dept_id, education_level, enrollment_year, birth_date, id_card, user_id, student_status, status, remark, create_by, create_time) VALUES
(101,   '2026010101', 'wxl(测试)', '0', 1, 301, 201, '本科', 2026, NULL, NULL, 101, '1', '1', 'CI 合成学生档案', 'admin', NOW()),
(10001, '2026010102', 'wxl(测试)', '0', 1, 301, 201, '本科', 2026, NULL, NULL, 101, '1', '1', 'CI 合成学籍备用行', 'admin', NOW());

-- ---------- 3. 学期 / 校区锚点 ----------
DELETE FROM brm_semester WHERE semester_id = 1;
INSERT INTO brm_semester (semester_id, semester_name, academic_year_id, start_date, end_date, semester_order, status, create_by, create_time) VALUES
(1, '2026上学期', 1, '2026-01-01', '2026-06-30', 1, '0', 'admin', NOW());

DELETE FROM brm_campus WHERE campus_id = 1;
INSERT INTO brm_campus (campus_id, campus_name, campus_address, order_num, status, create_by, create_time) VALUES
(1, '东校区', NULL, 0, '0', 'admin', NOW());

-- ---------- 4. 校验 ----------
SELECT 'ci_anchor_users' AS metric, COUNT(*) AS c FROM sys_user WHERE user_id IN (1,100,101)
UNION ALL SELECT 'ci_teacher', COUNT(*) FROM brm_teacher WHERE teacher_id IN (1,100)
UNION ALL SELECT 'ci_student', COUNT(*) FROM sam_student WHERE student_id IN (101,10001)
UNION ALL SELECT 'ci_semester', COUNT(*) FROM brm_semester WHERE semester_id = 1
UNION ALL SELECT 'ci_campus', COUNT(*) FROM brm_campus WHERE campus_id = 1;
