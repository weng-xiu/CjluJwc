-- =====================================================================================
-- 回滚：V20261031.2 —— 撤销 sam_student 学号重编号并删除 student_no 唯一索引
-- 对应变更：db/migration/V20261031_2__clean_and_unique_student_no.sql
-- 反向操作：DROP INDEX uk_sam_student_no → 清掉 remark 里的重编号说明 → 去掉 '-D<student_id>' 后缀
-- 影响表：sam_student（恢复变更前的撞号状态）
-- 前置条件：**必须先回滚应用版本**——新代码在学籍新增处依赖该唯一键拦重复学号（并转成
--           业务提示）；先删索引会让重复学号重新静默入库。
-- 说明：
--   1) 唯一索引必须先删，否则恢复出来的重复学号会被唯一键拒绝。
--   2) 恢复口径：变更时统一加了 '-D<student_id>' 后缀，这里按同一规则剥离。若历史上本来
--      就有学号以 '-D<student_id>' 结尾（人工录入的极少见写法），会被误剥；执行前先跑
--      下方第 0 段体检，确认待恢复行数与变更日志中记录的 176 一致再继续。
--   3) 变更时对 remark 追加的是「V20261031.2 学号撞号重编号 <旧> -> <新>」，这里按同一标记清除。
-- 幂等性：是（索引按存在性跳过；后缀/标记不存在时 UPDATE 影响 0 行）
-- 执行方式：mysql -h127.0.0.1 -P3306 -uroot -p yu-cjlujwc < db/rollback/V20261031_2__clean_and_unique_student_no.sql
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261031.2';
-- =====================================================================================

-- ---------------------------------------------------------------------------
-- 0) 体检：待恢复行数（应与变更影响行数 176 一致）
-- ---------------------------------------------------------------------------
SELECT COUNT(*) AS rows_to_restore
FROM `sam_student`
WHERE `student_no` LIKE CONCAT('%-D', `student_id`);

-- ---------------------------------------------------------------------------
-- 1) 先删唯一索引
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sam_student'
      AND index_name = 'uk_sam_student_no');

SET @sql = IF(@idx_exists > 0,
    'ALTER TABLE `sam_student` DROP INDEX `uk_sam_student_no`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2) 清掉 remark 中的重编号说明（必须在改学号之前做：改号后标记里的「旧学号」就对不上了）
-- ---------------------------------------------------------------------------
UPDATE `sam_student`
SET `remark` = NULLIF(TRIM(BOTH ' ;' FROM REGEXP_REPLACE(
        `remark`, '(; )?V20261031\\.2 学号撞号重编号 [^;]*', '')), '')
WHERE `remark` LIKE '%V20261031.2 学号撞号重编号%';

-- ---------------------------------------------------------------------------
-- 3) 剥离 '-D<student_id>' 后缀，恢复原学号
-- ---------------------------------------------------------------------------
UPDATE `sam_student`
SET `student_no` = SUBSTRING(`student_no`, 1,
        CHAR_LENGTH(`student_no`) - CHAR_LENGTH(CONCAT('-D', `student_id`))),
    `update_time` = NOW()
WHERE `student_no` = CONCAT(
        SUBSTRING(`student_no`, 1,
            CHAR_LENGTH(`student_no`) - CHAR_LENGTH(CONCAT('-D', `student_id`))),
        '-D', `student_id`);

-- ---------------------------------------------------------------------------
-- 4) 校验：索引已删；撞号组数应回到变更前的 115
-- ---------------------------------------------------------------------------
SELECT
    (SELECT COUNT(*) FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'sam_student'
        AND index_name = 'uk_sam_student_no') AS uk_sam_student_no_cols,
    (SELECT COUNT(*) FROM (
        SELECT 1 FROM `sam_student` GROUP BY `student_no` HAVING COUNT(*) > 1) d) AS duplicate_student_no_groups,
    (SELECT COUNT(*) FROM `sam_student`) AS total_students;
