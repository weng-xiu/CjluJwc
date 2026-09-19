-- ============================================================
-- Phase 9: S5 学籍异动Flowable审批 + O1 已办修复 + P1 消息待办中心
-- 幂等脚本，可重复执行
-- ============================================================

-- 1. S5: sam_status_change 增加 proc_inst_id 列
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sam_status_change' AND column_name = 'proc_inst_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE sam_status_change ADD COLUMN proc_inst_id VARCHAR(64) DEFAULT NULL COMMENT "流程实例ID"', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. P1: 统一消息表 sys_message
CREATE TABLE IF NOT EXISTS sys_message (
  message_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  receiver_id   BIGINT       NOT NULL COMMENT '接收人用户ID',
  msg_type      CHAR(1)      NOT NULL DEFAULT '3' COMMENT '类型(0预警1审批2变更3通知)',
  title         VARCHAR(200) NOT NULL COMMENT '标题',
  content       VARCHAR(1000) DEFAULT NULL COMMENT '内容',
  business_type VARCHAR(50)  DEFAULT NULL COMMENT '关联业务类型',
  business_id   BIGINT       DEFAULT NULL COMMENT '关联业务ID',
  read_status   CHAR(1)      NOT NULL DEFAULT '0' COMMENT '已读(0未读1已读)',
  read_time     DATETIME     DEFAULT NULL COMMENT '阅读时间',
  create_by     VARCHAR(64)  DEFAULT '' COMMENT '创建者',
  create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
  remark        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (message_id),
  INDEX idx_receiver (receiver_id, read_status),
  INDEX idx_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一消息表';

-- 3. P1: 统一待办表 sys_todo
CREATE TABLE IF NOT EXISTS sys_todo (
  todo_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '待办ID',
  receiver_id   BIGINT       NOT NULL COMMENT '接收人用户ID',
  todo_type     CHAR(1)      NOT NULL DEFAULT '1' COMMENT '类型(0预警1审批2变更3通知)',
  title         VARCHAR(200) NOT NULL COMMENT '标题',
  business_type VARCHAR(50)  DEFAULT NULL COMMENT '关联业务类型',
  business_id   BIGINT       DEFAULT NULL COMMENT '关联业务ID',
  status        CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态(0待办1已办)',
  complete_time DATETIME     DEFAULT NULL COMMENT '完成时间',
  create_by     VARCHAR(64)  DEFAULT '' COMMENT '创建者',
  create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
  remark        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (todo_id),
  INDEX idx_receiver_status (receiver_id, status),
  INDEX idx_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一待办表';

-- 4. 菜单: 消息中心 (父菜单 2760，与已有菜单不冲突)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2760, '消息中心', 0, 9, 'msgcenter', NULL, '', '', 1, 0, 'M', '0', '0', '', 'message', 'admin', NOW(), '', NULL, '统一消息与待办目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2760);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2761, '我的消息', 2760, 1, 'message', 'system/msgcenter/message', '', '', 1, 0, 'C', '0', '0', 'system:msg:list', 'email', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2761);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2762, '我的待办', 2760, 2, 'todo', 'system/msgcenter/todo', '', '', 1, 0, 'C', '0', '0', 'system:todo:list', 'form', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2762);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2763, '消息编辑', 2761, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:msg:edit', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2763);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2764, '待办办理', 2762, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:todo:edit', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2764);

-- 5. 菜单: 学籍异动审批权限 (S5)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2765, '异动审批', 2100, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'sam:statusChange:approve', '#', 'admin', NOW(), '', NULL, 'S5 学籍异动多级审批'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2765);

-- 6. 角色菜单关联 (admin role_id=1)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m WHERE m.menu_id IN (2760,2761,2762,2763,2764,2765)
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);

-- 完成
SELECT 'Phase 9 migration completed successfully' AS result;
