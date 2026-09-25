-- ===============================================
-- phase23：P4 打印与电子凭证 数据库升级脚本（幂等）
-- 内容：
--   1) 新建 sys_print_template（打印凭证模板）表
--   2) 新建 sys_print_record（电子凭证发放记录）表
--   3) 字典 sys_print_biz_type（凭证业务类型）
--   4) 预置 5 类默认打印模板（成绩证明单/证书/准考证/课表/监考通知单）
--   5) 管理端「打印与凭证」菜单（3050 段）+ 门户「我的凭证」菜单（2552/2553）
-- 说明：模板为 Velocity HTML（$!{xxx} 占位、#foreach($r in $rows) 明细，行计数用 $foreach.count），
--       渲染时模型全量 HTML 转义防注入；凭证发放生成 编号+验证码+快照SHA256 支持公开验真
-- 幂等：CREATE TABLE IF NOT EXISTS + INSERT ... WHERE NOT EXISTS
-- menu_id 采用 3050 段（3042 已被 phase22 占用），门户段用 2552+
-- ===============================================

-- 1) 打印凭证模板表
CREATE TABLE IF NOT EXISTS `sys_print_template` (
  `template_id`   bigint       NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_code` varchar(50)  NOT NULL COMMENT '模板编码',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `biz_type`      varchar(30)  NOT NULL COMMENT '业务类型（GRADE成绩证明 SCHEDULE课表 CERTIFICATE证书 EXAM_TICKET准考证 INVIGILATION监考通知单）',
  `content`       mediumtext   COMMENT '模板内容（Velocity HTML）',
  `status`        char(1)      DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by`     varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
  `update_by`     varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     DEFAULT NULL COMMENT '更新时间',
  `remark`        varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`template_id`),
  UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打印凭证模板（P4）';

-- 2) 电子凭证发放记录表
CREATE TABLE IF NOT EXISTS `sys_print_record` (
  `record_id`     bigint       NOT NULL AUTO_INCREMENT COMMENT '凭证ID',
  `biz_type`      varchar(30)  NOT NULL COMMENT '凭证业务类型',
  `biz_id`        bigint       NOT NULL COMMENT '业务主键（成绩单/课表为用户ID，证书certId，准考证seatId，监考单invigilationId）',
  `title`         varchar(200) DEFAULT NULL COMMENT '凭证标题',
  `serial_no`     varchar(64)  NOT NULL COMMENT '凭证编号',
  `verify_code`   varchar(32)  NOT NULL COMMENT '验证码',
  `snapshot`      longtext     COMMENT '数据快照JSON（验真比对基准）',
  `data_hash`     varchar(64)  DEFAULT NULL COMMENT '快照SHA256',
  `template_code` varchar(50)  DEFAULT NULL COMMENT '发放时模板编码（重打按此模板）',
  `receive_id`    bigint       DEFAULT NULL COMMENT '接收人ID（sys_user）',
  `receive_name`  varchar(100) DEFAULT NULL COMMENT '接收人名称',
  `issue_channel` char(1)      DEFAULT '0' COMMENT '发放渠道（0管理端 1门户）',
  `issue_by`      varchar(64)  DEFAULT NULL COMMENT '发放人',
  `issue_time`    datetime     DEFAULT NULL COMMENT '发放时间',
  `status`        char(1)      DEFAULT '0' COMMENT '状态（0有效 1已作废）',
  `create_by`     varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
  `remark`        varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_serial_no` (`serial_no`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  KEY `idx_receive` (`receive_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子凭证发放记录（P4）';

-- 3) 字典：凭证业务类型
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '打印凭证类型', 'sys_print_biz_type', '0', 'admin', sysdate(), 'P4 打印与电子凭证业务类型'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'sys_print_biz_type');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '成绩证明单', 'GRADE', 'sys_print_biz_type', '', 'primary', 'N', '0', 'admin', sysdate(), '学生成绩单打印与电子凭证'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sys_print_biz_type' AND dict_value = 'GRADE');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '课表', 'SCHEDULE', 'sys_print_biz_type', '', 'success', 'N', '0', 'admin', sysdate(), '学生/教师课表打印'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sys_print_biz_type' AND dict_value = 'SCHEDULE');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '证书', 'CERTIFICATE', 'sys_print_biz_type', '', 'warning', 'N', '0', 'admin', sysdate(), '毕业/学位/结业证书打印'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sys_print_biz_type' AND dict_value = 'CERTIFICATE');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '准考证', 'EXAM_TICKET', 'sys_print_biz_type', '', 'info', 'N', '0', 'admin', sysdate(), '考场座位准考证打印'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sys_print_biz_type' AND dict_value = 'EXAM_TICKET');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '监考通知单', 'INVIGILATION', 'sys_print_biz_type', '', 'danger', 'N', '0', 'admin', sysdate(), '教师监考工作通知单打印'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sys_print_biz_type' AND dict_value = 'INVIGILATION');

