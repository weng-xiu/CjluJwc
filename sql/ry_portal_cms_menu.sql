-- ----------------------------
-- 门户CMS内容管理(portal_cms)菜单SQL
-- 菜单ID从2600开始（2500-2540已被portal模块占用，2400-2499已被dis模块占用）
-- 已确认2600-2650范围未被占用
-- ----------------------------

-- ====================
-- 一级目录 - 门户内容管理 (parent=0)
-- ====================
insert into sys_menu values('2600', '门户内容管理', '0', '8', 'portal-cms', null, '', '', 1, 0, 'M', '0', '0', '', 'documentation', 'admin', sysdate(), '', null, '');

-- ====================
-- 栏目管理 (parent=2600)
-- ====================
insert into sys_menu values('2601', '栏目管理', '2600', '1', 'columnManage', 'portal/columnManage/index', '', '', 1, 0, 'C', '0', '0', 'portal:column:list', 'tree', 'admin', sysdate(), '', null, '');

-- 栏目管理 - 按钮权限
insert into sys_menu values('2611', '栏目查询', '2601', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:column:query',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2612', '栏目新增', '2601', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:column:add',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2613', '栏目修改', '2601', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:column:edit',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2614', '栏目删除', '2601', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:column:remove', '#', 'admin', sysdate(), '', null, '');

-- ====================
-- 文章管理 (parent=2600)
-- ====================
insert into sys_menu values('2602', '文章管理', '2600', '2', 'articleManage', 'portal/articleManage/index', '', '', 1, 0, 'C', '0', '0', 'portal:article:list', 'edit', 'admin', sysdate(), '', null, '');

-- 文章管理 - 按钮权限
insert into sys_menu values('2621', '文章查询', '2602', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:article:query',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2622', '文章新增', '2602', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:article:add',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2623', '文章修改', '2602', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:article:edit',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2624', '文章删除', '2602', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:article:remove',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2625', '文章发布', '2602', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:article:publish', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2626', '文章审核', '2602', '6', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:article:review',  '#', 'admin', sysdate(), '', null, '');

-- ====================
-- 轮播管理 (parent=2600)
-- ====================
insert into sys_menu values('2603', '轮播管理', '2600', '3', 'bannerManage', 'portal/bannerManage/index', '', '', 1, 0, 'C', '0', '0', 'portal:banner:list', 'slider', 'admin', sysdate(), '', null, '');

-- 轮播管理 - 按钮权限
insert into sys_menu values('2631', '轮播查询', '2603', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:banner:query',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2632', '轮播新增', '2603', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:banner:add',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2633', '轮播修改', '2603', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:banner:edit',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2634', '轮播删除', '2603', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:banner:remove', '#', 'admin', sysdate(), '', null, '');

-- ====================
-- 数据字典 - 字典类型
-- ID从103开始（100-102已被user_mgmt_upgrade.sql占用）
-- ====================
insert into sys_dict_type values(103, '栏目类型', 'portal_column_type',    '0', 'admin', sysdate(), '', null, '门户栏目类型(1列表 2单页 3链接)');
insert into sys_dict_type values(104, '文章状态', 'portal_article_status', '0', 'admin', sysdate(), '', null, '门户文章发布状态(0草稿 1待审核 2已发布 3已撤回)');

-- ====================
-- 数据字典 - 字典数据
-- ID从230开始（200-224已被user_mgmt_upgrade.sql占用）
-- ====================
-- 栏目类型 (portal_column_type)
insert into sys_dict_data values(230, 1, '列表', '1', 'portal_column_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '列表型栏目');
insert into sys_dict_data values(231, 2, '单页', '2', 'portal_column_type', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '单页型栏目');
insert into sys_dict_data values(232, 3, '链接', '3', 'portal_column_type', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '外链型栏目');

-- 文章状态 (portal_article_status)
insert into sys_dict_data values(233, 1, '草稿',   '0', 'portal_article_status', '', 'info',    'Y', '0', 'admin', sysdate(), '', null, '草稿状态');
insert into sys_dict_data values(234, 2, '待审核', '1', 'portal_article_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '待审核状态');
insert into sys_dict_data values(235, 3, '已发布', '2', 'portal_article_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '已发布状态');
insert into sys_dict_data values(236, 4, '已撤回', '3', 'portal_article_status', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '已撤回状态');

-- ====================
-- 角色-菜单授权
-- 为管理员角色(role_id=1)分配门户内容管理全部菜单权限
-- ====================
insert into sys_role_menu values('1', '2600');  -- 门户内容管理(目录)
insert into sys_role_menu values('1', '2601');  -- 栏目管理(菜单)
insert into sys_role_menu values('1', '2611');  -- 栏目查询(按钮)
insert into sys_role_menu values('1', '2612');  -- 栏目新增(按钮)
insert into sys_role_menu values('1', '2613');  -- 栏目修改(按钮)
insert into sys_role_menu values('1', '2614');  -- 栏目删除(按钮)
insert into sys_role_menu values('1', '2602');  -- 文章管理(菜单)
insert into sys_role_menu values('1', '2621');  -- 文章查询(按钮)
insert into sys_role_menu values('1', '2622');  -- 文章新增(按钮)
insert into sys_role_menu values('1', '2623');  -- 文章修改(按钮)
insert into sys_role_menu values('1', '2624');  -- 文章删除(按钮)
insert into sys_role_menu values('1', '2625');  -- 文章发布(按钮)
insert into sys_role_menu values('1', '2626');  -- 文章审核(按钮)
insert into sys_role_menu values('1', '2603');  -- 轮播管理(菜单)
insert into sys_role_menu values('1', '2631');  -- 轮播查询(按钮)
insert into sys_role_menu values('1', '2632');  -- 轮播新增(按钮)
insert into sys_role_menu values('1', '2633');  -- 轮播修改(按钮)
insert into sys_role_menu values('1', '2634');  -- 轮播删除(按钮)
