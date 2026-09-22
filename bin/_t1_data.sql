SELECT 'semesters' AS section; SELECT semester_id, semester_name, status FROM tpm_semester LIMIT 10;
SELECT 'schedulable_offerings_by_semester' AS section;
SELECT o.semester_id, COUNT(*) AS schedulable FROM tpm_course_offering o
  WHERE o.del_flag='0' AND o.status='0' AND o.offering_status='1'
    AND NOT EXISTS(SELECT 1 FROM tpm_schedule s WHERE s.offering_id=o.offering_id AND s.del_flag='0')
  GROUP BY o.semester_id;
SELECT 'offerings_total' AS section;
SELECT COUNT(*) AS total_offerings, SUM(offering_status='1') AS confirmed FROM tpm_course_offering WHERE del_flag='0' AND status='0';
SELECT 'classrooms' AS section;
SELECT COUNT(*) AS classrooms, SUM(status='0') AS active FROM brm_classroom;
SELECT 'existing_schedules' AS section;
SELECT COUNT(*) AS schedules FROM tpm_schedule WHERE del_flag='0';
