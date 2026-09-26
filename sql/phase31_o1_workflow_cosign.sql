-- =============================================================
-- Phase 31：O1 通用工作流增强收尾（加签 / 会签 / 委托 + 协同留痕）
-- 覆盖：
--   1) 新增 oa_countersign_batch / oa_countersign_item 两张协同批次与明细表，
--      承载前加签、后加签、会签、委托四类动作的发起、表决与门禁状态；
--   2) 补齐字典 oa_cosign_mode / oa_cosign_vote / oa_cosign_status；
--   3) 新增按钮权限「待办任务协同」oa:task:coSign（parent=2704 待办任务），
--      授权超级管理员与教务处管理员。
-- 说明：流程图渲染复用既有 /oa/workflow/instance/detail 与新增 /oa/workflow/diagram
--      查询接口，权限沿用 oa:instance:query / oa:task:list，无需新增查询菜单。
-- 菜单ID：现有最大为 3091，本脚本使用 3100 段避免冲突。
-- 本脚本幂等，可重复执行。
-- =============================================================

-- ----------------------------
-- 1. 协同批次表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `oa_countersign_batch` (
  `batch_id`        bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `task_id`         varchar(64)  NOT NULL                COMMENT 'Flowable任务ID',
  `proc_inst_id`    varchar(64)  DEFAULT NULL            COMMENT 'Flowable流程实例ID',
  `process_name`    varchar(100) DEFAULT NULL            COMMENT '流程名称',
  `node_id`         varchar(64)  DEFAULT NULL            COMMENT '节点ID',
  `node_name`       varchar(100) DEFAULT NULL            COMMENT '节点名称',
  `sign_mode`       char(1)      NOT NULL DEFAULT '0'    COMMENT '动作类型（0前加签 1后加签 2会签 3委托）',
  `sign_rule`       varchar(10)  DEFAULT 'ALL'           COMMENT '表决规则（ALL全部同意 ANY一人同意即定论）',
  `initiator`       varchar(50)  NOT NULL                COMMENT '发起人登录名',
  `total_count`     int(4)       DEFAULT 0               COMMENT '应处理人数',
  `done_count`      int(4)       DEFAULT 0               COMMENT '已处理人数',
  `agree_count`     int(4)       DEFAULT 0               COMMENT '同意人数',
  `disagree_count`  int(4)       DEFAULT 0               COMMENT '不同意人数',
  `status`          char(1)      DEFAULT '0'             COMMENT '批次状态（0进行中 1意见已齐 2已取消）',
  `release_time`    datetime     DEFAULT NULL            COMMENT '意见齐备（门禁释放）时间',
  `create_by`       varchar(50)  DEFAULT ''              COMMENT '创建者',
  `create_time`     datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`       varchar(50)  DEFAULT ''              COMMENT '更新者',
  `update_time`     datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`          varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`batch_id`),
  KEY `idx_cs_batch_task` (`task_id`),
  KEY `idx_cs_batch_proc` (`proc_inst_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='工作流加签/会签/委托批次表';

-- ----------------------------
-- 2. 协同明细表（一人一条意见）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `oa_countersign_item` (
  `item_id`         bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `batch_id`        bigint(20)   NOT NULL                COMMENT '批次ID',
  `task_id`         varchar(64)  DEFAULT NULL            COMMENT 'Flowable任务ID',
  `proc_inst_id`    varchar(64)  DEFAULT NULL            COMMENT 'Flowable流程实例ID',
  `handler`         varchar(50)  NOT NULL                COMMENT '协同办理人登录名',
  `handler_name`    varchar(50)  DEFAULT NULL            COMMENT '协同办理人昵称',
  `vote`            char(1)      DEFAULT '0'             COMMENT '表决结果（0未表态 1同意 2不同意）',
  `opinion`         varchar(500) DEFAULT NULL            COMMENT '意见内容',
  `status`          char(1)      DEFAULT '0'             COMMENT '明细状态（0待处理 1已处理 2已取消）',
  `handle_time`     datetime     DEFAULT NULL            COMMENT '处理时间',
  `create_by`       varchar(50)  DEFAULT ''              COMMENT '创建者',
  `create_time`     datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`       varchar(50)  DEFAULT ''              COMMENT '更新者',
  `update_time`     datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`          varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`item_id`),
  KEY `idx_cs_item_batch` (`batch_id`),
  KEY `idx_cs_item_task` (`task_id`),
  KEY `idx_cs_item_handler` (`handler`, `status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='工作流加签/会签/委托明细表';

-- ----------------------------
-- 3. 字典：动作类型 / 表决结果 / 批次状态
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '工作流协同类型', 'oa_cosign_mode', '0', 'admin', sysdate(), '加签会签委托动作类型'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='oa_cosign_mode');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '工作流协同表决', 'oa_cosign_vote', '0', 'admin', sysdate(), '加签会签表决结果'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='oa_cosign_vote');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '工作流协同状态', 'oa_cosign_status', '0', 'admin', sysdate(), '加签会签委托批次与明细状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='oa_cosign_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '前加签', '0', 'oa_cosign_mode', '', 'warning', 'Y', '0', 'admin', sysdate(), '意见未齐时阻塞节点提交'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='oa_cosign_mode' AND dict_value='0');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '后加签', '1', 'oa_cosign_mode', '', 'info', 'N', '0', 'admin', sysdate(), '不阻塞本人提交，意见事后归档'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='oa_cosign_mode' AND dict_value='1');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '会签', '2', 'oa_cosign_mode', '', 'primary', 'N', '0', 'admin', sysdate(), '多人并行表决，意见齐备前不可提交'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='oa_cosign_mode' AND dict_value='2');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '委托', '3', 'oa_cosign_mode', '', 'success', 'N', '0', 'admin', sysdate(), '他人代办，办结后知会原办理人'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='oa_cosign_mode' AND dict_value='3');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '未表态', '0', 'oa_cosign_vote', '', 'info', 'Y', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='oa_cosign_vote' AND dict_value='0');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '同意', '1', 'oa_cosign_vote', '', 'success', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='oa_cosign_vote' AND dict_value='1');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '不同意', '2', 'oa_cosign_vote', '', 'danger', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='oa_cosign_vote' AND dict_value='2');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '进行中', '0', 'oa_cosign_status', '', 'warning', 'Y', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='oa_cosign_status' AND dict_value='0');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '已齐备', '1', 'oa_cosign_status', '', 'success', 'N', '0', 'admin', sysdate(), '意见已齐或随节点办结归档'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='oa_cosign_status' AND dict_value='1');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已取消', '2', 'oa_cosign_status', '', 'info', 'N', '0', 'admin', sysdate(), '流程终止或委托收回'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='oa_cosign_status' AND dict_value='2');

