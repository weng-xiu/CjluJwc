-- ===============================================
-- 师生互动服务门户模块 (PORTAL) 数据库初始化脚本
-- 包含：教务通知（portal_notice）
-- ===============================================

-- ----------------------------
-- 1. 教务通知表
-- ----------------------------
DROP TABLE IF EXISTS `portal_notice`;
CREATE TABLE `portal_notice` (
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

-- ----------------------------
-- 初始化示例数据
-- ----------------------------
INSERT INTO `portal_notice` VALUES (1, '2026年春季学期选课通知', '1', '各位同学：2026年春季学期选课将于近日开放，请及时登录选课系统进行选课操作。选课时间：2026年5月25日-5月30日。', NULL, '教务处', '1', '2026-05-20', '1', '1', 128, '0', 'admin', NOW(), '', NULL, NULL);
INSERT INTO `portal_notice` VALUES (2, '期末考试安排通知', '2', '2026年春季学期期末考试将于第18-19周进行，具体安排请查看考试安排页面。请各位同学做好复习准备。', NULL, '教务处', '1', '2026-05-18', '0', '1', 256, '0', 'admin', NOW(), '', NULL, NULL);
INSERT INTO `portal_notice` VALUES (3, '关于开展2026年度学籍核查工作的通知', '3', '根据学籍管理规定，现开展2026年度学籍信息核查工作。请所有在校生登录系统核对个人学籍信息。', NULL, '学籍管理科', '1', '2026-05-15', '1', '0', 89, '0', 'admin', NOW(), '', NULL, NULL);
