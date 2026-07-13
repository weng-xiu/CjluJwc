-- ===============================================
-- 门户CMS内容管理模块 (PORTAL_CMS) 数据库初始化脚本
-- 包含：栏目管理(portal_column)、文章管理(portal_article)、轮播管理(portal_banner)
-- 字符集：UTF8MB4
-- 创建时间：2026-07-13
-- ===============================================

-- ----------------------------
-- 1. 门户栏目表
-- ----------------------------
DROP TABLE IF EXISTS `portal_column`;
CREATE TABLE `portal_column` (
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

-- ----------------------------
-- 2. 门户文章表
-- ----------------------------
DROP TABLE IF EXISTS `portal_article`;
CREATE TABLE `portal_article` (
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

-- ----------------------------
-- 3. 门户轮播表
-- ----------------------------
DROP TABLE IF EXISTS `portal_banner`;
CREATE TABLE `portal_banner` (
  `banner_id` bigint NOT NULL AUTO_INCREMENT COMMENT '轮播ID',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `image_url` varchar(500) NOT NULL COMMENT '图片URL',
  `link_url` varchar(500) DEFAULT NULL COMMENT '点击跳转链接',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `is_active` char(1) DEFAULT '1' COMMENT '是否激活(0否 1是)',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`banner_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户轮播表';

-- ----------------------------
-- 初始化栏目数据
-- 一级栏目（1-8）对应长江大学官网导航
-- ----------------------------
INSERT INTO `portal_column` (`column_id`, `column_name`, `column_code`, `parent_id`, `column_type`, `icon`, `sort_order`, `is_visible`, `external_url`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(1, '新闻资讯', 'news', 0, '1', 'el-icon-news', 1, '1', NULL, 'admin', NOW(), '', NULL, '新闻资讯栏目'),
(2, '学术动态', 'academic', 0, '1', 'el-icon-reading', 2, '1', NULL, 'admin', NOW(), '', NULL, '学术动态栏目'),
(3, '通知公告', 'notice', 0, '1', 'el-icon-bell', 3, '1', NULL, 'admin', NOW(), '', NULL, '通知公告栏目'),
(4, '校园看点', 'campus', 0, '1', 'el-icon-picture-outline', 4, '1', NULL, 'admin', NOW(), '', NULL, '校园看点栏目'),
(5, '媒体长大', 'media', 0, '1', 'el-icon-video-camera', 5, '1', NULL, 'admin', NOW(), '', NULL, '媒体长大栏目'),
(6, '长大人', 'people', 0, '1', 'el-icon-user', 6, '1', NULL, 'admin', NOW(), '', NULL, '长大人栏目'),
(7, '专题专栏', 'topic', 0, '1', 'el-icon-folder-opened', 7, '1', NULL, 'admin', NOW(), '', NULL, '专题专栏栏目'),
(8, '快速通道', 'links', 0, '3', 'el-icon-link', 8, '1', NULL, 'admin', NOW(), '', NULL, '快速通道-外部链接');

-- ----------------------------
-- 二级栏目（9-15）单页类型
-- ----------------------------
INSERT INTO `portal_column` (`column_id`, `column_name`, `column_code`, `parent_id`, `column_type`, `icon`, `sort_order`, `is_visible`, `external_url`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(9,  '学校概况', 'about', 0, '2', NULL, 9,  '1', NULL, 'admin', NOW(), '', NULL, '学校概况单页'),
(10, '组织机构', 'organization', 0, '2', NULL, 10, '1', NULL, 'admin', NOW(), '', NULL, '组织机构单页'),
(11, '师资队伍', 'faculty', 0, '2', NULL, 11, '1', NULL, 'admin', NOW(), '', NULL, '师资队伍单页'),
(12, '学科建设', 'disciplines', 0, '2', NULL, 12, '1', NULL, 'admin', NOW(), '', NULL, '学科建设单页'),
(13, '教育教学', 'education', 0, '2', NULL, 13, '1', NULL, 'admin', NOW(), '', NULL, '教育教学单页'),
(14, '科学研究', 'research', 0, '2', NULL, 14, '1', NULL, 'admin', NOW(), '', NULL, '科学研究单页'),
(15, '招生就业', 'recruitment', 0, '2', NULL, 15, '1', NULL, 'admin', NOW(), '', NULL, '招生就业单页');

-- ----------------------------
-- 初始化示例文章数据（8条）
-- ----------------------------
INSERT INTO `portal_article` (`article_id`, `column_id`, `title`, `summary`, `content`, `cover_url`, `source`, `author`, `publish_status`, `publish_date`, `is_top`, `is_featured`, `view_count`, `reviewer_id`, `review_comment`, `review_time`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(1, 1, '我校召开2026年春季学期教学工作会议',
 '3月15日，学校在行政楼报告厅召开春季学期教学工作会议，部署新学期教学重点工作。',
 '<p>3月15日上午，学校在行政楼报告厅召开2026年春季学期教学工作会议。校领导、各学院院长、教学副院长及教务处相关人员参加了会议。</p><p>会议围绕新学期教学重点工作进行了部署，强调要持续推进教育教学改革，提升人才培养质量。</p>',
 '/upload/portal/article/2026/03/teaching-meeting.jpg', '长江大学新闻网', '宣传部', '2', '2026-03-15 10:00:00', '1', '1', 356, 1, '审核通过', '2026-03-14 16:00:00', 'admin', NOW(), '', NULL, NULL),

(2, 1, '长江大学与中科院签署战略合作协议',
 '我校与中国科学院签署战略合作协议，双方将在人才培养、科研合作等方面展开深度合作。',
 '<p>近日，长江大学与中国科学院在北京签署战略合作协议。</p><p>根据协议，双方将共建联合实验室，在石油工程、农业科学等优势学科领域开展深度科研合作，并联合培养高层次创新人才。</p>',
 '/upload/portal/article/2026/03/cas-agreement.jpg', '长江大学新闻网', '宣传部', '2', '2026-03-12 14:30:00', '0', '1', 528, 1, '审核通过', '2026-03-11 09:00:00', 'admin', NOW(), '', NULL, NULL),

(3, 2, '我校三项课题获国家自然科学基金重点项目立项',
 '2026年度国家自然科学基金重点项目评审结果公布，我校三项课题获立项资助。',
 '<p>近日，2026年度国家自然科学基金重点项目评审结果揭晓，我校石油工程学院、农学院、化学与环境工程学院各有一项课题获重点项目立项资助，直接经费合计超过800万元。</p>',
 '/upload/portal/article/2026/04/nsfc-projects.jpg', '科研处', '科研处', '2', '2026-04-20 09:00:00', '1', '1', 412, 1, '审核通过', '2026-04-19 17:00:00', 'admin', NOW(), '', NULL, NULL),

(4, 3, '关于2026年春季学期期末考试安排的通知',
 '各教学单位：2026年春季学期期末考试将于第18-19周进行，请做好相关准备工作。',
 '<p>各教学单位：</p><p>2026年春季学期期末考试将于第18-19周（6月23日-7月4日）进行。请各学院按照教务处统一安排，做好考试组织工作。</p><p>请各位同学认真复习备考，遵守考试纪律。</p>',
 NULL, '教务处', '教务处', '2', '2026-06-10 08:00:00', '1', '0', 892, 1, '审核通过', '2026-06-09 15:00:00', 'admin', NOW(), '', NULL, NULL),

(5, 3, '关于开展2026届毕业生学位授予工作的通知',
 '根据学位授予工作安排，现启动2026届毕业生学位授予审核工作。',
 '<p>各学院：</p><p>根据学校学位授予工作安排，现启动2026届毕业生学位授予审核工作。请各学院于6月30日前完成初审并将名单报送学位办。</p><p>联系人：张老师，电话：8060xxxx</p>',
 NULL, '学位办', '学位办', '2', '2026-06-05 10:00:00', '0', '0', 567, 1, '审核通过', '2026-06-04 14:00:00', 'admin', NOW(), '', NULL, NULL),

(6, 4, '春日长大：校园樱花季摄影展',
 '三月的长大校园，樱花盛开，春意盎然。让我们跟随镜头，感受校园春色。',
 '<p>三月的长江大学校园，樱花如雪，玉兰飘香。东西校区的樱花大道迎来了最美的季节。</p><p>本次摄影展收录了师生投稿的50余幅作品，展现了春日校园的独特魅力。</p>',
 '/upload/portal/article/2026/03/cherry-blossom.jpg', '校园记者团', '李明', '2', '2026-03-25 12:00:00', '0', '1', 723, 1, '审核通过', '2026-03-24 16:00:00', 'admin', NOW(), '', NULL, NULL),

(7, 5, '【湖北日报】长江大学：扎根荆楚大地 服务区域发展',
 '湖北日报整版报道我校服务区域经济社会发展的典型做法和成效。',
 '<p>近日，湖北日报以整版篇幅报道了长江大学扎根荆楚大地、服务区域经济社会发展的典型做法和显著成效。</p><p>报道从人才培养、科技创新、社会服务等多个维度，全面展示了学校近年来取得的成绩。</p>',
 '/upload/portal/article/2026/04/hubei-daily.jpg', '湖北日报', '转载', '2', '2026-04-08 09:30:00', '0', '0', 345, 1, '审核通过', '2026-04-07 17:00:00', 'admin', NOW(), '', NULL, NULL),

(8, 6, '【长大人】记国家奖学金获得者张同学：在科研路上笃定前行',
 '张同学，石油工程学院2022级博士生，获2025年度国家奖学金。',
 '<p>张同学，石油工程学院2022级博士研究生，师从王教授。在读期间发表SCI论文5篇，获2025年度研究生国家奖学金。</p><p>"科研是一条漫长的路，但每一步都算数。"这是张同学常挂在嘴边的话。</p>',
 '/upload/portal/article/2026/05/scholar-zhang.jpg', '学生工作处', '编辑部', '1', NULL, '0', '0', 0, NULL, NULL, NULL, 'admin', NOW(), '', NULL, '待审核文章');

-- ----------------------------
-- 初始化轮播数据（3条）
-- ----------------------------
INSERT INTO `portal_banner` (`banner_id`, `title`, `image_url`, `link_url`, `sort_order`, `is_active`, `start_time`, `end_time`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, '2026年招生季 | 欢迎报考长江大学', '/upload/portal/banner/2026/admission.jpg', '/portal/article/2', 1, '1', '2026-05-01 00:00:00', '2026-09-30 23:59:59', 'admin', NOW(), '', NULL),
(2, '长江大学与中科院签署战略合作协议', '/upload/portal/banner/2026/cas-cooperation.jpg', '/portal/article/2', 2, '1', '2026-03-12 00:00:00', '2026-08-31 23:59:59', 'admin', NOW(), '', NULL),
(3, '春日校园樱花季', '/upload/portal/banner/2026/spring-campus.jpg', '/portal/article/6', 3, '1', '2026-03-20 00:00:00', '2026-04-30 23:59:59', 'admin', NOW(), '', NULL);