-- ----------------------------
-- 4. 按钮权限：待办任务协同（加签/会签/委托），parent=2704 待办任务
-- ----------------------------
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3100,'待办任务协同',2704,3,'','','','',1,0,'F','0','0','oa:task:coSign','#','admin',sysdate(),'',NULL,'O1 加签/会签/委托发起权限'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3100);

-- ----------------------------
-- 5. 角色授权：超级管理员(role_id=1)、教务处管理员(role_id=3)
--    协同动作发生在待办任务页，需一并确保两角色拥有该页面与查询/审批权限
-- ----------------------------
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT r.role_id, m.menu_id FROM
  (SELECT 1 AS role_id UNION ALL SELECT 3) r
  CROSS JOIN (SELECT 3100 AS menu_id UNION ALL SELECT 2704 UNION ALL SELECT 2721 UNION ALL SELECT 2722) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=r.role_id AND rm.`menu_id`=m.menu_id);

-- ----------------------------
-- 验证
-- ----------------------------
SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME IN ('oa_countersign_batch','oa_countersign_item') ORDER BY TABLE_NAME;
SELECT dict_type, dict_value, dict_label FROM sys_dict_data
  WHERE dict_type IN ('oa_cosign_mode','oa_cosign_vote','oa_cosign_status') ORDER BY dict_type, dict_sort;
SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id = 3100;
