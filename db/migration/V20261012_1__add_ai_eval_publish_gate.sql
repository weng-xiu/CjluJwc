-- =====================================================================================
-- 版本：V20261012.1
-- 标题：智能体发布前评测集与发布门禁（K4 AI 治理 · ③ 评测集存档 + 发布前打分留痕）
-- 目的：K4 主体（内容审核 AiContentModerator、模型版本/治理留痕、语料脱敏 AiCorpusDesensitizer）已落地，
--       本批补齐唯一残项③——「智能体发布前评测集存档」。当前 AI 为单一 RAG 教务政策问答助手（无多智能体/
--       发布态），故经产品口径确认收敛为「最小·单助手评测存档」：以 golden 评测用例集在发布/更新前回归跑分，
--       通过率低于阈值即判定拦截发布，批次与逐条得分入库留痕，满足《人工智能+教育行动计划》能力评估可回溯要求。
-- 做法：CREATE TABLE IF NOT EXISTS 建 3 表；sys_config/字典/菜单按 WHERE NOT EXISTS 幂等写入；示例用例仅在表为空时播种。
-- 影响表：新增 sys_ai_eval_case / sys_ai_eval_run / sys_ai_eval_result；追加 sys_config / sys_dict_* / sys_menu / sys_role_menu。
-- 兼容：不改写既有问答与知识库；门禁仅在管理员显式调用「发布门禁」时生效（ai.eval.publish.enabled=false 可灰度关闭）。
-- 关联事项：V4.x §7 K4③；后端 SysAiEvalController / SysAiEvalServiceImpl；前端 yu-ui-vue3 views/system/aiEval/index
-- 幂等性：是（建表 IF NOT EXISTS；配置/字典/菜单/示例均 NOT EXISTS 判重）
-- 回滚脚本：db/rollback/V20261012_1__add_ai_eval_publish_gate.sql
-- 执行说明：本机存量库按既有约定「人工应用 + 带回滚脚本」，FLYWAY_ENABLED 默认 false。
-- =====================================================================================

