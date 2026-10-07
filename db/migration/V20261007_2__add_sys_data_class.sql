-- =====================================================================================
-- 版本：V20261007.2
-- 标题：新增「数据分类分级台账」表 sys_data_class + 分类/分级/保护措施字典 + 实测种子登记
--       （K1 等保数据新规 · "从查系统转向查数据"核心抓手）
-- 目的：对业务表字段逐条登记数据类别、安全等级、保护措施与是否个人信息，形成可审计、可导出的
--       数据资产清单。台账登记口径与运行期保护相互印证：本批种子严格取自代码中已落地的
--       @Sensitive 脱敏字段与 SensitiveFieldCipher 密文列，确保"登记的即已保护的"，不虚报。
-- 做法：CREATE TABLE IF NOT EXISTS + 唯一键 uk(table_name,column_name)；字典按 WHERE NOT EXISTS
--       幂等写入；种子用 INSERT IGNORE（依赖唯一键去重）保证重复执行不产生重复行。
-- 影响表：新增 sys_data_class；sys_dict_type / sys_dict_data 追加三个字典类型及明细。
-- 说明：本脚本仅落数据层与字典；管理端页面（yu-ui-vue3 views/system/dataClass）与 sys_menu 菜单
--       权限点作为独立前端切片后续登记，避免登记指向不存在页面的死菜单。admin(role_id=1) 拥有
--       *:*:* 超级短路，页面就绪前即可通过 /system/dataClass 接口维护与导出台账。
-- 关联事项：V4.x §6.2 K1（数据分类分级台账）；后端见 SysDataClassController(/system/dataClass)
-- 幂等性：是
-- 回滚脚本：db/rollback/V20261007_2__add_sys_data_class.sql
-- 执行说明：本机存量库按既有约定「人工应用 + 带回滚脚本」，FLYWAY_ENABLED 默认 false。
-- =====================================================================================

