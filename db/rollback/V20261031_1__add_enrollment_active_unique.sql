-- =====================================================================================
-- 回滚：V20261031.1 —— 删除选课报名表的「活跃行唯一」约束
-- 对应变更：db/migration/V20261031_1__add_enrollment_active_unique.sql
-- 反向操作：DROP INDEX uk_enroll_active → DROP COLUMN active_slot（均按 information_schema 判存在）
-- 影响表：tpm_selection_enrollment（只删索引与生成列，报名行一条不动）
-- 前置条件：**必须先回滚应用版本**——新代码在 insert 处依赖该唯一键把并发重复选课转成
--           业务提示；先删索引会让并发重复选课重新变成「两条在册行 + 名额占两次」。
-- 说明：生成列 active_slot 是纯派生物（不存业务信息），删掉不丢数据。
-- 幂等性：是
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261031.1';
-- =====================================================================================

-- ---------------------------------------------------------------------------
-- 1) 先删唯一索引（列被索引引用时无法单独删除）
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'tpm_selection_enrollment'
      AND index_name = 'uk_enroll_active');

SET @sql = IF(@idx_exists > 0,
    'ALTER TABLE `tpm_selection_enrollment` DROP INDEX `uk_enroll_active`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2) 再删生成列
-- ---------------------------------------------------------------------------
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'tpm_selection_enrollment'
      AND column_name = 'active_slot');

SET @sql = IF(@col_exists = 1,
    'ALTER TABLE `tpm_selection_enrollment` DROP COLUMN `active_slot`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 3) 校验：索引与列都不在了
-- ---------------------------------------------------------------------------
SELECT
    (SELECT COUNT(*) FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'tpm_selection_enrollment'
        AND index_name = 'uk_enroll_active') AS uk_enroll_active_cols,
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'tpm_selection_enrollment'
        AND column_name = 'active_slot') AS has_active_slot;