-- ----------------------------
-- 1、评测用例（golden set）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_ai_eval_case` (
  `case_id`              bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '用例ID',
  `question`             varchar(500) NOT NULL                COMMENT '评测提问',
  `expect_knowledge_ids` varchar(250) DEFAULT NULL            COMMENT '期望命中知识ID（逗号分隔，可空）',
  `expect_keywords`      varchar(500) DEFAULT NULL            COMMENT '期望得分关键词（回答须全部包含，可空）',
  `category`             varchar(64)  DEFAULT NULL            COMMENT '业务分类',
  `sort`                 int(4)       DEFAULT 0               COMMENT '排序',
  `enabled`              char(1)      DEFAULT '0'             COMMENT '是否启用（0启用 1停用）',
  `remark`               varchar(500) DEFAULT NULL            COMMENT '备注',
  `create_by`            varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time`          datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`            varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time`          datetime     DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (`case_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体评测用例集（K4 发布前评测）';

-- ----------------------------
-- 2、评测批次（发布前打分留痕主表）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_ai_eval_run` (
  `run_id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `trigger_type`    varchar(20)  DEFAULT 'MANUAL'        COMMENT '触发方式（MANUAL手动 PUBLISH发布门禁）',
  `model_version`   varchar(100) DEFAULT NULL            COMMENT '本次所依据模型版本（空=本地抽取）',
  `total_cases`     int(11)      DEFAULT 0               COMMENT '参与评测用例总数',
  `passed_cases`    int(11)      DEFAULT 0               COMMENT '通过用例数',
  `pass_rate`       double(5,2)  DEFAULT 0               COMMENT '通过率（百分比）',
  `avg_confidence`  double(5,4)  DEFAULT 0               COMMENT '平均检索置信度',
  `pass_threshold`  double(5,2)  DEFAULT 80              COMMENT '本次生效通过率阈值（百分比）',
  `result`          char(1)      DEFAULT '1'             COMMENT '门禁结果（0通过 1拦截）',
  `operator`        varchar(64)  DEFAULT ''              COMMENT '操作人',
  `remark`          varchar(500) DEFAULT NULL            COMMENT '备注',
  `create_by`       varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time`     datetime     DEFAULT NULL            COMMENT '创建时间',
  PRIMARY KEY (`run_id`),
  KEY `idx_eval_run_result` (`result`),
  KEY `idx_eval_run_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体发布前评测批次留痕（K4）';

-- ----------------------------
-- 3、评测逐条结果（明细）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_ai_eval_result` (
  `result_id`         bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '结果ID',
  `run_id`            bigint(20)   NOT NULL                COMMENT '所属批次ID',
  `case_id`           bigint(20)   DEFAULT NULL            COMMENT '用例ID',
  `question`          varchar(500) DEFAULT NULL            COMMENT '评测提问',
  `answer`            text                                 COMMENT '实际回答',
  `answer_source`     varchar(20)  DEFAULT NULL            COMMENT '回答来源（LLM/EXTRACT/NONE/ERROR/MODERATED）',
  `confidence`        double(5,4)  DEFAULT 0               COMMENT '检索置信度',
  `moderation_result` char(1)      DEFAULT '0'             COMMENT '治理结果码（0通过 1输入拦截 2输出改写）',
  `matched`           char(1)      DEFAULT NULL            COMMENT '是否命中期望（0命中 1未命中）',
  `passed`            char(1)      DEFAULT NULL            COMMENT '该用例是否通过（0通过 1未过）',
  `create_time`       datetime     DEFAULT NULL            COMMENT '创建时间',
  PRIMARY KEY (`result_id`),
  KEY `idx_eval_result_run` (`run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体评测逐条结果（K4）';

-- ----------------------------
-- 4、系统参数（发布门禁开关与阈值；缺省：门禁开、通过率阈值 80%、置信度下限 0.12）
-- ----------------------------
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT * FROM (
  SELECT 'AI发布门禁开关' AS a, 'ai.eval.publish.enabled' AS b, 'true' AS c, 'Y' AS d, 'system' AS e, NOW() AS f, 'true/false，发布/更新前是否强制以评测集回归并拦截低分发布；置 false 仅评测留痕不拦截（灰度期用）' AS g
  UNION ALL SELECT 'AI评测通过率阈值','ai.eval.pass.threshold','80','Y','system',NOW(),'发布前评测通过率(百分比 0~100)低于此值即拦截发布，缺省 80'
  UNION ALL SELECT 'AI评测置信度下限','ai.eval.min.confidence','0.12','Y','system',NOW(),'用例未设期望命中/关键词时，检索置信度达到此下限方判为通过'
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_config c2 WHERE c2.config_key = t.b);

-- ----------------------------
-- 5、字典：评测批次门禁结果（供管理端结果列 tag 展示）
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT 'AI评测门禁结果', 'sys_ai_eval_result', '0', 'system', NOW(), '智能体发布前评测批次门禁结果码'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'sys_ai_eval_result');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT * FROM (
  SELECT 1 AS a, '通过' AS b, '0' AS c, 'sys_ai_eval_result' AS d, '' AS e, 'success' AS f, 'N' AS g, '0' AS h, 'system' AS i, NOW() AS j, '通过率达到阈值，允许发布' AS k
  UNION ALL SELECT 2,'拦截','1','sys_ai_eval_result','','danger','Y','0','system',NOW(),'通过率低于阈值或无启用用例，拦截发布'
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sys_ai_eval_result' AND dict_value = '0');

-- ----------------------------
-- 6、菜单与权限（挂系统管理 parent=1，紧随数据分类分级(order=15) 之后取 order_num=16；menu_id 3410~3417 空闲段）
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3410, 'AI评测与发布门禁', 1, 16, 'aiEval', 'system/aiEval/index', '', '', 1, 0, 'C', '0', '0', 'system:aiEval:list', 'validCode', 'admin', NOW(), '', NULL, 'K4 AI治理·发布前评测集存档与门禁'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3410 OR perms = 'system:aiEval:list');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3411, '评测查询', 3410, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:aiEval:query', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3411 OR perms = 'system:aiEval:query');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3412, '用例新增', 3410, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:aiEval:add', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3412 OR perms = 'system:aiEval:add');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3413, '用例修改', 3410, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:aiEval:edit', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3413 OR perms = 'system:aiEval:edit');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3414, '用例删除', 3410, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:aiEval:remove', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3414 OR perms = 'system:aiEval:remove');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3415, '评测导出', 3410, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:aiEval:export', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3415 OR perms = 'system:aiEval:export');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3416, '运行评测', 3410, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'system:aiEval:run', '#', 'admin', NOW(), '', NULL, '以全部启用用例手动跑分留痕'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3416 OR perms = 'system:aiEval:run');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3417, '发布门禁', 3410, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'system:aiEval:publish', '#', 'admin', NOW(), '', NULL, '发布前评测并按阈值放行/拦截'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3417 OR perms = 'system:aiEval:publish');

-- 角色授权：默认仅授予 admin(role_id=1) 全套（1 C + 7 F）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM (
  SELECT 3410 AS menu_id UNION ALL SELECT 3411 UNION ALL SELECT 3412 UNION ALL SELECT 3413
  UNION ALL SELECT 3414 UNION ALL SELECT 3415 UNION ALL SELECT 3416 UNION ALL SELECT 3417
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = 1 AND x.menu_id = m.menu_id);

-- ----------------------------
-- 7、示例评测用例（仅表为空时播种，供管理员据本校知识库调整关键词/期望命中；remark 标注为示例）
-- ----------------------------
INSERT INTO sys_ai_eval_case (question, expect_keywords, category, sort, enabled, remark, create_by, create_time)
SELECT * FROM (
  SELECT '期末考试补考申请流程是怎样的？' AS a, '补考' AS b, '考试' AS c, 1 AS d, '0' AS e, '示例用例，请依据知识库实际口径调整关键词' AS f, 'system' AS g, NOW() AS h
  UNION ALL SELECT '学生学籍异动（转专业）需要哪些材料？','转专业','学籍',2,'0','示例用例，请依据知识库实际口径调整关键词','system',NOW()
  UNION ALL SELECT '成绩不及格如何申请重修？','重修','成绩',3,'0','示例用例，请依据知识库实际口径调整关键词','system',NOW()
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_ai_eval_case);

-- ----------------------------
-- 8、验证
-- ----------------------------
SELECT '评测表' AS item, COUNT(1) AS cnt FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name IN ('sys_ai_eval_case','sys_ai_eval_run','sys_ai_eval_result')
UNION ALL SELECT '评测参数', COUNT(1) FROM sys_config WHERE config_key LIKE 'ai.eval.%'
UNION ALL SELECT '门禁结果字典', COUNT(1) FROM sys_dict_data WHERE dict_type = 'sys_ai_eval_result'
UNION ALL SELECT '评测菜单', COUNT(1) FROM sys_menu WHERE menu_id BETWEEN 3410 AND 3417
UNION ALL SELECT '示例用例', COUNT(1) FROM sys_ai_eval_case;