-- 4) 预置默认模板（仅在编码不存在时插入，不覆盖用户后续修改）
-- 4.1 成绩证明单
INSERT INTO sys_print_template (template_code, template_name, biz_type, content, status, create_by, create_time, remark)
SELECT 'TPL_GRADE_DEFAULT', '成绩证明单默认模板', 'GRADE',
'<html><head><meta charset="utf-8"/><style>body{font-family:"Microsoft YaHei",SimSun,sans-serif;margin:30px;color:#222}h1{text-align:center;font-size:22px;letter-spacing:6px}h3{text-align:center;font-weight:normal;margin-top:2px}.meta{display:flex;flex-wrap:wrap;font-size:13px;margin:12px 0}.meta div{width:33%;padding:3px 0}table{width:100%;border-collapse:collapse;font-size:12px}th,td{border:1px solid #666;padding:5px 6px;text-align:center}th{background:#f2f5f9}.sum{font-size:13px;margin-top:12px;line-height:1.9}.foot{margin-top:26px;font-size:12px;display:flex;justify-content:space-between}.sig{text-align:right;margin-top:36px;font-size:13px;padding-right:30px}</style></head><body>
<h1>$!{schoolName}学生成绩证明单</h1>
<h3>$!{semesterText}</h3>
<div class="meta"><div>姓名：$!{studentName}</div><div>学号：$!{studentNo}</div><div>性别：$!{gender}</div><div>专业：$!{majorName}</div><div>班级：$!{className}</div><div>院系：$!{deptName}</div><div>入学年份：$!{enrollmentYear}</div><div>层次：$!{educationLevel}</div></div>
<table><thead><tr><th>序号</th><th>课程编码</th><th>课程名称</th><th>学期</th><th>学分</th><th>总学时</th><th>考试类型</th><th>平时</th><th>期末</th><th>总评</th><th>绩点</th><th>等级</th><th>通过</th></tr></thead><tbody>
#foreach($r in $rows)<tr><td>$foreach.count</td><td>$!{r.courseCode}</td><td style="text-align:left">$!{r.courseName}</td><td>$!{r.semesterName}</td><td>$!{r.credit}</td><td>$!{r.totalHours}</td><td>$!{r.examTypeText}</td><td>$!{r.regularScore}</td><td>$!{r.examScore}</td><td>$!{r.totalScore}</td><td>$!{r.gradePoint}</td><td>$!{r.gradeLevel}</td><td>$!{r.isPassText}</td></tr>
#end</tbody></table>
<div class="sum">共 $!{courseCount} 门课程，总学分 $!{totalCredit}，通过学分 $!{passedCredit}，平均绩点 GPA $!{gpa}，加权平均分 $!{avgScore}。</div>
<div class="sig">教务处（盖章）<br/>$!{issueDate}</div>
<div class="foot"><span>$!{verifyTip}</span><span>凭证编号：$!{serialNo}</span><span>验证码：$!{verifyCode}</span></div>
</body></html>',
'0', 'admin', sysdate(), 'P4 默认模板：变量 studentName/studentNo/rows(courseCode,courseName,semesterName,credit,totalHours,examTypeText,regularScore,examScore,totalScore,gradePoint,gradeLevel,isPassText)/汇总 gpa,avgScore 等'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_print_template WHERE template_code = 'TPL_GRADE_DEFAULT');

