-- =====================================================================================
-- 版本：V20261004.3
-- 标题：为「学生学籍身份证号」新增密文列 id_card_cipher（K1 合规②列加密灰度迁移 · 非破坏）
-- 目的：对存量明文身份证列实施静态加密。采用灰度路线：新增独立密文列，保留原明文列 id_card 不动，
--       由 SamStudentCipherMigrationServiceImpl 在开关 data.encrypt.enabled=true 时批量回填密文、
--       双读优先取密文。任何阶段可回滚（明文列始终完整），不改变既有查询/导出的行为。
-- 做法：information_schema + PREPARE 幂等 ADD COLUMN（MySQL 8 不支持 ADD COLUMN IF NOT EXISTS）。
--       列可空，默认 NULL，供后续增量回填；不对存量数据做任何改写。
-- 影响表：sam_student（新增 1 列 id_card_cipher；存量行该列为 NULL，明文列不受影响）
-- 密钥：加密密钥经环境变量 data.encrypt.key 注入，禁止入库、禁止硬编码；密钥缺失时迁移服务拒绝回填。
-- 安全：应用本 DDL 前，后端即便部署也安全——引用 id_card_cipher 的 mapper 语句受 enabled 门控，
--       默认 false 时永不下发；故「先应用 DDL、后开灰度」与「先开灰度、后应用 DDL」均不报错。
-- 关联事项：V4.1 §6.2 K1②（敏感字段静态加密）；配套 V20261004.1 表、V20261004.2 审计菜单
-- 幂等性：是（重复执行时列已存在则跳过）
-- 回滚脚本：db/rollback/V20261004_3__add_id_card_cipher_column.sql
-- 执行说明：本机存量库按既有约定「人工应用 + 带回滚脚本」，FLYWAY_ENABLED 默认 false。
-- =====================================================================================

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sam_student' AND column_name = 'id_card_cipher');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `sam_student` ADD COLUMN `id_card_cipher` varchar(255) DEFAULT NULL COMMENT ''身份证号密文（AES-GCM，灰度迁移用，明文列保留于 id_card）'' AFTER `id_card`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 校验：应返回 1 行
SELECT column_name, column_type, is_nullable, column_comment
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'sam_student' AND column_name = 'id_card_cipher';
