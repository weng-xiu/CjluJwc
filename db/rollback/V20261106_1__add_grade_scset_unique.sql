-- =====================================================================================
-- 回滚：V20261106.1 —— 删除成绩表的「同一门课唯一」约束
-- 对应变更：db/migration/V20261106_1__add_grade_scset_unique.sql
-- 反向操作：DROP INDEX uk_grade_scset（按 information_schema 判存在，幂等）
-- 影响表：aem_grade_record（只删索引，成绩行一条不动）
-- 前置条件：**必须先回滚应用版本**——新代码依赖该唯一键把「重复导入」转成覆盖更新；
--           先删索引会让同一 (学生, 课程, 学期, 考试类型) 在每次重复导入时新增一行，
--           GPA 与学分按行累加，成绩直接算错。
-- 说明：本索引是纯约束，不承载数据，删掉不丢任何成绩；但删掉后 batchInsert 的
--       `on duplicate key update` 子句会退化为纯 insert（MySQL 不报错，静默重复插入），
--       所以回滚动作必须与「把批量导入换回逐条 update」的应用版本同时生效。
-- 幂等性：是
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261106.1';
-- =====================================================================================

-- ---------------------------------------------------------------------------
-- 1) 删除唯一索引
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'aem_grade_record'
      AND index_name = 'uk_grade_scset');

SET @sql = IF(@idx_exists > 0,
    'ALTER TABLE `aem_grade_record` DROP INDEX `uk_grade_scset`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2) 校验：索引已不在，且原表仍保留三列普通索引（回滚不得误伤 idx_grade_*）
-- ---------------------------------------------------------------------------
SELECT
    (SELECT COUNT(*) FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'aem_grade_record'
        AND index_name = 'uk_grade_scset') AS uk_grade_scset_cols,
    (SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'aem_grade_record'
        AND index_name LIKE 'idx\_grade\_%') AS kept_plain_indexes,
    (SELECT COUNT(*) FROM `aem_grade_record`) AS total_grade_rows;
