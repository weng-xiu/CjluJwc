-- ============================================================
-- Phase 38: yutest 沙箱库 Schema 漂移补齐（幂等脚本，可重复执行）
-- 背景：2026-10-04 E2E 核验发现代码引用的表/列在沙箱库缺失，导致
--       驾驶舱 overview、消息中心、门户文章/通知列表等接口 code:500。
-- 内容：1) 补建 sys_message / sys_todo（原 DDL 见 phase9_s5_o1_p1.sql）
--       2) 补建 portal_column / portal_article（原 DDL 见 portal_cms.sql；
--          PortalArticleMapper LEFT JOIN portal_column，故一并补齐依赖表）
--       3) 补建 portal_notice（原 DDL 见 portal.sql）
--       4) tpm_course_library / tpm_training_plan 幂等补 del_flag 列
--          （原补列存储过程方案见 phase6_portal_mobile.sql，本脚本改用
--           information_schema + PREPARE 免存储过程，与 fix-drift.sql 一致）
-- 执行：mysql --default-character-set=utf8mb4 -e "source .../phase38_schema_drift_fix.sql"
-- ============================================================

-- ---------- 1. 统一消息表 sys_message ----------
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

-- ---------- 2. 统一待办表 sys_todo ----------
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

-- ---------- 3. 门户栏目表 portal_column（portal_article 的 JOIN 依赖表） ----------
CREATE TABLE IF NOT EXISTS `portal_column` (
  `column_id` bigint NOT NULL AUTO_INCREMENT COMMENT '栏目ID',
  `column_name` varchar(100) NOT NULL COMMENT '栏目名称',
  `column_code` varchar(50) NOT NULL COMMENT '栏目编码',
  `parent_id` bigint DEFAULT 0 COMMENT '父栏目ID',
  `column_type` char(1) DEFAULT '1' COMMENT '栏目类型(1列表 2单页 3链接)',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `sort_order` int DEFAULT 0 COMMENT '显示排序',
  `is_visible` char(1) DEFAULT '1' COMMENT '是否显示(0否 1是)',
  `external_url` varchar(500) DEFAULT NULL COMMENT '外部链接(type=3时使用)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`column_id`),
  UNIQUE KEY `uk_column_code` (`column_code`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户栏目表';

-- ---------- 4. 门户文章表 portal_article ----------
CREATE TABLE IF NOT EXISTS `portal_article` (
  `article_id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `column_id` bigint NOT NULL COMMENT '所属栏目ID',
  `title` varchar(200) NOT NULL COMMENT '文章标题',
  `summary` varchar(500) DEFAULT NULL COMMENT '摘要',
  `content` longtext COMMENT '文章内容(富文本)',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面图URL',
  `source` varchar(100) DEFAULT NULL COMMENT '来源',
  `author` varchar(64) DEFAULT NULL COMMENT '作者',
  `publish_status` char(1) DEFAULT '0' COMMENT '发布状态(0草稿 1待审核 2已发布 3已撤回)',
  `publish_date` datetime DEFAULT NULL COMMENT '发布时间',
  `is_top` char(1) DEFAULT '0' COMMENT '是否置顶(0否 1是)',
  `is_featured` char(1) DEFAULT '0' COMMENT '是否推荐到首页(0否 1是)',
  `view_count` int DEFAULT 0 COMMENT '浏览次数',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人ID',
  `review_comment` varchar(500) DEFAULT NULL COMMENT '审核意见',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`article_id`),
  INDEX `idx_column_status` (`column_id`, `publish_status`),
  INDEX `idx_publish_date` (`publish_date` DESC),
  INDEX `idx_featured` (`is_featured`, `publish_date` DESC)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户文章表';

-- ---------- 5. 教务通知表 portal_notice ----------
CREATE TABLE IF NOT EXISTS `portal_notice` (
  `notice_id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `notice_title` varchar(200) NOT NULL COMMENT '通知标题',
  `notice_type` char(1) DEFAULT '1' COMMENT '通知类型（1选课通知 2考试通知 3学籍通知 4综合通知）',
  `notice_content` text COMMENT '通知内容',
  `publish_dept_id` bigint DEFAULT NULL COMMENT '发布部门ID（关联brm_department）',
  `publish_dept_name` varchar(100) DEFAULT NULL COMMENT '发布部门名称',
  `publish_status` char(1) DEFAULT '0' COMMENT '发布状态（0草稿 1已发布 2已撤回）',
  `publish_date` date DEFAULT NULL COMMENT '发布日期',
  `target_role` char(1) DEFAULT '0' COMMENT '目标角色（0所有人 1学生 2教师）',
  `is_top` char(1) DEFAULT '0' COMMENT '是否置顶（0否 1是）',
  `view_count` int DEFAULT '0' COMMENT '浏览次数',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教务通知表';

-- ---------- 6. tpm_course_library / tpm_training_plan 幂等补 del_flag ----------
-- （TpmCourseLibraryMapper / TpmTrainingPlanMapper 列表查询 WHERE del_flag='0'）
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tpm_course_library' AND COLUMN_NAME='del_flag')=0,
  'ALTER TABLE tpm_course_library ADD COLUMN del_flag CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志（0代表存在 2代表删除）''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tpm_training_plan' AND COLUMN_NAME='del_flag')=0,
  'ALTER TABLE tpm_training_plan ADD COLUMN del_flag CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志（0代表存在 2代表删除）''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ---------- 7. 完成校验：应返回 5 表 + 2 列 ----------
SELECT (SELECT COUNT(*) FROM information_schema.TABLES
         WHERE TABLE_SCHEMA=DATABASE()
           AND TABLE_NAME IN ('sys_message','sys_todo','portal_column','portal_article','portal_notice')) AS tables_ok,
       (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA=DATABASE()
           AND CONCAT(TABLE_NAME,'.',COLUMN_NAME) IN ('tpm_course_library.del_flag','tpm_training_plan.del_flag')) AS columns_ok;
