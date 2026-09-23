SELECT dict_type,dict_value,dict_label FROM sys_dict_data WHERE dict_label IN ('本科','专科','硕士研究生','博士研究生','高中') ORDER BY dict_type,dict_code;
SELECT education_level, COUNT(*) FROM tpm_training_plan GROUP BY education_level;
SELECT education_level, COUNT(*) FROM sam_student GROUP BY education_level LIMIT 5;