-- 4.2 证书
INSERT INTO sys_print_template (template_code, template_name, biz_type, content, status, create_by, create_time, remark)
SELECT 'TPL_CERT_DEFAULT', '证书默认模板', 'CERTIFICATE',
'<html><head><meta charset="utf-8"/><style>@page{size:A4 landscape;margin:10mm}body{font-family:"Microsoft YaHei",SimSun,serif;margin:40px 70px;color:#222;border:3px double #a00000;padding:40px 50px;text-align:center}h1{font-size:30px;letter-spacing:14px;color:#a00000;margin-bottom:4px}.sub{font-size:14px;letter-spacing:4px;color:#666}p.body{font-size:18px;line-height:2.3;text-align:left;margin-top:34px}.photo{float:right;width:130px;height:170px;border:1px dashed #999;font-size:12px;color:#999;line-height:160px;margin:6px 0 6px 24px}table.info{margin-top:28px;font-size:13px;border-collapse:collapse;text-align:left}table.info td{padding:4px 10px}table.info td.label{color:#666}.no{margin-top:26px;font-size:14px;letter-spacing:1px;text-align:left}.sig{display:flex;justify-content:space-between;margin-top:36px;font-size:15px;padding:0 30px}.verify{margin-top:28px;font-size:11px;color:#999;letter-spacing:.5px}</style></head><body>
<div class="photo">照片</div>
<h1>$!{certTypeName}</h1>
<div class="sub">$!{schoolName}</div>
<p class="body">$!{studentName}，$!{gender}，$!{birthDate}生，$!{enrollmentYear}年入$!{educationLevel}学习，修完规定课程，成绩合格，准予发证。</p>
<p class="body">特发$!{certTypeName}。</p>
<table class="info"><tr><td class="label">学　号：</td><td>$!{studentNo}</td><td class="label">专　业：</td><td>$!{majorName}</td></tr><tr><td class="label">学历层次：</td><td>$!{educationLevel}</td><td class="label">发证日期：</td><td>$!{certDate}</td></tr></table>
<div class="no">证书编号：$!{certNumber}</div>
<div class="sig"><span>校长（签章）：＿＿＿＿＿＿</span><span>$!{schoolName}</span></div>
<div class="verify">$!{verifyTip}　编号 $!{serialNo}　验证码 $!{verifyCode}</div>
</body></html>',
'0', 'admin', sysdate(), 'P4 默认模板：变量 certTypeName/studentName/gender/birthDate/enrollmentYear/educationLevel/certNumber/certDate/majorName 等'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_print_template WHERE template_code = 'TPL_CERT_DEFAULT');

-- 4.3 准考证
INSERT INTO sys_print_template (template_code, template_name, biz_type, content, status, create_by, create_time, remark)
SELECT 'TPL_TICKET_DEFAULT', '准考证默认模板', 'EXAM_TICKET',
'<html><head><meta charset="utf-8"/><style>body{font-family:"Microsoft YaHei",SimSun,sans-serif;margin:36px;color:#222}h1{text-align:center;font-size:20px;letter-spacing:4px}.tip{text-align:center;font-size:12px;color:#666;margin-bottom:12px}.box{display:flex;border:2px solid #333;padding:18px}.photo{width:120px;height:160px;border:1px dashed #999;font-size:12px;color:#999;text-align:center;line-height:150px;margin-right:18px;flex-shrink:0}table.info{width:100%;border-collapse:collapse;font-size:13px}table.info td{border:1px solid #999;padding:8px 10px}table.info td.label{width:15%;background:#f2f5f9;text-align:center}.seat{font-size:26px;font-weight:bold;color:#a00000;text-align:center}ol.rules{font-size:12px;color:#555;line-height:1.9;margin-top:14px}.verify{margin-top:16px;font-size:12px;color:#777;display:flex;justify-content:space-between}</style></head><body>
<h1>$!{schoolName} $!{examName} 准考证</h1>
<div class="tip">请考生提前 30 分钟携带本准考证及学生证入场</div>
<div class="box">
<div class="photo">照片</div>
<table class="info">
<tr><td class="label">姓　名</td><td>$!{studentName}</td><td class="label">学　号</td><td>$!{studentNo}</td></tr>
<tr><td class="label">专　业</td><td>$!{majorName}</td><td class="label">班　级</td><td>$!{className}</td></tr>
<tr><td class="label">课程名称</td><td>$!{courseName}（$!{courseCode}）</td><td class="label">考试日期</td><td>$!{examDate}</td></tr>
<tr><td class="label">考试时间</td><td>$!{startTime} - $!{endTime}（$!{duration}分钟）</td><td class="label">考场</td><td>$!{classroomName}</td></tr>
<tr><td class="label">座位号</td><td colspan="3"><span class="seat">$!{seatNumber}</span>（第$!{rowNumber}行 第$!{colNumber}列）</td></tr>
</table>
</div>
<ol class="rules"><li>凭本准考证和有效证件入场，证件不齐不得进入考场。</li><li>开考后 15 分钟迟到者不得入场，考试 60 分钟内不得交卷离场。</li><li>严禁携带手机等通讯工具及资料入座，违者按作弊处理。</li></ol>
<div class="verify"><span>$!{verifyTip}</span><span>编号：$!{serialNo}　验证码：$!{verifyCode}</span></div>
</body></html>',
'0', 'admin', sysdate(), 'P4 默认模板：变量 examName/courseName/courseCode/examDate/startTime/endTime/duration/classroomName/seatNumber/rowNumber/colNumber 等'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_print_template WHERE template_code = 'TPL_TICKET_DEFAULT');

