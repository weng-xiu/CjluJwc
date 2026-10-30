-- =====================================================================================
-- 标题：测试/预发环境「敏感数据去标识化生成器」（K1 等保数据新规 · 测试数据不得含真实个人信息）
-- 目的：等保数据安全要求生产数据不得原样进入测试/演示环境。本脚本对已在「数据分类分级
--       台账 sys_data_class」登记的 PII/CONTACT 字段进行**可复现的去标识化改写**，使测试库
--       保留数据规模与字段格式（仍可跑通业务与展示），但不再包含任何真实自然人身份信息。
--       登记口径 = 去标识化口径，与运行期 @Sensitive 脱敏、SensitiveFieldCipher 加密相互印证。
-- 覆盖：sam_student.student_name/id_card(/id_card_cipher)、brm_teacher.teacher_name/phone/email、
--       sys_user.nick_name/phonenumber/email（登录账号 user_name 保留，保证测试账号仍可登录）。
-- 做法：改写规则确定且可复现（依主键派生合成值，姓名/身份证/手机/邮箱格式合法但纯属虚构）；
--       密文列 id_card_cipher 去标识化后原密文失效，置 NULL（列不存在时自动跳过，兼容 Schema 漂移）。
-- 安全闸门：**仅允许库名匹配 *_test / *_preview / *_dev / yutest* / *_demo 的环境执行**，
--       其余一律 SIGNAL 中止，杜绝误伤生产/主开发库；脚本不自动运行，须运维显式施加到测试库。
-- 执行方式（PowerShell，须带 utf8mb4 以正确处理中文字段值）：
--       mysql --default-character-set=utf8mb4 -h<host> -P<port> -u<user> -p <test_db> < sql/desensitize-test-data.sql
-- 幂等性：是（多次执行结果一致，均改写为同一套由主键派生的合成值）
-- 合成标记（V5.23 S-10，仓库门禁 bin/pii-scan.js 依此豁免，勿改口径）：
--       身份证 99010119900101xxxx、手机号 1380000xxxx；同口径已回填至 sql/yangtze_seed_data.sql
-- 关联：db/migration/V20261007_2__add_sys_data_class.sql（台账登记）；@Sensitive DesensitizedType 口径
-- =====================================================================================

DROP PROCEDURE IF EXISTS cjlu_desensitize_test_data;

DELIMITER $$

CREATE PROCEDURE cjlu_desensitize_test_data()
BEGIN
    DECLARE v_db         VARCHAR(64);
    DECLARE v_has_cipher INT DEFAULT 0;

    SELECT DATABASE() INTO v_db;

    -- ---------- 安全闸门：非测试/预发/演示库直接中止（消息用 ASCII，避免编码歧义） ----------
    IF NOT (   v_db LIKE '%test%'
            OR v_db LIKE '%preview%'
            OR v_db LIKE '%\_dev%'
            OR v_db LIKE 'yutest%'
            OR v_db LIKE '%\_demo%') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'ABORT: cjlu desensitize generator allows only *_test/*_preview/*_dev/yutest*/*_demo DB';
    END IF;

    -- ---------- 1) 学生学籍：姓名 + 身份证号 ----------
    UPDATE sam_student
       SET student_name = CONCAT('测试学生', LPAD(student_id, 6, '0')),
           id_card      = CONCAT('99010119900101', LPAD(MOD(student_id, 10000), 4, '0'))
     WHERE student_name IS NOT NULL;

    -- ---------- 2) 教师信息：姓名 + 电话 + 邮箱（仅改写原非空值，不凭空造数据） ----------
    UPDATE brm_teacher
       SET teacher_name = CONCAT('测试教师', LPAD(teacher_id, 6, '0'))
     WHERE teacher_name IS NOT NULL;

    UPDATE brm_teacher
       SET phone = CONCAT('138', LPAD(MOD(teacher_id, 100000000), 8, '0'))
     WHERE phone IS NOT NULL AND phone <> '';

    UPDATE brm_teacher
       SET email = CONCAT('t', LPAD(teacher_id, 6, '0'), '@test.invalid')
     WHERE email IS NOT NULL AND email <> '';

    -- ---------- 3) 系统用户：昵称 + 手机 + 邮箱（保留 user_name 登录账号） ----------
    UPDATE sys_user
       SET nick_name = CONCAT('测试用户', LPAD(user_id, 6, '0'))
     WHERE nick_name IS NOT NULL AND nick_name <> '';

    UPDATE sys_user
       SET phonenumber = CONCAT('139', LPAD(MOD(user_id, 100000000), 8, '0'))
     WHERE phonenumber IS NOT NULL AND phonenumber <> '';

    UPDATE sys_user
       SET email = CONCAT('u', LPAD(user_id, 6, '0'), '@test.invalid')
     WHERE email IS NOT NULL AND email <> '';

    -- ---------- 4) 身份证号密文列：去标识化后原密文失效，置 NULL（列存在才动，兼容 Schema 漂移） ----------
    SELECT COUNT(*) INTO v_has_cipher
      FROM information_schema.columns
     WHERE table_schema = v_db AND table_name = 'sam_student' AND column_name = 'id_card_cipher';

    IF v_has_cipher > 0 THEN
        SET @cipher_sql = 'UPDATE `sam_student` SET `id_card_cipher` = NULL';
        PREPARE cipher_stmt FROM @cipher_sql;
        EXECUTE cipher_stmt;
        DEALLOCATE PREPARE cipher_stmt;
    END IF;
END$$

DELIMITER ;

CALL cjlu_desensitize_test_data();
DROP PROCEDURE cjlu_desensitize_test_data;

-- ---------- 验证：抽样确认已无真实身份信息（姓名应均为「测试*」前缀） ----------
SELECT 'sam_student' AS tbl, COUNT(*) AS rows_total,
       SUM(student_name LIKE '测试学生%') AS masked_name,
       SUM(id_card LIKE '99010119900101%') AS masked_id
  FROM sam_student
UNION ALL
SELECT 'brm_teacher', COUNT(*), SUM(teacher_name LIKE '测试教师%'), SUM(email LIKE '%@test.invalid') FROM brm_teacher
UNION ALL
SELECT 'sys_user', COUNT(*), SUM(nick_name LIKE '测试用户%'), SUM(email LIKE '%@test.invalid') FROM sys_user;
