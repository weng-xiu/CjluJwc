-- =====================================================================================
-- 变更：V20261106.1 —— aem_grade_record 补齐 uk_grade_scset，让 Excel 重复导入真正幂等
-- 背景：AemGradeRecordMapper.batchInsert 用 `on duplicate key update` 实现「重复导入即覆盖」，
--       并注释依赖唯一键 uk_grade_scset。但该键此前只出现在 sql/phase5_aem_optimization.sql
--       的存储过程里（条件 ADD UNIQUE），既没进 db/ci/01-schema.sql 基线，也没随 Flyway 下发。
--       后果：
--         · CI 空库没有该键 → 集成测试里 upsert 退化成纯 insert，与生产行为不一致；
--         · 存量环境若未重放 phase5 → 每次重复导入都新增一行成绩，同一
--           (学生, 课程, 学期, 考试类型) 出现多行，GPA/学分按行累加，直接算错。
-- 做法：0) 体检：列出重复组（只读，必须为空）
--       1) 幂等新增 UNIQUE KEY uk_grade_scset (student_id, course_id, semester_id, exam_type)
--       2) 校验：索引在位、无重复组
-- 为什么不复用 phase5 的做法：phase5 在建键前用 `DELETE gr1 ... grade_id > gr2.grade_id`
--       硬删重复行——删掉的是后导入的那条（很可能是修正后的正确成绩），留下旧行。
--       成绩属重要数据，删行必须人工确认口径，所以本脚本只体检、不清理。
-- 影响表：aem_grade_record（仅新增唯一索引，一行数据都不动）
-- 幂等性：是（索引按 information_schema 判存在；体检与校验均为只读）
-- 回滚脚本：db/rollback/V20261106_1__add_grade_scset_unique.sql
-- 执行方式：mysql -h127.0.0.1 -P3306 -uroot -p yu-cjlujwc < db/migration/V20261106_1__add_grade_scset_unique.sql
-- 注意事项：
--   1) exam_type 允许 NULL，而 MySQL 唯一索引不约束 NULL——历史 NULL 行之间仍可重复。
--      第 0 步体检用 IFNULL(exam_type,'0') 归并分组，能识别「NULL 与 '0' 本属同一条」的情况；
--      若返回此类组，请先把 NULL 规范化为 '0' 再执行第 1 步（否则新键管不到这些行）。
--   2) course_id 允许 NULL（DEFAULT NULL），同理不被唯一性覆盖。importGrade 强制课程非空，
--      故经导入入口写入的数据不受影响。
--   3) 加键后 batchInsert 的「覆盖」语义受 submit_status 保护：'1' 已提交待审 / '2' 已锁定的
--      成绩保持原值不被覆盖（见同批次 AemGradeRecordMapper.xml 的 if(...) 谓词）。
--      唯一键本身只保证「同键唯一」，不区分状态，两道防线缺一不可。
-- =====================================================================================

-- ---------------------------------------------------------------------------
-- 0) 体检：列出重复组（正常应为 0 行）
--    按 IFNULL(exam_type,'0') 归并：唯一索引把 NULL 视为互不相等，而业务上 NULL 即 '0'。
-- ---------------------------------------------------------------------------
SELECT `student_id`,
       `course_id`,
       `semester_id`,
       IFNULL(`exam_type`, '0') AS `exam_type`,
       COUNT(*) AS `dup_rows`,
       GROUP_CONCAT(`grade_id` ORDER BY `grade_id`) AS `grade_ids`,
       GROUP_CONCAT(IFNULL(`submit_status`, '-') ORDER BY `grade_id`) AS `submit_statuses`
FROM `aem_grade_record`
GROUP BY `student_id`, `course_id`, `semester_id`, IFNULL(`exam_type`, '0')
HAVING COUNT(*) > 1;

-- ---------------------------------------------------------------------------
-- 1) 新增唯一索引 uk_grade_scset（幂等）
--    列序与 batchInsert 的 on duplicate key update 依赖一致，勿调整。
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'aem_grade_record'
      AND index_name = 'uk_grade_scset');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `aem_grade_record`
       ADD UNIQUE KEY `uk_grade_scset` (`student_id`, `course_id`, `semester_id`, `exam_type`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2) 校验：索引 4 列就位，且无重复组
-- ---------------------------------------------------------------------------
SELECT
    (SELECT COUNT(*) FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'aem_grade_record'
        AND index_name = 'uk_grade_scset') AS uk_grade_scset_cols,
    (SELECT COUNT(*) FROM (
        SELECT 1 FROM `aem_grade_record`
        GROUP BY `student_id`, `course_id`, `semester_id`, IFNULL(`exam_type`, '0')
        HAVING COUNT(*) > 1) d) AS duplicate_grade_groups,
    (SELECT COUNT(*) FROM `aem_grade_record`) AS total_grade_rows;
