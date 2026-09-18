-- ============================================================
-- phase6_portal_mobile_seed.sql
-- 门户移动端演示种子数据：教室 / 考试安排 / 监考分配 / 评教问卷 / 评教题目
-- 用途：验证 mobile/exam、mobile/invigilation、mobile/evaluation 三页
-- 特性：固定 9000+ ID 段，先删后插，可重复执行
-- 执行：mysql -h 127.0.0.1 -uroot -D yu-cjlujwc --default-character-set=utf8mb4 -e "source .../phase6_portal_mobile_seed.sql"
-- ============================================================

-- ---------- 清理旧种子（幂等重跑） ----------
DELETE FROM aem_evaluation_question  WHERE question_id     BETWEEN 9000 AND 9999;
DELETE FROM aem_evaluation_questionnaire WHERE questionnaire_id BETWEEN 9000 AND 9999;
DELETE FROM aem_exam_invigilation    WHERE invigilation_id BETWEEN 9000 AND 9999;
DELETE FROM aem_exam_plan            WHERE exam_id         BETWEEN 9000 AND 9999;
DELETE FROM brm_classroom            WHERE classroom_id    BETWEEN 9000 AND 9999;

-- ---------- 教室（监考卡片"教室"名称来源） ----------
INSERT INTO brm_classroom (classroom_id, classroom_name, building_id, type_id, capacity, status, create_by, create_time) VALUES
(9001, '一教101', NULL, NULL, 60,  '0', 'seed', NOW()),
(9002, '一教102', NULL, NULL, 60,  '0', 'seed', NOW()),
(9003, '二教203', NULL, NULL, 120, '0', 'seed', NOW()),
(9004, '三教505（机考）', NULL, NULL, 80, '0', 'seed', NOW());

-- ---------- 考试安排（exam_type: 0期末 1补考 2重修；plan_status: 0未安排 1已安排 2已发布） ----------
INSERT INTO aem_exam_plan (exam_id, exam_name, semester_id, exam_type, course_id, exam_date, start_time, end_time, duration, total_students, plan_status, status, create_by, create_time, remark) VALUES
(9001, '2026秋季学期期末考试：高等数学A',       1, '0', NULL, '2026-12-24', '09:00', '11:00', 120, 240, '2', '0', 'seed', NOW(), '全校区统考'),
(9002, '2026秋季学期期末考试：大学英语',         1, '0', NULL, '2026-12-25', '14:00', '16:00', 120, 320, '2', '0', 'seed', NOW(), '含听力，请提前到场'),
(9003, '2026秋季学期期末考试：线性代数',         1, '0', NULL, '2026-12-26', '09:00', '11:00', 120, 180, '1', '0', 'seed', NOW(), NULL),
(9004, '2026秋季学期期末考试：数据结构',         1, '0', NULL, '2026-12-27', '14:00', '16:30', 150, 90,  '1', '0', 'seed', NOW(), '机考场次'),
(9005, '2026补考：概率论与数理统计',             1, '1', NULL, '2026-09-26', '09:00', '11:00', 120, 35,  '2', '0', 'seed', NOW(), '春季学期补考'),
(9006, '2026补考：大学物理',                     1, '1', NULL, '2026-09-27', '14:00', '16:00', 120, 28,  '0', '0', 'seed', NOW(), NULL),
(9007, '2026重修考试：复变函数',                 1, '2', NULL, '2026-10-17', '09:00', '11:00', 120, 12,  '1', '0', 'seed', NOW(), NULL),
(9008, '2026重修考试：思想政治理论实践课',       1, '2', NULL, '2026-12-28', '14:00', '15:30', 90,  8,   '0', '0', 'seed', NOW(), NULL);

-- ---------- 监考分配（duty_type: 0主监考 1副监考 2巡考；teacher_id=1 为演示教师 wengxiulin） ----------
INSERT INTO aem_exam_invigilation (invigilation_id, exam_id, classroom_id, teacher_id, exam_date, start_time, end_time, duty_type, status, create_by, create_time, remark) VALUES
(9001, 9001, 9001, 1, '2026-12-24', '09:00', '11:00', '0', '0', 'seed', NOW(), '主监考'),
(9002, 9001, 9002, 1, '2026-12-24', '09:00', '11:00', '1', '0', 'seed', NOW(), '副监考'),
(9003, 9002, 9003, 1, '2026-12-25', '14:00', '16:00', '0', '0', 'seed', NOW(), NULL),
(9004, 9002, 9001, 1, '2026-12-25', '14:00', '16:00', '2', '0', 'seed', NOW(), '楼层巡视'),
(9005, 9003, 9002, 1, '2026-12-26', '09:00', '11:00', '0', '0', 'seed', NOW(), NULL),
(9006, 9004, 9004, 1, '2026-12-27', '14:00', '16:30', '1', '0', 'seed', NOW(), '机房场次'),
(9007, 9005, 9001, 1, '2026-09-26', '09:00', '11:00', '0', '0', 'seed', NOW(), '补考监考'),
(9008, 9005, 9002, 1, '2026-09-26', '09:00', '11:00', '1', '0', 'seed', NOW(), NULL),
(9009, 9007, 9003, 1, '2026-10-17', '09:00', '11:00', '0', '0', 'seed', NOW(), NULL),
(9010, 9002, 9002, 1, '2026-12-25', '14:00', '16:00', '1', '0', 'seed', NOW(), NULL);

