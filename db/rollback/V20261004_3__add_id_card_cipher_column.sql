-- =====================================================================================
-- 回滚：V20261004.3 —— 移除「学生学籍身份证号」密文列 id_card_cipher
-- 对应变更：db/migration/V20261004_3__add_id_card_cipher_column.sql
-- 前置动作：回滚前请将配置 data.encrypt.enabled 置为 false（关闭灰度），避免双读/回填语句命中缺失列。
-- 逆向操作：information_schema + PREPARE 幂等 DROP COLUMN（列不存在则跳过）
-- 影响表：sam_student（删除 id_card_cipher 列；明文列 id_card 始终完整，删除密文列不丢数据）
-- 回滚后果：回到本变更前的明文存储状态；已回填的密文随列删除而丢弃（明文仍在，无损失）
-- 幂等性：是
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261004.3';
-- =====================================================================================

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sam_student' AND column_name = 'id_card_cipher');
SET @sql = IF(@col_exists = 1,
    'ALTER TABLE `sam_student` DROP COLUMN `id_card_cipher`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 校验：应返回 0 行
SELECT COUNT(*) AS remaining FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'sam_student' AND column_name = 'id_card_cipher';
