-- ----------------------------
-- 办公自动化管理(oa)字典SQL
-- ----------------------------

-- 字典类型
insert into sys_dict_type values('105', '公文类型',     'oa_document_type',    '0', 'admin', sysdate(), '', null, 'OA公文类型(0发文 1收文 2签报)');
insert into sys_dict_type values('106', '公文密级',     'oa_secret_level',     '0', 'admin', sysdate(), '', null, 'OA公文密级(0公开 1内部 2秘密 3机密)');
insert into sys_dict_type values('107', '紧急程度',     'oa_urgent_level',     '0', 'admin', sysdate(), '', null, 'OA紧急程度(0普通 1加急 2特急)');
insert into sys_dict_type values('108', '公文状态',     'oa_document_status',  '0', 'admin', sysdate(), '', null, 'OA公文状态(0草稿 1审批中 2已发布 3已归档 4已驳回)');
insert into sys_dict_type values('109', '流程状态',     'oa_process_status',   '0', 'admin', sysdate(), '', null, 'OA流程状态(0运行中 1已完成 2已驳回 3已撤销)');
insert into sys_dict_type values('110', '审批操作',     'oa_action_type',      '0', 'admin', sysdate(), '', null, 'OA审批操作(0通过 1驳回 2转办 3会签 4撤销)');
insert into sys_dict_type values('111', '会议类型',     'oa_meeting_type',     '0', 'admin', sysdate(), '', null, 'OA会议类型(0普通会议 1视频会议 2紧急会议)');
insert into sys_dict_type values('112', '会议状态',     'oa_meeting_status',   '0', 'admin', sysdate(), '', null, 'OA会议状态(0未开始 1进行中 2已结束 3已取消)');
insert into sys_dict_type values('113', '出席状态',     'oa_attend_status',    '0', 'admin', sysdate(), '', null, 'OA出席状态(0待确认 1参加 2不参加 3待定)');
insert into sys_dict_type values('114', '公告类型',     'oa_notice_type',      '0', 'admin', sysdate(), '', null, 'OA公告类型(0通知 1公告 2通报)');
insert into sys_dict_type values('115', '发布范围',     'oa_publish_scope',    '0', 'admin', sysdate(), '', null, 'OA发布范围(0全体 1指定部门 2指定人员)');
insert into sys_dict_type values('116', '发布状态',     'oa_publish_status',   '0', 'admin', sysdate(), '', null, 'OA发布状态(0草稿 1已发布 2已撤回)');
insert into sys_dict_type values('117', '日程类型',     'oa_schedule_type',    '0', 'admin', sysdate(), '', null, 'OA日程类型(0个人 1会议 2任务 3提醒)');
insert into sys_dict_type values('118', '提醒方式',     'oa_remind_type',      '0', 'admin', sysdate(), '', null, 'OA提醒方式(0不提醒 1系统消息 2邮件 3短信)');
insert into sys_dict_type values('119', '日程状态',     'oa_schedule_status',  '0', 'admin', sysdate(), '', null, 'OA日程状态(0正常 1已完成 2已取消)');