-- ---------- 评教问卷（eval_status: 0未开始 1进行中 2已结束） ----------
INSERT INTO aem_evaluation_questionnaire (questionnaire_id, semester_id, title, description, question_count, full_score, start_time, end_time, eval_status, is_anonymous, status, create_by, create_time) VALUES
(9001, 1, '2026秋季学期期中教学评价',     '对任课教师的教学态度、方法与效果进行评价，欢迎提出意见建议。', 6, 100.00, '2026-09-01 00:00:00', '2026-12-31 23:59:59', '1', '1', '0', 'seed', NOW()),
(9002, 1, '2026秋季学期期末教学评价（预告）', '期末评教将于第18周开启，敬请期待。',                     6, 100.00, '2026-12-20 00:00:00', '2027-01-10 23:59:59', '0', '1', '0', 'seed', NOW()),
(9003, 1, '2026春季学期教学评价（已截止）', '本次评教活动已结束，感谢您的参与。',                       6, 100.00, '2026-04-01 00:00:00', '2026-06-30 23:59:59', '2', '1', '0', 'seed', NOW());

-- ---------- 评教题目（question_type: 2评分 3文本；评分题 max_score=5 对应五星） ----------
INSERT INTO aem_evaluation_question (question_id, questionnaire_id, question_type, question_content, sort_order, max_score, options_json, status, create_by, create_time) VALUES
(9101, 9001, '2', '教学态度认真，备课充分',           1, 5.0, NULL, '0', 'seed', NOW()),
(9102, 9001, '2', '讲授条理清晰，重点突出',           2, 5.0, NULL, '0', 'seed', NOW()),
(9103, 9001, '2', '注重互动，课堂气氛活跃',           3, 5.0, NULL, '0', 'seed', NOW()),
(9104, 9001, '2', '作业批改及时，辅导耐心',           4, 5.0, NULL, '0', 'seed', NOW()),
(9105, 9001, '2', '整体教学效果满意',                 5, 5.0, NULL, '0', 'seed', NOW()),
(9106, 9001, '3', '请对本课程的教学提出具体建议',     6, 0.0, NULL, '0', 'seed', NOW()),
(9111, 9002, '2', '教学态度认真，备课充分',           1, 5.0, NULL, '0', 'seed', NOW()),
(9112, 9002, '2', '讲授条理清晰，重点突出',           2, 5.0, NULL, '0', 'seed', NOW()),
(9113, 9002, '2', '注重互动，课堂气氛活跃',           3, 5.0, NULL, '0', 'seed', NOW()),
(9114, 9002, '2', '作业批改及时，辅导耐心',           4, 5.0, NULL, '0', 'seed', NOW()),
(9115, 9002, '2', '整体教学效果满意',                 5, 5.0, NULL, '0', 'seed', NOW()),
(9116, 9002, '3', '请对本课程的教学提出具体建议',     6, 0.0, NULL, '0', 'seed', NOW()),
(9121, 9003, '2', '教学态度认真，备课充分',           1, 5.0, NULL, '0', 'seed', NOW()),
(9122, 9003, '2', '讲授条理清晰，重点突出',           2, 5.0, NULL, '0', 'seed', NOW()),
(9123, 9003, '2', '注重互动，课堂气氛活跃',           3, 5.0, NULL, '0', 'seed', NOW()),
(9124, 9003, '2', '作业批改及时，辅导耐心',           4, 5.0, NULL, '0', 'seed', NOW()),
(9125, 9003, '2', '整体教学效果满意',                 5, 5.0, NULL, '0', 'seed', NOW()),
(9126, 9003, '3', '请对本课程的教学提出具体建议',     6, 0.0, NULL, '0', 'seed', NOW());

-- ---------- 校验 ----------
SELECT 'classroom' t, COUNT(*) c FROM brm_classroom        WHERE classroom_id    BETWEEN 9000 AND 9999
UNION ALL SELECT 'exam_plan', COUNT(*)    FROM aem_exam_plan            WHERE exam_id         BETWEEN 9000 AND 9999
UNION ALL SELECT 'invigilation', COUNT(*) FROM aem_exam_invigilation    WHERE invigilation_id BETWEEN 9000 AND 9999
UNION ALL SELECT 'questionnaire', COUNT(*) FROM aem_evaluation_questionnaire WHERE questionnaire_id BETWEEN 9000 AND 9999
UNION ALL SELECT 'question', COUNT(*)     FROM aem_evaluation_question  WHERE question_id     BETWEEN 9000 AND 9999;
