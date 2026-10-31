-- =====================================================================================
-- 变更：V20261031.1 —— tpm_selection_enrollment 增加「活跃行唯一」约束（P-2）
-- 背景：同一学生并发提交两次选课（双击 / 前端重试 / 网关重发）时，两条请求都能通过
--       Java 侧的冲突检测与门数上限（都是「先查后改」），占用名额由 occupySeat 的
--       条件更新串行化了，落报名记录却没有——于是同一 (round_id, student_id,
--       course_offering_id) 会出现两条在册行，名额被占两次。顺序场景下会被时间冲突
--       误报成「与已选课程【同一门课】时间冲突：第3-4节 与 第3-4节 冲突」，学生看不出
--       真实原因；并发场景下完全无保护。
-- 做法：MySQL 没有部分索引（PG 的 WHERE 索引），用 STORED 生成列把「活跃行」投影成常量 1，
--       再用 (round_id, student_id, course_offering_id, active_slot) 建唯一索引。
--       唯一索引允许多个 NULL，因此：
--         · 在册(1) / 落选候补(2) → active_slot = 1  → 同键只能有一条；
--         · 退课(3) / 逻辑删除(del_flag=2) → active_slot = NULL → 不参与唯一性，
--           「退课后重新选同一门课」照旧可以插入新行（旧行保留作审计轨迹）。
--       不用简单的 3 列唯一键：那会把「退课后再选」这条正常业务直接挡住。
-- 影响表：tpm_selection_enrollment（仅新增生成列与唯一索引，不改存量数据）
-- 幂等性：是（列/索引均按 information_schema 判存在，存在则跳过）
-- 回滚脚本：db/rollback/V20261031_1__add_enrollment_active_unique.sql
-- 执行方式：mysql -h127.0.0.1 -P3306 -uroot -p yu-cjlujwc < db/migration/V20261031_1__add_enrollment_active_unique.sql
-- 注意事项：
--   1) 脚本开头先跑体检 SELECT。若它返回任何行，说明存量已存在活跃重复（本仓备份库实测
--      13957 行 / 0 组，预期为空），必须先人工合并重复行再执行第 2 步，否则 ADD UNIQUE 会
--      报 Duplicate entry 并中断（中断时不会留下半成品：加列与加索引是两条独立 DDL）。
--   2) ADD COLUMN ... STORED 会重建表（ALGORITHM=INPLACE，允许并发 DML），万级表耗时在
--      秒级；执行期间会持有元数据锁，避开选课高峰。
-- =====================================================================================

-- ---------------------------------------------------------------------------
-- 0) 体检：列出活跃重复组（必须为空）
-- ---------------------------------------------------------------------------
SELECT round_id, student_id, course_offering_id, COUNT(*) AS active_rows
FROM `tpm_selection_enrollment`
WHERE `del_flag` = '0' AND `result_status` IN ('1', '2')
GROUP BY round_id, student_id, course_offering_id
HAVING COUNT(*) > 1;

-- ---------------------------------------------------------------------------
-- 1) 新增生成列 active_slot（幂等）
-- ---------------------------------------------------------------------------
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'tpm_selection_enrollment'
      AND column_name = 'active_slot');

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `tpm_selection_enrollment`
       ADD COLUMN `active_slot` tinyint
         GENERATED ALWAYS AS (CASE WHEN `del_flag` = ''0'' AND `result_status` IN (''1'', ''2'') THEN 1 ELSE NULL END) STORED
         COMMENT ''活跃行标记（1=在册/候补，退课或删除为 NULL）；仅用于唯一约束，业务不读写''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2) 新增唯一索引 uk_enroll_active（幂等）
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'tpm_selection_enrollment'
      AND index_name = 'uk_enroll_active');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `tpm_selection_enrollment`
       ADD UNIQUE KEY `uk_enroll_active` (`round_id`, `student_id`, `course_offering_id`, `active_slot`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 3) 校验：列与索引都存在，且当前无活跃重复
-- ---------------------------------------------------------------------------
SELECT
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'tpm_selection_enrollment'
        AND column_name = 'active_slot') AS has_active_slot,
    (SELECT COUNT(*) FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'tpm_selection_enrollment'
        AND index_name = 'uk_enroll_active') AS uk_enroll_active_cols,
    (SELECT COUNT(*) FROM (
        SELECT 1 FROM `tpm_selection_enrollment`
        WHERE `del_flag` = '0' AND `result_status` IN ('1', '2')
        GROUP BY round_id, student_id, course_offering_id
        HAVING COUNT(*) > 1) d) AS active_duplicate_groups;
