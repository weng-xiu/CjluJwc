-- =============================================================
-- phase5_aem_optimization.sql  (MySQL 5.7 / 8.0 通用)
-- 考核与评价管理(AEM)模块优化脚本
-- 可重复执行
-- =============================================================

DROP PROCEDURE IF EXISTS p_aem_opt;
DELIMITER $$
CREATE PROCEDURE p_aem_opt()
BEGIN
    -- 安全加索引
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_exam_plan' AND index_name='idx_exam_plan_course') THEN
        ALTER TABLE aem_exam_plan ADD INDEX idx_exam_plan_course (course_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_exam_plan' AND index_name='idx_exam_plan_date') THEN
        ALTER TABLE aem_exam_plan ADD INDEX idx_exam_plan_date (exam_date); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_exam_plan' AND index_name='idx_exam_plan_sem_course') THEN
        ALTER TABLE aem_exam_plan ADD INDEX idx_exam_plan_sem_course (semester_id, course_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_exam_plan' AND index_name='idx_exam_plan_status') THEN
        ALTER TABLE aem_exam_plan ADD INDEX idx_exam_plan_status (plan_status, status); END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_exam_invigilation' AND index_name='idx_invig_teacher') THEN
        ALTER TABLE aem_exam_invigilation ADD INDEX idx_invig_teacher (teacher_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_exam_invigilation' AND index_name='idx_invig_classroom') THEN
        ALTER TABLE aem_exam_invigilation ADD INDEX idx_invig_classroom (classroom_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_exam_invigilation' AND index_name='idx_invig_date') THEN
        ALTER TABLE aem_exam_invigilation ADD INDEX idx_invig_date (exam_date); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_exam_invigilation' AND index_name='idx_invig_teacher_date_status') THEN
        ALTER TABLE aem_exam_invigilation ADD INDEX idx_invig_teacher_date_status (teacher_id, exam_date, status); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_exam_invigilation' AND index_name='idx_invig_exam_duty') THEN
        ALTER TABLE aem_exam_invigilation ADD INDEX idx_invig_exam_duty (exam_id, duty_type); END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_exam_seat' AND index_name='idx_seat_student') THEN
        ALTER TABLE aem_exam_seat ADD INDEX idx_seat_student (student_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_exam_seat' AND index_name='idx_seat_classroom') THEN
        ALTER TABLE aem_exam_seat ADD INDEX idx_seat_classroom (classroom_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_exam_seat' AND index_name='idx_seat_exam_classroom') THEN
        ALTER TABLE aem_exam_seat ADD INDEX idx_seat_exam_classroom (exam_id, classroom_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_exam_seat' AND index_name='idx_seat_exam_student') THEN
        ALTER TABLE aem_exam_seat ADD INDEX idx_seat_exam_student (exam_id, student_id); END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_grade_record' AND index_name='idx_grade_semester') THEN
        ALTER TABLE aem_grade_record ADD INDEX idx_grade_semester (semester_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_grade_record' AND index_name='idx_grade_course') THEN
        ALTER TABLE aem_grade_record ADD INDEX idx_grade_course (course_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_grade_record' AND index_name='idx_grade_student') THEN
        ALTER TABLE aem_grade_record ADD INDEX idx_grade_student (student_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_grade_record' AND index_name='idx_grade_sem_course_type') THEN
        ALTER TABLE aem_grade_record ADD INDEX idx_grade_sem_course_type (semester_id, course_id, exam_type); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_grade_record' AND index_name='idx_grade_reviewed') THEN
        ALTER TABLE aem_grade_record ADD INDEX idx_grade_reviewed (is_reviewed, status); END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_grade_review' AND index_name='idx_review_student') THEN
        ALTER TABLE aem_grade_review ADD INDEX idx_review_student (student_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_grade_review' AND index_name='idx_review_status') THEN
        ALTER TABLE aem_grade_review ADD INDEX idx_review_status (approve_status); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_grade_review' AND index_name='idx_review_grade_type') THEN
        ALTER TABLE aem_grade_review ADD INDEX idx_review_grade_type (grade_id, review_type); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_grade_review' AND index_name='idx_review_course') THEN
        ALTER TABLE aem_grade_review ADD INDEX idx_review_course (course_id); END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_grade_statistics' AND index_name='idx_stat_course_sem') THEN
        ALTER TABLE aem_grade_statistics ADD INDEX idx_stat_course_sem (course_id, semester_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_grade_statistics' AND index_name='idx_stat_sem_class') THEN
        ALTER TABLE aem_grade_statistics ADD INDEX idx_stat_sem_class (semester_id, class_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_grade_statistics' AND index_name='idx_stat_course_sem_class') THEN
        ALTER TABLE aem_grade_statistics ADD INDEX idx_stat_course_sem_class (course_id, semester_id, class_id); END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_evaluation_questionnaire' AND index_name='idx_qnaire_semester') THEN
        ALTER TABLE aem_evaluation_questionnaire ADD INDEX idx_qnaire_semester (semester_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_evaluation_questionnaire' AND index_name='idx_qnaire_status') THEN
        ALTER TABLE aem_evaluation_questionnaire ADD INDEX idx_qnaire_status (eval_status, status); END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_evaluation_question' AND index_name='idx_question_qnaire') THEN
        ALTER TABLE aem_evaluation_question ADD INDEX idx_question_qnaire (questionnaire_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_evaluation_question' AND index_name='idx_question_qnaire_sort') THEN
        ALTER TABLE aem_evaluation_question ADD INDEX idx_question_qnaire_sort (questionnaire_id, sort_order); END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_evaluation_result' AND index_name='idx_result_qnaire') THEN
        ALTER TABLE aem_evaluation_result ADD INDEX idx_result_qnaire (questionnaire_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_evaluation_result' AND index_name='idx_result_student') THEN
        ALTER TABLE aem_evaluation_result ADD INDEX idx_result_student (student_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_evaluation_result' AND index_name='idx_result_course') THEN
        ALTER TABLE aem_evaluation_result ADD INDEX idx_result_course (course_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_evaluation_result' AND index_name='idx_result_qnaire_student') THEN
        ALTER TABLE aem_evaluation_result ADD INDEX idx_result_qnaire_student (questionnaire_id, student_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_evaluation_result' AND index_name='idx_result_tcs') THEN
        ALTER TABLE aem_evaluation_result ADD INDEX idx_result_tcs (teacher_id, course_id, status); END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_supervision_record' AND index_name='idx_super_teacher') THEN
        ALTER TABLE aem_supervision_record ADD INDEX idx_super_teacher (teacher_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_supervision_record' AND index_name='idx_super_course') THEN
        ALTER TABLE aem_supervision_record ADD INDEX idx_super_course (course_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_supervision_record' AND index_name='idx_super_date') THEN
        ALTER TABLE aem_supervision_record ADD INDEX idx_super_date (visit_date); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_supervision_record' AND index_name='idx_super_supervisor') THEN
        ALTER TABLE aem_supervision_record ADD INDEX idx_super_supervisor (supervisor); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_supervision_record' AND index_name='idx_super_level_type') THEN
        ALTER TABLE aem_supervision_record ADD INDEX idx_super_level_type (evaluation_level, record_type); END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_gpa_score_mapping' AND index_name='idx_gpa_mapping_config_sort') THEN
        ALTER TABLE aem_gpa_score_mapping ADD INDEX idx_gpa_mapping_config_sort (config_id, sort_order); END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='tpm_course_offering' AND index_name='idx_offering_course_sem') THEN
        ALTER TABLE tpm_course_offering ADD INDEX idx_offering_course_sem (course_id, semester_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='tpm_selection_enrollment' AND index_name='idx_enroll_offering_status') THEN
        ALTER TABLE tpm_selection_enrollment ADD INDEX idx_enroll_offering_status (course_offering_id, result_status); END IF;

    -- 清理重复成绩
    DELETE gr1 FROM aem_grade_record gr1
    INNER JOIN aem_grade_record gr2
      ON gr1.student_id=gr2.student_id AND gr1.course_id=gr2.course_id
     AND gr1.semester_id=gr2.semester_id AND gr1.exam_type=gr2.exam_type
     AND gr1.grade_id > gr2.grade_id;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_grade_record' AND index_name='uk_grade_scset') THEN
        ALTER TABLE aem_grade_record ADD UNIQUE KEY uk_grade_scset (student_id, course_id, semester_id, exam_type); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_exam_seat' AND index_name='uk_seat_exam_student') THEN
        ALTER TABLE aem_exam_seat ADD UNIQUE KEY uk_seat_exam_student (exam_id, student_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_exam_seat' AND index_name='uk_seat_exam_room_no') THEN
        ALTER TABLE aem_exam_seat ADD UNIQUE KEY uk_seat_exam_room_no (exam_id, classroom_id, seat_number); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_exam_invigilation' AND index_name='uk_invig_exam_teacher_duty') THEN
        ALTER TABLE aem_exam_invigilation ADD UNIQUE KEY uk_invig_exam_teacher_duty (exam_id, teacher_id, duty_type); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_evaluation_result' AND index_name='uk_result_qnsct') THEN
        ALTER TABLE aem_evaluation_result ADD UNIQUE KEY uk_result_qnsct (questionnaire_id, student_id, course_id, teacher_id); END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='aem_grade_statistics' AND index_name='uk_stat_csc') THEN
        ALTER TABLE aem_grade_statistics ADD UNIQUE KEY uk_stat_csc (course_id, semester_id, class_id); END IF;
END$$
DELIMITER ;
CALL p_aem_opt();
DROP PROCEDURE p_aem_opt;

-- 评教答题明细表
CREATE TABLE IF NOT EXISTS aem_evaluation_answer (
  answer_id        bigint        NOT NULL AUTO_INCREMENT COMMENT '答题ID',
  result_id        bigint        NOT NULL COMMENT '评教结果ID',
  questionnaire_id bigint        NOT NULL COMMENT '问卷ID',
  question_id      bigint        NOT NULL COMMENT '题目ID',
  student_id       bigint        DEFAULT NULL COMMENT '学生ID',
  course_id        bigint        DEFAULT NULL COMMENT '课程ID',
  teacher_id       bigint        DEFAULT NULL COMMENT '教师ID',
  question_type    char(1)       DEFAULT '0' COMMENT '题型（0单选1多选2评分3文本）',
  score_value      decimal(4,1)  DEFAULT 0.0 COMMENT '本题得分',
  option_value     varchar(500)  DEFAULT NULL COMMENT '选项值',
  text_value       varchar(1000) DEFAULT NULL COMMENT '文本回答',
  create_time      datetime      DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (answer_id),
  KEY idx_answer_result (result_id),
  KEY idx_answer_question (question_id),
  KEY idx_answer_tq (teacher_id, question_id),
  KEY idx_answer_qnaire_question (questionnaire_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评教答题明细表';

-- 成绩统计批次字段
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='aem_grade_statistics' AND column_name='stat_time');
SET @sql := IF(@c=0, 'ALTER TABLE aem_grade_statistics ADD COLUMN stat_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''统计生成时间''', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='aem_grade_statistics' AND column_name='batch_no');
SET @sql := IF(@c=0, 'ALTER TABLE aem_grade_statistics ADD COLUMN batch_no varchar(32) DEFAULT NULL COMMENT ''统计批次号''', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 成绩等级编码统一 A-F -> 中文，与GPA映射对齐（先加宽字段）
ALTER TABLE aem_grade_record MODIFY COLUMN grade_level varchar(20) DEFAULT NULL COMMENT '等级（优秀/良好/中等/及格/不及格）';
UPDATE aem_grade_record SET grade_level='优秀' WHERE grade_level='A';
UPDATE aem_grade_record SET grade_level='良好' WHERE grade_level='B';
UPDATE aem_grade_record SET grade_level='中等' WHERE grade_level='C';
UPDATE aem_grade_record SET grade_level='及格' WHERE grade_level='D';
UPDATE aem_grade_record SET grade_level='不及格' WHERE grade_level='F';
