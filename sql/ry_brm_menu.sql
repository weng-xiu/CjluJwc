-- ----------------------------
-- 基础资源管理(brm)菜单SQL
-- ----------------------------
-- 一级菜单 - 基础资源管理目录
insert into sys_menu values('2000', '基础资源管理', '0', '5', 'brm',              null, '', '', 1, 0, 'M', '0', '0', '',              'education', 'admin', sysdate(), '', null, '');

-- ====================
-- 基础信息管理 (parent=2000)
-- ====================
insert into sys_menu values('2001', '基础信息管理', '2000', '1', 'basic',         null, '', '', 1, 0, 'M', '0', '0', '',              'tree',      'admin', sysdate(), '', null, '');
insert into sys_menu values('2002', '学年管理',     '2001', '1', 'year',          'brm/year/index',          '', '', 1, 0, 'C', '0', '0', 'brm:year:list',        'date',      'admin', sysdate(), '', null, '');
insert into sys_menu values('2003', '学期管理',     '2001', '2', 'semester',      'brm/semester/index',      '', '', 1, 0, 'C', '0', '0', 'brm:semester:list',    'time-range','admin', sysdate(), '', null, '');
insert into sys_menu values('2004', '院系管理',     '2001', '3', 'dept',          'brm/dept/index',          '', '', 1, 0, 'C', '0', '0', 'brm:dept:list',        'tree',      'admin', sysdate(), '', null, '');
insert into sys_menu values('2005', '专业管理',     '2001', '4', 'major',         'brm/major/index',         '', '', 1, 0, 'C', '0', '0', 'brm:major:list',       'education', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2006', '班级管理',     '2001', '5', 'clazz',         'brm/clazz/index',         '', '', 1, 0, 'C', '0', '0', 'brm:class:list',       'people',    'admin', sysdate(), '', null, '');
insert into sys_menu values('2007', '校区管理',     '2001', '6', 'campus',        'brm/campus/index',        '', '', 1, 0, 'C', '0', '0', 'brm:campus:list',      'location',  'admin', sysdate(), '', null, '');
insert into sys_menu values('2008', '教学楼管理',   '2001', '7', 'building',      'brm/building/index',      '', '', 1, 0, 'C', '0', '0', 'brm:building:list',    'component', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2009', '教室管理',     '2001', '8', 'classroom',     'brm/classroom/index',     '', '', 1, 0, 'C', '0', '0', 'brm:classroom:list',   'house',     'admin', sysdate(), '', null, '');

-- ====================
-- 教师信息管理 (parent=2000)
-- ====================
insert into sys_menu values('2010', '教师信息管理', '2000', '2', 'teacher-group',  null,                      '', '', 1, 0, 'M', '0', '0', '',                     'user',      'admin', sysdate(), '', null, '');
insert into sys_menu values('2011', '教师管理',     '2010', '1', 'teacher',       'brm/teacher/index',       '', '', 1, 0, 'C', '0', '0', 'brm:teacher:list',     'user',      'admin', sysdate(), '', null, '');
insert into sys_menu values('2012', '教师任职',     '2010', '2', 'position',      'brm/position/index',      '', '', 1, 0, 'C', '0', '0', 'brm:position:list',    'star',      'admin', sysdate(), '', null, '');
insert into sys_menu values('2013', '授课资格',     '2010', '3', 'qualification', 'brm/qualification/index', '', '', 1, 0, 'C', '0', '0', 'brm:qualification:list', 'validCode', 'admin', sysdate(), '', null, '');

-- ====================
-- 教室资源管理 (parent=2000)
-- ====================
insert into sys_menu values('2014', '教室资源管理', '2000', '3', 'classroom-resource', null,                 '', '', 1, 0, 'M', '0', '0', '',                   'monitor',   'admin', sysdate(), '', null, '');
insert into sys_menu values('2015', '教室类型',     '2014', '1', 'roomtype',         'brm/roomtype/index',    '', '', 1, 0, 'C', '0', '0', 'brm:roomtype:list',    'nested',    'admin', sysdate(), '', null, '');
insert into sys_menu values('2016', '教室借用',     '2014', '2', 'borrow',           'brm/borrow/index',      '', '', 1, 0, 'C', '0', '0', 'brm:borrow:list',      'form',      'admin', sysdate(), '', null, '');
insert into sys_menu values('2017', '多媒体设备',   '2014', '3', 'equip',            'brm/equip/index',       '', '', 1, 0, 'C', '0', '0', 'brm:equip:list',       'monitor',   'admin', sysdate(), '', null, '');
insert into sys_menu values('2018', '设备维护',     '2014', '4', 'maintenance',      'brm/maintenance/index', '', '', 1, 0, 'C', '0', '0', 'brm:maintenance:list', 'tool',      'admin', sysdate(), '', null, '');

-- ====================
-- 按钮权限 - 学年管理 (parent=2002)
-- ====================
insert into sys_menu values('2019', '学年查询', '2002', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:year:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2020', '学年新增', '2002', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:year:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2021', '学年修改', '2002', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:year:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2022', '学年删除', '2002', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:year:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2023', '学年导出', '2002', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:year:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 学期管理 (parent=2003)
insert into sys_menu values('2024', '学期查询', '2003', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:semester:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2025', '学期新增', '2003', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:semester:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2026', '学期修改', '2003', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:semester:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2027', '学期删除', '2003', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:semester:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2028', '学期导出', '2003', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:semester:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 院系管理 (parent=2004)
insert into sys_menu values('2029', '院系查询', '2004', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:dept:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2030', '院系新增', '2004', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:dept:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2031', '院系修改', '2004', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:dept:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2032', '院系删除', '2004', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:dept:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2033', '院系导出', '2004', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:dept:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 专业管理 (parent=2005)
insert into sys_menu values('2034', '专业查询', '2005', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:major:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2035', '专业新增', '2005', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:major:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2036', '专业修改', '2005', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:major:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2037', '专业删除', '2005', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:major:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2038', '专业导出', '2005', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:major:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 班级管理 (parent=2006)
insert into sys_menu values('2039', '班级查询', '2006', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:class:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2040', '班级新增', '2006', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:class:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2041', '班级修改', '2006', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:class:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2042', '班级删除', '2006', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:class:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2043', '班级导出', '2006', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:class:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 校区管理 (parent=2007)
insert into sys_menu values('2044', '校区查询', '2007', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:campus:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2045', '校区新增', '2007', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:campus:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2046', '校区修改', '2007', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:campus:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2047', '校区删除', '2007', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:campus:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2048', '校区导出', '2007', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:campus:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 教学楼管理 (parent=2008)
insert into sys_menu values('2049', '教学楼查询', '2008', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:building:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2050', '教学楼新增', '2008', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:building:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2051', '教学楼修改', '2008', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:building:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2052', '教学楼删除', '2008', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:building:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2053', '教学楼导出', '2008', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:building:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 教室管理 (parent=2009)
insert into sys_menu values('2054', '教室查询', '2009', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:classroom:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2055', '教室新增', '2009', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:classroom:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2056', '教室修改', '2009', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:classroom:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2057', '教室删除', '2009', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:classroom:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2058', '教室导出', '2009', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:classroom:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 教师管理 (parent=2011)
insert into sys_menu values('2059', '教师查询', '2011', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:teacher:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2060', '教师新增', '2011', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:teacher:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2061', '教师修改', '2011', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:teacher:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2062', '教师删除', '2011', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:teacher:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2063', '教师导出', '2011', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:teacher:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 教师任职 (parent=2012)
insert into sys_menu values('2064', '任职查询', '2012', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:position:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2065', '任职新增', '2012', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:position:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2066', '任职修改', '2012', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:position:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2067', '任职删除', '2012', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:position:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2068', '任职导出', '2012', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:position:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 授课资格 (parent=2013)
insert into sys_menu values('2069', '资格查询', '2013', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:qualification:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2070', '资格新增', '2013', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:qualification:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2071', '资格修改', '2013', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:qualification:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2072', '资格删除', '2013', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:qualification:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2073', '资格导出', '2013', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:qualification:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 教室类型 (parent=2015)
insert into sys_menu values('2074', '类型查询', '2015', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:roomtype:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2075', '类型新增', '2015', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:roomtype:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2076', '类型修改', '2015', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:roomtype:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2077', '类型删除', '2015', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:roomtype:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2078', '类型导出', '2015', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:roomtype:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 教室借用 (parent=2016)
insert into sys_menu values('2079', '借用查询', '2016', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:borrow:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2080', '借用新增', '2016', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:borrow:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2081', '借用修改', '2016', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:borrow:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2082', '借用删除', '2016', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:borrow:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2083', '借用导出', '2016', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:borrow:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 多媒体设备 (parent=2017)
insert into sys_menu values('2084', '设备查询', '2017', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:equip:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2085', '设备新增', '2017', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:equip:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2086', '设备修改', '2017', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:equip:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2087', '设备删除', '2017', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:equip:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2088', '设备导出', '2017', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:equip:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 设备维护 (parent=2018)
insert into sys_menu values('2089', '维护查询', '2018', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:maintenance:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2090', '维护新增', '2018', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:maintenance:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2091', '维护修改', '2018', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:maintenance:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2092', '维护删除', '2018', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:maintenance:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2093', '维护导出', '2018', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'brm:maintenance:export',   '#', 'admin', sysdate(), '', null, '');
