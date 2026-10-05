-- =====================================================================================
-- 版本：V20261005.1
-- 标题：新增用户多因子鉴别（MFA）绑定表 sys_user_mfa（K3-2 TOTP 二次鉴别后端地基）
-- 目的：为登录引入基于 TOTP（RFC 6238）的第二因子。保存每个用户的 Base32 密钥与启用
--       状态，与 sys_user 主档解耦；仅当 status='1'（已启用）时登录流程强制校验一次性口令。
--       属"按用户 opt-in"：默认无任何人启用，存量登录行为零影响。
-- 做法：CREATE TABLE IF NOT EXISTS（幂等，可重复执行）。
-- 影响表：新增 sys_user_mfa；不改动任何既有表。
-- 算法：TotpUtils（yu-common）纯 JDK 实现，无第三方依赖；绑定接口见 SysMfaController(/mfa/**)。
-- 关联事项：V4.0 §7.3 K3-2（MFA 二次鉴别）
-- 幂等性：是（表已存在则跳过）
-- 回滚脚本：db/rollback/V20261005_1__add_sys_user_mfa.sql
-- =====================================================================================

CREATE TABLE IF NOT EXISTS sys_user_mfa (
  user_id     BIGINT       NOT NULL               COMMENT '用户ID(主键，关联 sys_user.user_id)',
  secret      VARCHAR(64)  NOT NULL               COMMENT 'Base32 编码的 TOTP 密钥',
  status      CHAR(1)      DEFAULT '0'            COMMENT '状态(0待确认 1已启用)',
  bind_time   DATETIME     DEFAULT NULL           COMMENT '启用(确认)时间',
  create_time DATETIME     DEFAULT NULL           COMMENT '创建时间',
  update_time DATETIME     DEFAULT NULL           COMMENT '更新时间',
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户多因子鉴别(MFA)绑定表';

-- 验证
SELECT 'MFA绑定表' AS item, COUNT(1) AS cnt FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'sys_user_mfa';