-- ----------------------------
-- 1、台账主表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_data_class (
  class_id          BIGINT(20)   NOT NULL AUTO_INCREMENT   COMMENT '台账主键',
  table_name        VARCHAR(64)  NOT NULL                  COMMENT '业务表名',
  table_comment     VARCHAR(200) DEFAULT NULL              COMMENT '表中文名',
  column_name       VARCHAR(64)  NOT NULL                  COMMENT '字段名',
  column_comment    VARCHAR(200) DEFAULT NULL              COMMENT '字段中文名',
  data_category     VARCHAR(32)  NOT NULL                  COMMENT '数据类别(字典 sys_data_category)',
  security_level    VARCHAR(8)   NOT NULL                  COMMENT '安全等级(字典 sys_data_security_level: L1~L4)',
  protection_measure VARCHAR(32) DEFAULT 'NONE'            COMMENT '保护措施(字典 sys_data_protection)',
  is_personal_info  CHAR(1)      DEFAULT 'N'               COMMENT '是否个人信息(Y是 N否)',
  status            CHAR(1)      DEFAULT '0'               COMMENT '状态(0正常 1停用登记)',
  remark            VARCHAR(500) DEFAULT NULL              COMMENT '备注',
  create_by         VARCHAR(64)  DEFAULT ''                COMMENT '创建者',
  create_time       DATETIME                               COMMENT '创建时间',
  update_by         VARCHAR(64)  DEFAULT ''                COMMENT '更新者',
  update_time       DATETIME                               COMMENT '更新时间',
  PRIMARY KEY (class_id),
  UNIQUE KEY uk_data_class_table_column (table_name, column_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据分类分级台账';

-- ----------------------------
-- 2、字典类型
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT * FROM (
  SELECT '数据类别' AS a, 'sys_data_category' AS b, '0' AS c, 'system' AS d, NOW() AS e, '数据分类分级台账-数据类别' AS f
  UNION ALL SELECT '数据安全等级','sys_data_security_level','0','system',NOW(),'数据分类分级台账-安全等级(L1~L4)'
  UNION ALL SELECT '数据保护措施','sys_data_protection','0','system',NOW(),'数据分类分级台账-保护措施'
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type x WHERE x.dict_type = t.b);

-- ----------------------------
-- 3、字典明细
-- ----------------------------
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT * FROM (
  SELECT 1 AS a,'个人身份信息' AS b,'PII' AS c,'sys_data_category' AS d,'' AS e,'danger' AS f,'N' AS g,'0' AS h,'system' AS i,NOW() AS j,'可直接识别自然人的身份信息' AS k
  UNION ALL SELECT 2,'联系方式','CONTACT','sys_data_category','','warning','N','0','system',NOW(),'手机/邮箱等可触达个人的信息'
  UNION ALL SELECT 3,'凭证口令','CREDENTIAL','sys_data_category','','danger','N','0','system',NOW(),'口令、密钥等认证凭据'
  UNION ALL SELECT 4,'学业信息','ACADEMIC','sys_data_category','','primary','N','0','system',NOW(),'成绩/学籍/培养方案等学业数据'
  UNION ALL SELECT 5,'财务信息','FINANCIAL','sys_data_category','','danger','N','0','system',NOW(),'缴费、报销等财务数据'
  UNION ALL SELECT 6,'运行信息','OPERATION','sys_data_category','','info','N','0','system',NOW(),'日志、审计等系统运行数据'
  UNION ALL SELECT 9,'其他','OTHER','sys_data_category','','','Y','0','system',NOW(),''
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = 'sys_data_category' AND x.dict_value = 'PII');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT * FROM (
  SELECT 1 AS a,'L1 公开' AS b,'L1' AS c,'sys_data_security_level' AS d,'' AS e,'success' AS f,'N' AS g,'0' AS h,'system' AS i,NOW() AS j,'可对外公开' AS k
  UNION ALL SELECT 2,'L2 内部','L2','sys_data_security_level','','primary','N','0','system',NOW(),'仅限内部使用'
  UNION ALL SELECT 3,'L3 敏感','L3','sys_data_security_level','','warning','N','0','system',NOW(),'个人敏感信息，需脱敏展示'
  UNION ALL SELECT 4,'L4 核心敏感','L4','sys_data_security_level','','danger','N','0','system',NOW(),'身份证号等，需加密存储+严格访问控制'
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = 'sys_data_security_level' AND x.dict_value = 'L1');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT * FROM (
  SELECT 1 AS a,'无' AS b,'NONE' AS c,'sys_data_protection' AS d,'' AS e,'info' AS f,'Y' AS g,'0' AS h,'system' AS i,NOW() AS j,'未采取专项保护措施' AS k
  UNION ALL SELECT 2,'脱敏','MASK','sys_data_protection','','primary','N','0','system',NOW(),'展示期动态脱敏(@Sensitive)'
  UNION ALL SELECT 3,'加密','ENCRYPT','sys_data_protection','','success','N','0','system',NOW(),'存储期静态加密(SensitiveFieldCipher)'
  UNION ALL SELECT 4,'访问控制','ACCESS','sys_data_protection','','warning','N','0','system',NOW(),'数据权限/角色限定访问'
  UNION ALL SELECT 5,'脱敏且加密','MASK_ENCRYPT','sys_data_protection','','danger','N','0','system',NOW(),'存储加密+展示脱敏双重保护'
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = 'sys_data_protection' AND x.dict_value = 'NONE');

-- ----------------------------
-- 4、种子登记：严格取自代码中已落地的敏感字段保护（@Sensitive / 密文列），登记口径=保护口径
-- ----------------------------
INSERT IGNORE INTO sys_data_class
  (table_name, table_comment, column_name, column_comment, data_category, security_level, protection_measure, is_personal_info, status, remark, create_by, create_time)
VALUES
  ('sam_student','学生学籍','id_card','身份证号(明文列)','PII','L4','MASK_ENCRYPT','Y','0','明文列保留期，展示脱敏(ID_CARD)、静态加密灰度迁移至 id_card_cipher','system',NOW()),
  ('sam_student','学生学籍','id_card_cipher','身份证号密文','PII','L4','ENCRYPT','Y','0','AES-GCM/SM4 密文列，密钥经环境变量注入','system',NOW()),
  ('sam_student','学生学籍','student_name','学生姓名','PII','L3','MASK','Y','0','展示脱敏(USERNAME)','system',NOW()),
  ('brm_teacher','教师信息','teacher_name','教师姓名','PII','L3','MASK','Y','0','展示脱敏(USERNAME)','system',NOW()),
  ('brm_teacher','教师信息','phone','联系电话','CONTACT','L3','MASK','Y','0','展示脱敏(PHONE)','system',NOW()),
  ('brm_teacher','教师信息','email','邮箱','CONTACT','L3','MASK','Y','0','展示脱敏(EMAIL)','system',NOW()),
  ('sys_user','用户','phonenumber','手机号码','CONTACT','L3','MASK','Y','0','展示脱敏(PHONE)','system',NOW()),
  ('sys_user','用户','email','邮箱','CONTACT','L3','MASK','Y','0','展示脱敏(EMAIL)','system',NOW());

-- ----------------------------
-- 5、验证
-- ----------------------------
SELECT '台账表' AS item, COUNT(1) AS cnt FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_data_class'
UNION ALL SELECT '台账字典类型', COUNT(1) FROM sys_dict_type WHERE dict_type IN ('sys_data_category','sys_data_security_level','sys_data_protection')
UNION ALL SELECT '种子登记行', COUNT(1) FROM sys_data_class;
