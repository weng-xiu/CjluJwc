-- P4 模板语法修复：Velocity 2.x 静默参考 $!{x}（原误写 ${!x}）、行计数 $foreach.count（原 $velocityCount）
UPDATE sys_print_template
SET content = REPLACE(content, '${!', '$!{')
WHERE content LIKE '%${!%';

UPDATE sys_print_template
SET content = REPLACE(content, '$velocityCount', '$foreach.count')
WHERE content LIKE '%$velocityCount%';

SELECT template_code,
       LOCATE('$!{', content) > 0 AS has_silent_ref,
       LOCATE('${!', content) > 0 AS bad_ref_left,
       LOCATE('$velocityCount', content) > 0 AS bad_count_left
FROM sys_print_template;
