SET NAMES utf8mb4;
-- ============================================================
-- 门户内容管理（PORTAL_CMS）测试数据集 fixture
-- 覆盖三表：portal_column / portal_article / portal_banner
-- 用途：为门户内容管理的功能测试/演示/E2E 造数补齐 portal_cms.sql 基础种子未覆盖的状态与边界，
--       使以下真实查询分支可被端到端验证（见每条注释标注的分支）：
--         · 发布状态机四态 0草稿/1待审核/2已发布/3已撤回（公开页仅放行 status='2'）
--         · 首页聚合 home() 各栏目 subList 截断（news>5 只取前 5）
--         · selectFeaturedArticles 首页推荐位（status='2' 且 is_featured='1'）
--         · selectPrevArticle / selectNextArticle 同栏目上一篇/下一篇（按 article_id 排序链）
--         · incrementViewCount 浏览次数（含 0 与超大值边界）
--         · cover_url 为空 → 前端“暂无图片”回退路径
--         · 审批发布完整留痕（reviewer_id / review_comment / review_time）
--         · selectActiveBanners 三重过滤（is_active='1' 且 start_time<=NOW 且 end_time>=NOW，含 null 时段）
--         · 栏目树 parent_id 层级 + is_visible='0' 隐藏栏目不进首页 columns
-- 特性：固定 9000+ 专用 ID 段，先删后插，可重复执行；不污染 portal_cms.sql 的 1-15 栏目 / 1-8 文章 / 1-3 轮播
-- 图片：复用 seed_portal_covers.sql 中已实测可无 Referer 直接 200 加载的 Unsplash 直链（许可允许商用免署名）
-- 执行：mysql -h 127.0.0.1 -uroot -p -D yu-cjlujwc --default-character-set=utf8mb4 -e "source d:/project/CjluJwc/CjluJwc/sql/seed_portal_cms_test_data.sql"
--       （PowerShell 下务必带 --default-character-set=utf8mb4，否则中文乱码）
-- ============================================================

-- ---------- 清理旧种子（幂等重跑：仅删本 fixture 的 9000+ 段） ----------
DELETE FROM portal_article WHERE article_id BETWEEN 9000 AND 9999;
DELETE FROM portal_banner  WHERE banner_id  BETWEEN 9000 AND 9999;
DELETE FROM portal_column  WHERE column_id  BETWEEN 9000 AND 9999;

