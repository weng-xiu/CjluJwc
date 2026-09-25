-- 测验种子（本地测试库一次性数据，非通用升级脚本）
-- 1) 学生 wxl(user_id=101) 绑定学籍 10001
UPDATE sam_student SET user_id = 101 WHERE student_id = 10001;
-- 2) 教师账号 2001(user_id=100)：建 teacher_id=100 档案并接管两门开课
UPDATE brm_teacher SET user_id = 100 WHERE teacher_id = 1;
INSERT INTO brm_teacher (teacher_id, teacher_code, teacher_name, user_id, gender, dept_id, title, status, create_by, create_time)
SELECT 100, 'T100', 'weng(测试)', 100, '1', 101, '教授', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM brm_teacher WHERE teacher_id = 100);
UPDATE tpm_course_offering SET teacher_id = 100 WHERE offering_id IN (9321, 9322);
-- 3) 密码统一为 admin123（与 user_id=1 同 hash）
SET @h = (SELECT password FROM (SELECT password FROM sys_user WHERE user_id = 1) t);
UPDATE sys_user SET password = @h WHERE user_id IN (100, 101);
-- 4) 校验
SELECT u.user_id, u.user_name, u.nick_name, ur.role_id,
       (SELECT student_id FROM sam_student s WHERE s.user_id = u.user_id) AS bound_student,
       (SELECT teacher_id FROM brm_teacher t WHERE t.teacher_id = u.user_id) AS bound_teacher
FROM sys_user u JOIN sys_user_role ur ON u.user_id = ur.user_id WHERE u.user_id IN (100, 101);