-- 公文类型 (oa_document_type)
insert into sys_dict_data values('250', 1, '发文',   '0', 'oa_document_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '发文');
insert into sys_dict_data values('251', 2, '收文',   '1', 'oa_document_type', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '收文');
insert into sys_dict_data values('252', 3, '签报',   '2', 'oa_document_type', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '签报');

-- 公文密级 (oa_secret_level)
insert into sys_dict_data values('253', 1, '公开',   '0', 'oa_secret_level', '', 'success', 'Y', '0', 'admin', sysdate(), '', null, '公开');
insert into sys_dict_data values('254', 2, '内部',   '1', 'oa_secret_level', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '内部');
insert into sys_dict_data values('255', 3, '秘密',   '2', 'oa_secret_level', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '秘密');
insert into sys_dict_data values('256', 4, '机密',   '3', 'oa_secret_level', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '机密');

-- 紧急程度 (oa_urgent_level)
insert into sys_dict_data values('257', 1, '普通',   '0', 'oa_urgent_level', '', 'info',    'Y', '0', 'admin', sysdate(), '', null, '普通');
insert into sys_dict_data values('258', 2, '加急',   '1', 'oa_urgent_level', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '加急');
insert into sys_dict_data values('259', 3, '特急',   '2', 'oa_urgent_level', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '特急');

-- 公文状态 (oa_document_status)
insert into sys_dict_data values('260', 1, '草稿',   '0', 'oa_document_status', '', 'info',    'Y', '0', 'admin', sysdate(), '', null, '草稿');
insert into sys_dict_data values('261', 2, '审批中', '1', 'oa_document_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '审批中');
insert into sys_dict_data values('262', 3, '已发布', '2', 'oa_document_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '已发布');
insert into sys_dict_data values('263', 4, '已归档', '3', 'oa_document_status', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '已归档');
insert into sys_dict_data values('264', 5, '已驳回', '4', 'oa_document_status', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '已驳回');

-- 流程状态 (oa_process_status)
insert into sys_dict_data values('265', 1, '运行中', '0', 'oa_process_status', '', 'warning', 'Y', '0', 'admin', sysdate(), '', null, '运行中');
insert into sys_dict_data values('266', 2, '已完成', '1', 'oa_process_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '已完成');
insert into sys_dict_data values('267', 3, '已驳回', '2', 'oa_process_status', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '已驳回');
insert into sys_dict_data values('268', 4, '已撤销', '3', 'oa_process_status', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '已撤销');

-- 审批操作 (oa_action_type)
insert into sys_dict_data values('269', 1, '通过',   '0', 'oa_action_type', '', 'success', 'Y', '0', 'admin', sysdate(), '', null, '通过');
insert into sys_dict_data values('270', 2, '驳回',   '1', 'oa_action_type', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '驳回');
insert into sys_dict_data values('271', 3, '转办',   '2', 'oa_action_type', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '转办');
insert into sys_dict_data values('272', 4, '会签',   '3', 'oa_action_type', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '会签');
insert into sys_dict_data values('273', 5, '撤销',   '4', 'oa_action_type', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '撤销');

-- 会议类型 (oa_meeting_type)
insert into sys_dict_data values('274', 1, '普通会议', '0', 'oa_meeting_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '普通会议');
insert into sys_dict_data values('275', 2, '视频会议', '1', 'oa_meeting_type', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '视频会议');
insert into sys_dict_data values('276', 3, '紧急会议', '2', 'oa_meeting_type', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '紧急会议');

-- 会议状态 (oa_meeting_status)
insert into sys_dict_data values('277', 1, '未开始', '0', 'oa_meeting_status', '', 'info',    'Y', '0', 'admin', sysdate(), '', null, '未开始');
insert into sys_dict_data values('278', 2, '进行中', '1', 'oa_meeting_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '进行中');
insert into sys_dict_data values('279', 3, '已结束', '2', 'oa_meeting_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '已结束');
insert into sys_dict_data values('280', 4, '已取消', '3', 'oa_meeting_status', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '已取消');

-- 出席状态 (oa_attend_status)
insert into sys_dict_data values('281', 1, '待确认', '0', 'oa_attend_status', '', 'info',    'Y', '0', 'admin', sysdate(), '', null, '待确认');
insert into sys_dict_data values('282', 2, '参加',   '1', 'oa_attend_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '参加');
insert into sys_dict_data values('283', 3, '不参加', '2', 'oa_attend_status', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '不参加');
insert into sys_dict_data values('284', 4, '待定',   '3', 'oa_attend_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '待定');

-- 公告类型 (oa_notice_type)
insert into sys_dict_data values('285', 1, '通知',   '0', 'oa_notice_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '通知');
insert into sys_dict_data values('286', 2, '公告',   '1', 'oa_notice_type', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '公告');
insert into sys_dict_data values('287', 3, '通报',   '2', 'oa_notice_type', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '通报');

-- 发布范围 (oa_publish_scope)
insert into sys_dict_data values('288', 1, '全体',     '0', 'oa_publish_scope', '', 'success', 'Y', '0', 'admin', sysdate(), '', null, '全体');
insert into sys_dict_data values('289', 2, '指定部门', '1', 'oa_publish_scope', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '指定部门');
insert into sys_dict_data values('290', 3, '指定人员', '2', 'oa_publish_scope', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '指定人员');

-- 发布状态 (oa_publish_status)
insert into sys_dict_data values('291', 1, '草稿',   '0', 'oa_publish_status', '', 'info',    'Y', '0', 'admin', sysdate(), '', null, '草稿');
insert into sys_dict_data values('292', 2, '已发布', '1', 'oa_publish_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '已发布');
insert into sys_dict_data values('293', 3, '已撤回', '2', 'oa_publish_status', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '已撤回');

-- 日程类型 (oa_schedule_type)
insert into sys_dict_data values('294', 1, '个人',   '0', 'oa_schedule_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '个人');
insert into sys_dict_data values('295', 2, '会议',   '1', 'oa_schedule_type', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '会议');
insert into sys_dict_data values('296', 3, '任务',   '2', 'oa_schedule_type', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '任务');
insert into sys_dict_data values('297', 4, '提醒',   '3', 'oa_schedule_type', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '提醒');

-- 提醒方式 (oa_remind_type)
insert into sys_dict_data values('298', 1, '不提醒',   '0', 'oa_remind_type', '', 'info',    'Y', '0', 'admin', sysdate(), '', null, '不提醒');
insert into sys_dict_data values('299', 2, '系统消息', '1', 'oa_remind_type', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '系统消息');
insert into sys_dict_data values('300', 3, '邮件',     '2', 'oa_remind_type', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '邮件');
insert into sys_dict_data values('301', 4, '短信',     '3', 'oa_remind_type', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '短信');

-- 日程状态 (oa_schedule_status)
insert into sys_dict_data values('302', 1, '正常',   '0', 'oa_schedule_status', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '正常');
insert into sys_dict_data values('303', 2, '已完成', '1', 'oa_schedule_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '已完成');
insert into sys_dict_data values('304', 3, '已取消', '2', 'oa_schedule_status', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '已取消');