-- 4.4 课表
INSERT INTO sys_print_template (template_code, template_name, biz_type, content, status, create_by, create_time, remark)
SELECT 'TPL_SCHEDULE_DEFAULT', '课表默认模板', 'SCHEDULE',
'<html><head><meta charset="utf-8"/><style>body{font-family:"Microsoft YaHei",SimSun,sans-serif;margin:30px;color:#222}h1{text-align:center;font-size:20px;letter-spacing:4px}h3{text-align:center;font-weight:normal;margin-top:2px}.meta{display:flex;flex-wrap:wrap;font-size:13px;margin:12px 0}.meta div{width:33%;padding:3px 0}table{width:100%;border-collapse:collapse;font-size:12px}th,td{border:1px solid #666;padding:5px 6px;text-align:center}th{background:#f2f5f9}.foot{margin-top:22px;font-size:12px;display:flex;justify-content:space-between}</style></head><body>
<h1>$!{schoolName}学生课表</h1>
<h3>$!{semesterText}</h3>
<div class="meta"><div>姓名：$!{studentName}</div><div>学号：$!{studentNo}</div><div>专业：$!{majorName}</div><div>班级：$!{className}</div><div>院系：$!{deptName}</div><div>共 $!{courseCount} 门课程</div></div>
<table><thead><tr><th>序号</th><th>课程编码</th><th>课程名称</th><th>教师</th><th>教室</th><th>星期</th><th>节次</th><th>周次</th><th>学分</th></tr></thead><tbody>
#foreach($r in $rows)<tr><td>$foreach.count</td><td>$!{r.courseCode}</td><td style="text-align:left">$!{r.courseName}</td><td>$!{r.teacherName}</td><td>$!{r.classroomName}</td><td>$!{r.weekDayText}</td><td>$!{r.periodText}</td><td>$!{r.weekText}</td><td>$!{r.credit}</td></tr>
#end</tbody></table>
<div class="foot"><span>$!{verifyTip}</span><span>打印日期：$!{issueDate}</span></div>
</body></html>',
'0', 'admin', sysdate(), 'P4 默认模板：rows(courseCode,courseName,teacherName,classroomName,weekDayText,periodText,weekText,credit)'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_print_template WHERE template_code = 'TPL_SCHEDULE_DEFAULT');

-- 4.5 监考通知单
INSERT INTO sys_print_template (template_code, template_name, biz_type, content, status, create_by, create_time, remark)
SELECT 'TPL_INVIG_DEFAULT', '监考通知单默认模板', 'INVIGILATION',
'<html><head><meta charset="utf-8"/><style>body{font-family:"Microsoft YaHei",SimSun,sans-serif;margin:36px;color:#222}h1{text-align:center;font-size:22px;letter-spacing:6px}.meta{font-size:14px;line-height:2.1;margin-top:14px;padding:0 30px}b{min-width:90px;display:inline-block}table{width:100%;border-collapse:collapse;font-size:13px;margin-top:14px}th,td{border:1px solid #666;padding:7px 9px;text-align:center}th{background:#f2f5f9}ol.rules{font-size:12px;color:#555;line-height:1.9;margin-top:16px}.sig{display:flex;justify-content:space-between;font-size:14px;margin-top:40px;padding:0 20px}.verify{margin-top:18px;font-size:12px;color:#777}</style></head><body>
<h1>$!{schoolName}监考工作通知单</h1>
<div class="meta">
<b>$!{teacherName}</b> 老师（$!{deptName}，工号 $!{teacherCode}）：<br/>
您被安排承担以下考试监考工作，职责：<b>$!{dutyTypeName}</b>，同场次监考人员：<b>$!{partners}</b>，请按时到场履行职责。
</div>
<table><thead><tr><th>考试名称</th><th>考试课程</th><th>考试日期</th><th>时间</th><th>考场</th><th>考场容量</th></tr></thead><tbody>
<tr><td>$!{examName}</td><td>$!{courseName}</td><td>$!{examDate}</td><td>$!{startTime} - $!{endTime}</td><td>$!{classroomName}</td><td>$!{capacity}</td></tr>
</tbody></table>
<ol class="rules"><li>提前 20 分钟到考务室领取试卷，提前 10 分钟组织考生入场。</li><li>核验考生证件，督促手机等违禁物品存放于指定位置。</li><li>监考期间不吸烟、不阅读、不使用手机，不做与监考无关的事情。</li><li>考试结束组织收卷，清点无误后交回答卷处。</li></ol>
<div class="sig"><span>教务处（盖章）</span><span>$!{issueDate}</span></div>
<div class="verify">$!{verifyTip}　编号：$!{serialNo}　验证码：$!{verifyCode}</div>
</body></html>',
'0', 'admin', sysdate(), 'P4 默认模板：变量 teacherName/teacherCode/deptName/dutyTypeName/partners/examName/courseName/examDate/startTime/endTime/classroomName/capacity'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_print_template WHERE template_code = 'TPL_INVIG_DEFAULT');

