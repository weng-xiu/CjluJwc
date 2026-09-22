-- T1 时间片自动排课 功能测试种子：新增“已确认(offering_status=1)但未排课”的开课
-- 覆盖：大容量课(需120教室9003)、含实践课、同一教师多课(演示教师冲突规避)
INSERT INTO tpm_course_offering
  (offering_id, semester_id, course_id, teacher_id, campus_id, class_count, max_students, offering_status, status, del_flag, create_by, create_time, remark)
VALUES
  (9331, 1, 9307, 1, NULL, 2, 120, '1', '0', '0', 'admin', sysdate(), 'T1测试-大容量公共课'),
  (9332, 1, 9309, 1, NULL, 1,  60, '1', '0', '0', 'admin', sysdate(), 'T1测试-含实践课'),
  (9333, 1, 9310, 1, NULL, 1,  60, '1', '0', '0', 'admin', sysdate(), 'T1测试-同教师另一课'),
  (9334, 1, 9312, 1, NULL, 1,  80, '1', '0', '0', 'admin', sysdate(), 'T1测试-中容量课');

SELECT o.offering_id, cl.course_name, o.max_students, cl.total_hours,
       (SELECT COUNT(*) FROM tpm_schedule s WHERE s.offering_id=o.offering_id AND s.del_flag='0') AS has_sched
FROM tpm_course_offering o LEFT JOIN tpm_course_library cl ON o.course_id=cl.course_id
WHERE o.offering_id BETWEEN 9331 AND 9334;