-- ---------- 栏目：子栏目（树层级）+ 隐藏栏目（is_visible='0' 被首页 columns 过滤） ----------
INSERT INTO `portal_column` (`column_id`, `column_name`, `column_code`, `parent_id`, `column_type`, `icon`, `sort_order`, `is_visible`, `external_url`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(9001, '部门动态', 'dept-news', 1, '1', 'el-icon-document', 20, '1', NULL, 'seed', NOW(), '', NULL, '新闻资讯下的二级栏目-测试树层级'),
(9002, '内部参考', 'internal',  0, '1', NULL,               90, '0', NULL, 'seed', NOW(), '', NULL, '隐藏栏目-不应出现在首页可见栏目列表');

-- ---------- 文章：补齐四态 + 边界（column_id 复用基础种子 1-7 列表栏目） ----------
-- 列顺序：article_id, column_id, title, summary, content, cover_url, source, author,
--         publish_status, publish_date, is_top, is_featured, view_count,
--         reviewer_id, review_comment, review_time, create_by, create_time, update_by, update_time, remark
INSERT INTO `portal_article` (`article_id`, `column_id`, `title`, `summary`, `content`, `cover_url`, `source`, `author`, `publish_status`, `publish_date`, `is_top`, `is_featured`, `view_count`, `reviewer_id`, `review_comment`, `review_time`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
-- == news(1)：叠加 6 篇已发布 → 已发布数=8，触发 home() news subList(0,5) 截断；并构成 prev/next id 链 ==
(9001, 1, '国庆假期校园安全保卫战：校领导带队开展节前安全大检查',
 '9月30日，校领导带队对东西校区开展节前安全大检查，覆盖消防、食品、实验室等重点区域。',
 '<p>9月30日上午，学校组织开展国庆假期前校园安全大检查。</p><p>检查组深入消防控制室、学生食堂、实验室及宿舍区，逐项排查安全隐患，要求各单位压实安全责任，确保师生度过平安祥和的假期。</p>',
 'https://images.unsplash.com/photo-1587825140708-dfaf72ae4b04?q=80&w=1600&auto=format&fit=crop', '长江大学新闻网', '宣传部', '2', '2026-10-01 09:00:00', '1', '1', 1280, 1, '审核通过', '2026-09-30 17:00:00', 'seed', NOW(), '', NULL, 'news置顶+推荐'),
(9002, 1, '我校在2026年全国大学生学科竞赛中再添佳绩',
 '学校在2026年度多项全国性学科竞赛中获得一等奖12项、二等奖23项。',
 '<p>近日，2026年度全国大学生系列学科竞赛成绩公布。</p><p>我校参赛团队共获得一等奖12项、二等奖23项、三等奖40余项，获奖总数与质量均创历史新高，充分体现了学校创新人才培养成效。</p>',
 'https://images.unsplash.com/photo-1767855017537-0c7c74f50ba9?q=80&w=1600&auto=format&fit=crop', '长江大学新闻网', '教务处', '2', '2026-09-28 14:30:00', '0', '1', 860, 1, '审核通过', '2026-09-27 16:00:00', 'seed', NOW(), '', NULL, 'news推荐'),
(9003, 1, '长江大学2026年新生开学典礼隆重举行',
 '9月26日，学校在西校区运动场举行2026年新生开学典礼，8000余名新同学开启大学篇章。',
 '<p>9月26日上午，长江大学2026年新生开学典礼在西校区运动场举行。</p><p>校长在典礼上勉励新同学立志明德、博学笃行，成长为堪当民族复兴重任的时代新人。8000余名本科新生参加典礼。</p>',
 'https://images.unsplash.com/photo-1633734973050-d6499a977c17?q=80&w=1600&auto=format&fit=crop', '长江大学新闻网', '宣传部', '2', '2026-09-26 10:00:00', '0', '0', 512, 1, '审核通过', '2026-09-25 15:00:00', 'seed', NOW(), '', NULL, 'news-prev/next链成员'),
(9004, 1, '学校召开新学期本科教学督导工作会议',
 '教务处组织召开新学期本科教学督导工作会议，部署课堂教学质量监控重点工作。',
 '<p>9月25日，学校召开新学期本科教学督导工作会议。</p><p>会议对本学期课堂教学质量监控、毕业论文抽查、考试巡视等重点工作进行了安排，强调要以督导促规范、以规范提质量。</p>',
 'https://images.unsplash.com/photo-1598981457915-aea220950616?q=80&w=1600&auto=format&fit=crop', '长江大学新闻网', '教务处', '2', '2026-09-25 09:30:00', '0', '0', 300, 1, '审核通过', '2026-09-24 16:00:00', 'seed', NOW(), '', NULL, 'news-prev/next链成员'),
(9005, 1, '我校举办2026年校园金秋专场招聘会',
 '500余家用人单位进校揽才，提供岗位1.6万余个，现场达成初步意向3000余人。',
 '<p>9月24日，长江大学2026年校园金秋专场招聘会在体育馆举行。</p><p>共有500余家用人单位进校招聘，提供岗位超过1.6万个，涵盖石油、农业、信息、教育等多个领域，现场达成初步就业意向3000余人。</p>',
 'https://images.unsplash.com/photo-1521791136064-7986c2920216?q=80&w=1600&auto=format&fit=crop', '长江大学新闻网', '就业处', '2', '2026-09-24 15:00:00', '0', '0', 176, 1, '审核通过', '2026-09-23 17:00:00', 'seed', NOW(), '', NULL, 'news-prev/next链成员'),
(9014, 1, '【热点】校长讲授“开学第一课”思政大课',
 '校长以“能源报国的青春担当”为题，为2026级新生讲授开学第一课。',
 '<p>9月23日晚，校长在学术报告厅为2026级新生讲授“开学第一课”思政大课。</p><p>课程围绕能源报国的时代使命展开，激励广大学子将个人理想融入国家发展伟业。课程在校内平台直播，观看量持续攀升。</p>',
 'https://images.unsplash.com/photo-1741699428220-65f37f3fbbcb?q=80&w=1600&auto=format&fit=crop', '长江大学新闻网', '宣传部', '2', '2026-09-23 20:00:00', '0', '0', 99999, 1, '审核通过', '2026-09-22 16:00:00', 'seed', NOW(), '', NULL, 'view_count超大值边界'),
-- == news(1) 草稿：status='0' 且 publish_date 为空、审核字段为空 → 公开页/首页均不应出现 ==
(9006, 1, '（草稿）2026年秋季学期通识选修课开课通知',
 '拟于近期发布的秋季学期通识选修课开课安排，内容仍在拟定中。',
 '<p>（本条为草稿，用于测试发布状态机 status=0 草稿态，前台公开列表与首页聚合均不应检索到。）</p>',
 NULL, '教务处', '教务处', '0', NULL, '0', '0', 0, NULL, NULL, NULL, 'seed', NOW(), '', NULL, '草稿态-不应公开'),
-- == academic(2) 待审核：status='1' → 公开页不应出现，供后台审核流程演示 ==
(9007, 2, '（待审核）我校将主办2026年油气勘探国际学术研讨会',
 '会议拟邀请国内外20余位专家学者来校交流，宣传稿待审核。',
 '<p>（本条为待审核态 status=1，用于测试审核发布流转：approveArticle 通过后 status 置 2 方才对外可见。）</p>',
 'https://images.unsplash.com/photo-1578496480240-32d3e0c04525?q=80&w=1600&auto=format&fit=crop', '科研处', '科研处', '1', NULL, '0', '0', 0, NULL, NULL, NULL, 'seed', NOW(), '', NULL, '待审核态-审核流演示'),
-- == academic(2) 已发布：使 academic 栏目拥有 2 篇已发布（基础种子仅 1 篇），prev/next 生效 ==
(9015, 2, '我校科研团队在《Nature》子刊发表重要成果',
 '石油工程学院科研团队在油气储层表征领域取得突破，成果发表于Nature子刊。',
 '<p>近日，我校石油工程学院科研团队在油气储层微观表征领域取得重要研究进展。</p><p>相关成果以长文形式发表于《Nature》旗下子刊，论文第一作者为该校青年教师，长江大学为第一完成单位。</p>',
 'https://images.unsplash.com/photo-1578496480240-32d3e0c04525?q=80&w=1600&auto=format&fit=crop', '科研处', '科研处', '2', '2026-09-20 09:00:00', '0', '0', 640, 1, '审核通过', '2026-09-19 16:00:00', 'seed', NOW(), '', NULL, 'academic第二篇-prev/next'),
-- == topic(7)：基础种子该栏目 0 篇文章，此处补 1 篇已发布推荐，验证 selectPublishedArticlesByColumnCode('topic') 非空 ==
(9008, 7, '专题：学习贯彻2026年全国教育大会精神',
 '学校开设专题，系统报道各单位学习贯彻全国教育大会精神的生动实践。',
 '<p>为深入学习贯彻2026年全国教育大会精神，学校开设专题专栏。</p><p>专题集中报道各学院、各部门的学习贯彻举措与成效，营造浓厚氛围，推动大会精神落地见效。</p>',
 'https://images.unsplash.com/photo-1767855017537-0c7c74f50ba9?q=80&w=1600&auto=format&fit=crop', '宣传部', '宣传部', '2', '2026-09-18 10:00:00', '1', '1', 2050, 1, '审核通过', '2026-09-17 16:00:00', 'seed', NOW(), '', NULL, 'topic栏目首篇-推荐'),
-- == topic(7) 已撤回：status='3' → 撤回后不应出现在公开列表，验证状态机终态 ==
(9009, 7, '（已撤回）关于某线下活动的旧通知',
 '该活动通知已因安排调整撤回，用于测试 status=3 已撤回态不再对外展示。',
 '<p>（本条 publish_status=3 已撤回，用于验证撤回后的文章从公开检索链路中消失。）</p>',
 NULL, '校团委', '校团委', '3', '2026-09-10 09:00:00', '0', '0', 88, 1, '活动调整，撤回发布', '2026-09-12 11:00:00', 'seed', NOW(), '', NULL, '已撤回态-不应公开'),
-- == notice(3) 置顶已发布 ==
(9010, 3, '关于2026年秋季学期开学报到有关事项的通知',
 '各学院：2026年秋季学期定于9月初开学报到，请做好老生返校与新生报到准备。',
 '<p>各学院、各有关单位：</p><p>2026年秋季学期定于9月5日报到、9月6日正式上课。请各学院组织做好老生返校注册、教材领取及新学期教学准备工作，确保教学秩序平稳有序。</p>',
 'https://images.unsplash.com/photo-1598981457915-aea220950616?q=80&w=1600&auto=format&fit=crop', '教务处', '教务处', '2', '2026-09-30 08:00:00', '1', '0', 3200, 1, '审核通过', '2026-09-29 15:00:00', 'seed', NOW(), '', NULL, 'notice置顶'),
-- == campus(4) 已发布且 view_count=0：新发布零浏览边界，验证 incrementViewCount 从 0 起算 ==
(9011, 4, '秋日长大：银杏大道进入最佳观赏期',
 '金秋时节，校园银杏大道一片金黄，成为师生打卡胜地。',
 '<p>进入十月，长江大学校园银杏大道披上金黄色外衣。</p><p>满目灿然的银杏吸引众多师生驻足拍照，成为校园里一道亮丽的秋日风景线，尽显生态校园之美。</p>',
 'https://images.unsplash.com/photo-1775011412407-20a8786f5ccd?q=80&w=1600&auto=format&fit=crop', '校园记者团', '王芳', '2', '2026-10-02 12:00:00', '0', '0', 0, 1, '审核通过', '2026-10-01 16:00:00', 'seed', NOW(), '', NULL, 'view_count=0边界'),
-- == media(5) 已发布但 cover_url 为 NULL：验证前端“暂无图片”回退路径 ==
(9012, 5, '【人民日报】长江大学：深化产教融合培养应用型人才',
 '人民日报报道我校深化产教融合、服务行业产业发展的探索与成效。',
 '<p>《人民日报》刊发报道，聚焦长江大学深化产教融合、协同育人的改革实践。</p><p>报道介绍了学校依托行业特色、与头部企业共建实践平台、联合培养应用型人才的典型做法与育人成效。</p>',
 NULL, '人民日报', '转载', '2', '2026-09-22 09:30:00', '0', '0', 410, 1, '审核通过', '2026-09-21 17:00:00', 'seed', NOW(), '', NULL, '无封面-暂无图片回退'),
-- == people(6) 审批发布完整留痕：reviewer_id/review_comment/review_time 齐备，featured 推荐 ==
(9013, 6, '【长大人】全国优秀教师李教授：三尺讲台写春秋',
 '本刊走近全国优秀教师李教授，记录其三十年扎根教学一线的育人故事。',
 '<p>李教授，长江大学农学院博士生导师，从教三十年，始终把教书育人当作第一要务。</p><p>“讲台虽小，连着的是国家未来。”她主编教材多部，指导学生获国家级竞赛奖励，用坚守与热爱诠释了师者担当。</p>',
 'https://images.unsplash.com/photo-1741699428220-65f37f3fbbcb?q=80&w=1600&auto=format&fit=crop', '宣传部', '编辑部', '2', '2026-09-15 10:00:00', '0', '1', 1500, 1, '内容属实，同意发布', '2026-09-14 16:30:00', 'seed', NOW(), '', NULL, '审批发布完整留痕');

-- ---------- 轮播：selectActiveBanners 三重过滤边界（is_active + 起止时间窗） ----------
-- 列顺序：banner_id, title, image_url, link_url, sort_order, is_active, start_time, end_time, create_by, create_time, update_by, update_time
INSERT INTO `portal_banner` (`banner_id`, `title`, `image_url`, `link_url`, `sort_order`, `is_active`, `start_time`, `end_time`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
-- 当前时间窗内且激活 → 应出现在首页轮播
(9001, '2026年秋季开学季 | 青春正当时', 'https://images.unsplash.com/photo-1633734973050-d6499a977c17?q=80&w=1600&auto=format&fit=crop', '/portal/article/9003', 10, '1', '2026-09-01 00:00:00', '2026-12-31 23:59:59', 'seed', NOW(), '', NULL),
-- 停用（is_active='0'）→ 即便在时间窗内也不应出现
(9002, '（已停用）旧专题轮播', 'https://images.unsplash.com/photo-1767855017537-0c7c74f50ba9?q=80&w=1600&auto=format&fit=crop', '/portal/article/9008', 11, '0', '2026-09-01 00:00:00', '2026-12-31 23:59:59', 'seed', NOW(), '', NULL),
-- 已过期（end_time 早于当前）→ 被时间过滤排除
(9003, '（已过期）暑期招生轮播', 'https://images.unsplash.com/photo-1776861963167-fb0334d15555?q=80&w=1600&auto=format&fit=crop', NULL, 12, '1', '2026-05-01 00:00:00', '2026-08-31 23:59:59', 'seed', NOW(), '', NULL),
-- 未开始（start_time 晚于当前）→ 被时间过滤排除
(9004, '（未开始）2027新年轮播', 'https://images.unsplash.com/photo-1775011412407-20a8786f5ccd?q=80&w=1600&auto=format&fit=crop', NULL, 13, '1', '2027-01-01 00:00:00', '2027-02-28 23:59:59', 'seed', NOW(), '', NULL),
-- 无起止时间窗（start_time/end_time 均 NULL）→ 恒定视为激活可见
(9005, '长江大学欢迎您（常驻）', 'https://images.unsplash.com/photo-1587825140708-dfaf72ae4b04?q=80&w=1600&auto=format&fit=crop', NULL, 14, '1', NULL, NULL, 'seed', NOW(), '', NULL);

-- ============================================================
-- 校验：各态计数应与预期一致（导入后逐条核对）
-- ============================================================
SELECT '栏目-子栏目9001' k, COUNT(*) v FROM portal_column WHERE column_id = 9001
UNION ALL SELECT '栏目-隐藏9002', COUNT(*) FROM portal_column WHERE column_id = 9002 AND is_visible = '0'
UNION ALL SELECT '文章-草稿(0)',   COUNT(*) FROM portal_article WHERE article_id BETWEEN 9000 AND 9999 AND publish_status = '0'
UNION ALL SELECT '文章-待审核(1)', COUNT(*) FROM portal_article WHERE article_id BETWEEN 9000 AND 9999 AND publish_status = '1'
UNION ALL SELECT '文章-已发布(2)', COUNT(*) FROM portal_article WHERE article_id BETWEEN 9000 AND 9999 AND publish_status = '2'
UNION ALL SELECT '文章-已撤回(3)', COUNT(*) FROM portal_article WHERE article_id BETWEEN 9000 AND 9999 AND publish_status = '3'
UNION ALL SELECT '文章-无封面NULL', COUNT(*) FROM portal_article WHERE article_id BETWEEN 9000 AND 9999 AND cover_url IS NULL
UNION ALL SELECT '轮播-激活可见(应=2:9001+9005)', COUNT(*) FROM portal_banner WHERE banner_id BETWEEN 9000 AND 9999
    AND is_active = '1' AND (start_time IS NULL OR start_time <= NOW()) AND (end_time IS NULL OR end_time >= NOW())
UNION ALL SELECT 'news栏目已发布总数(含基础种子应>5,验证截断)', COUNT(*) FROM portal_article a JOIN portal_column c ON a.column_id = c.column_id
    WHERE c.column_code = 'news' AND a.publish_status = '2';