-- 5) 管理端菜单：打印与凭证（parent=1 系统管理，menu_id 3050 段）
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3050,'打印模板',1,11,'printTemplate','system/printTemplate/index','','',1,0,'C','0','0','system:printTemplate:list','print','admin',sysdate(),'',NULL,'P4 打印凭证模板管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3050);

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3051,'模板查询',3050,1,'','','','',1,0,'F','0','0','system:printTemplate:query','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3051);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3052,'模板新增',3050,2,'','','','',1,0,'F','0','0','system:printTemplate:add','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3052);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3053,'模板修改',3050,3,'','','','',1,0,'F','0','0','system:printTemplate:edit','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3053);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3054,'模板删除',3050,4,'','','','',1,0,'F','0','0','system:printTemplate:remove','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3054);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3055,'模板导出',3050,5,'','','','',1,0,'F','0','0','system:printTemplate:export','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3055);

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3056,'电子凭证',1,12,'credential','system/credential/index','','',1,0,'C','0','0','system:credential:list','documentation','admin',sysdate(),'',NULL,'P4 电子凭证发放记录与验真'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3056);

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3057,'凭证查询',3056,1,'','','','',1,0,'F','0','0','system:credential:query','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3057);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3058,'凭证发放',3056,2,'','','','',1,0,'F','0','0','system:credential:issue','#','admin',sysdate(),'',NULL,'单张/批量发放电子凭证'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3058);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3059,'凭证作废',3056,3,'','','','',1,0,'F','0','0','system:credential:revoke','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3059);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3060,'凭证导出',3056,4,'','','','',1,0,'F','0','0','system:credential:export','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3060);

-- 6) 门户菜单：我的凭证（学生 2501 与教师 2509 共用权限 portal:credential:list，2545/2546 段）
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2545,'我的凭证',2501,9,'myCredential','','','',1,0,'C','0','0','portal:credential:list','print','admin',sysdate(),'',NULL,'P4 门户打印与电子凭证'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2545);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2546,'凭证发放',2545,1,'','','','',1,0,'F','0','0','portal:credential:issue','#','admin',sysdate(),'',NULL,'自助发放电子凭证'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2546);

-- 授权：超级管理员(1) 管理端菜单
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,m.menu_id FROM (SELECT 3050 menu_id UNION SELECT 3051 UNION SELECT 3052 UNION SELECT 3053 UNION SELECT 3054 UNION SELECT 3055 UNION SELECT 3056 UNION SELECT 3057 UNION SELECT 3058 UNION SELECT 3059 UNION SELECT 3060) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=1 AND rm.`menu_id`=m.menu_id);

-- 授权：学生角色(7) 我的凭证
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 7,m.menu_id FROM (SELECT 2545 menu_id UNION SELECT 2546) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=7 AND rm.`menu_id`=m.menu_id);

-- 授权：教师角色 我的凭证（凡拥有监考安排菜单 2513 的角色同步授予）
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT DISTINCT r.role_id, t.menu_id
FROM (SELECT 2545 menu_id UNION SELECT 2546) t
JOIN (SELECT DISTINCT rm.`role_id` FROM sys_role_menu rm JOIN sys_role rr ON rr.`role_id` = rm.`role_id` AND rr.del_flag = '0' WHERE rm.menu_id = 2513) r
LEFT JOIN sys_role_menu x ON x.role_id = r.role_id AND x.menu_id = t.menu_id
WHERE x.menu_id IS NULL;

-- 验证
SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id BETWEEN 3050 AND 3060 OR menu_id IN (2545,2546);
SELECT template_code, template_name, biz_type FROM sys_print_template;
