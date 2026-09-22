SELECT menu_id, menu_name, path, menu_type, visible, status FROM sys_menu WHERE menu_id IN (2105,2620);
SELECT config_key, config_value FROM sys_config WHERE config_key='sys.account.captchaEnabled';
SELECT o.semester_id, COUNT(*) AS schedulable FROM tpm_course_offering o
  WHERE o.del_flag='0' AND o.status='0' AND o.offering_status='1'
    AND NOT EXISTS(SELECT 1 FROM tpm_schedule s WHERE s.offering_id=o.offering_id AND s.del_flag='0')
  GROUP BY o.semester_id;
