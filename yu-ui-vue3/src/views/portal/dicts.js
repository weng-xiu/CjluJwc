/**
 * 门户（portal）模块前端内置字典。
 *
 * 背景：Vue2 存量门户页在表格里以 dict-tag 渲染 portal_notice_type / portal_exam_type 等
 * 字典，但这些 dict_key 从未写入 sys_dict_type / sys_dict_data（sql/ 目录已核对，仅
 * portal_column_type、portal_article_status 两个字典入库）。Vue2 侧还漏写 dicts 声明，
 * 于是这些列在生产环境恒为空白 —— 属于展示层缺陷，而非数据缺失。
 *
 * 迁移到 Vue3 时不改数据库（避免为纯展示问题引入变更与回滚脚本），改为在本文件内置同义项，
 * 结构与 utils/dict 归一化后的元素对齐（{ value, label, listClass }），可直接喂给 <dict-tag>
 * 与 <el-option>，保证「查询下拉」与「列表标签」共用同一份口径。
 *
 * 后续若把这些枚举正式入库为 sys_dict_data，只需将页面改为 dicts: ['portal_xxx'] 并删除本文件
 * 对应常量即可，调用点无需其它改动。
 */

/** 通知类型（教务通知 / 通知管理共用） */
export const PORTAL_NOTICE_TYPE = [
  { value: '1', label: '选课通知', listClass: 'primary' },
  { value: '2', label: '考试通知', listClass: 'warning' },
  { value: '3', label: '学籍通知', listClass: 'success' },
  { value: '4', label: '综合通知', listClass: 'info' }
]

/**
 * 通知发布状态。
 * 补齐 '2'=已撤回：与 PortalNotice 的 publishStatus 注释及 readConverterExp（0草稿 1已发布 2已撤回）对齐，
 * 旧枚举只到 '1'，一旦库中存在 '2' 则该列渲染为空白。
 */
export const PORTAL_PUBLISH_STATUS = [
  { value: '0', label: '草稿', listClass: 'info' },
  { value: '1', label: '已发布', listClass: 'success' },
  { value: '2', label: '已撤回', listClass: 'warning' }
]

/** 通知目标角色 */
export const PORTAL_TARGET_ROLE = [
  { value: '0', label: '所有人', listClass: 'info' },
  { value: '1', label: '学生', listClass: 'primary' },
  { value: '2', label: '教师', listClass: 'success' }
]

/** 考试类型 */
export const PORTAL_EXAM_TYPE = [
  { value: '0', label: '期末考试', listClass: 'primary' },
  { value: '1', label: '补考', listClass: 'warning' },
  { value: '2', label: '重修考试', listClass: 'danger' }
]

/**
 * 监考职责类型。
 * 口径来源：sql/aem.sql 中 aem_exam_invigilation.duty_type char(1) DEFAULT '0'
 * COMMENT '职责（0主监考 1副监考 2巡考）'，并与 sql/phase6_portal_mobile_seed.sql 的
 * 演示数据（主监考写 '0'、副监考写 '1'）一致。旧门户页曾按 1/2/3 渲染，导致主监考整列空白。
 */
export const PORTAL_DUTY_TYPE = [
  { value: '0', label: '主监考', listClass: 'danger' },
  { value: '1', label: '副监考', listClass: 'warning' },
  { value: '2', label: '巡考', listClass: 'info' }
]

/**
 * 调停课类型。
 * 注：sql/tpm.sql 中 adjust_type 为 varchar(20)，建表注释写的是「调课/停课/补课」中文字面量，
 * 但已入库的 sys_dict_type=tpm_adjust_type（phase4/phase25）取值为 '1'/'2'/'3'，且管理端
 * views/tpm/adjust 与门户端的新增表单实际提交的都是数字。以写入路径为准，注释为历史遗留；
 * 若后续清洗数据为中文字面量，改为 dicts: ['tpm_adjust_type'] 并删除本常量即可。
 */
export const PORTAL_ADJUST_TYPE = [
  { value: '1', label: '调课', listClass: 'primary' },
  { value: '2', label: '停课', listClass: 'danger' },
  { value: '3', label: '补课', listClass: 'success' }
]

/**
 * 审批状态（调停课、学籍异动共用）。
 * 补齐 '3'=已撤销：sql/phase24_p6_portal_apply_loop.sql 明确门户端撤销调停课申请后落库
 * approve_status='3'，旧门户枚举缺该值，已撤销的申请在列表里渲染为空白。
 */
export const PORTAL_APPROVE_STATUS = [
  { value: '0', label: '待审批', listClass: 'warning' },
  { value: '1', label: '已通过', listClass: 'success' },
  { value: '2', label: '已驳回', listClass: 'danger' },
  { value: '3', label: '已撤销', listClass: 'info' }
]

/**
 * 学籍异动类型。
 * 口径来源：sql/sam.sql 中 sam_student_status.change_type char(1) NOT NULL
 * COMMENT '异动类型（0休学 1复学 2转学 3退学 4保留学籍）'；
 * yu-portal PortalStudentStatusController.PORTAL_CHANGE_TYPES = ["0","1","3"]（休学/复学/退学）
 * 与后端白名单互证。旧门户枚举整体错位一位（1休学/2复学/3退学/4转专业），
 * 会把库中「休学(0)」渲染成空白、把「复学(1)」渲染成「休学」——属于会读错业务状态的展示缺陷。
 */
export const PORTAL_STATUS_CHANGE_TYPE = [
  { value: '0', label: '休学', listClass: 'warning' },
  { value: '1', label: '复学', listClass: 'success' },
  { value: '2', label: '转学', listClass: 'primary' },
  { value: '3', label: '退学', listClass: 'danger' },
  { value: '4', label: '保留学籍', listClass: 'info' }
]

/**
 * 学籍状态：sam_student.student_status（0在读 1休学 2退学 3毕业 4转出 5保留学籍），
 * 与 SamStudent 的 @Excel(readConverterExp) 同口径。
 */
export const PORTAL_STUDENT_STATUS = [
  { value: '0', label: '在读', listClass: 'success' },
  { value: '1', label: '休学', listClass: 'warning' },
  { value: '2', label: '退学', listClass: 'danger' },
  { value: '3', label: '毕业', listClass: 'info' },
  { value: '4', label: '转出', listClass: 'info' },
  { value: '5', label: '保留学籍', listClass: 'primary' }
]

/** 门户端可自助申请的异动类型（与后端 PORTAL_CHANGE_TYPES 同口径，用于表单下拉裁剪） */
export const PORTAL_STATUS_CHANGE_TYPE_SELF_SERVICE = PORTAL_STATUS_CHANGE_TYPE.filter((d) =>
  ['0', '1', '3'].includes(d.value)
)

/** 考试成绩安排状态：aem_exam_plan.plan_status（0未安排 1已安排 2已发布） */
export const PORTAL_EXAM_PLAN_STATUS = [
  { value: '0', label: '未安排', listClass: 'info' },
  { value: '1', label: '已安排', listClass: 'warning' },
  { value: '2', label: '已发布', listClass: 'success' }
]

/** 评教问卷进行状态：aem_evaluation_questionnaire.eval_status（0未开始 1进行中 2已结束） */
export const PORTAL_EVAL_STATUS = [
  { value: '0', label: '未开始', listClass: 'info' },
  { value: '1', label: '进行中', listClass: 'success' },
  { value: '2', label: '已结束', listClass: 'warning' }
]

/** 问卷是否匿名：aem_evaluation_questionnaire.is_anonymous（0实名 1匿名） */
export const PORTAL_ANONYMOUS = [
  { value: '0', label: '实名', listClass: 'primary' },
  { value: '1', label: '匿名', listClass: 'info' }
]

/** 成绩记录考核类型：aem_grade_record.exam_type（0正考 1补考 2重修），与考试计划的枚举标签不同 */
export const GRADE_EXAM_TYPE = [
  { value: '0', label: '正考', listClass: 'primary' },
  { value: '1', label: '补考', listClass: 'warning' },
  { value: '2', label: '重修', listClass: 'danger' }
]

/**
 * 评教问卷「完成状态」（学生视角：是否已提交过该问卷）。
 * 对应后端 PortalEvaluationController.questionnaireList 回填的 transient 字段 completed，
 * 取值是布尔而非字典码，故用布尔两值枚举喂 dict-tag。
 */
export const PORTAL_EVAL_COMPLETED = [
  { value: true, label: '已评', listClass: 'success' },
  { value: false, label: '未评', listClass: 'warning' }
]

/** 成绩提交状态（对应 AemGradeRecord.submitStatus，A5 成绩提交流转） */
export const PORTAL_SUBMIT_STATUS = [
  { value: '0', label: '未提交', listClass: 'info' },
  { value: '1', label: '已提交待审', listClass: 'warning' },
  { value: '2', label: '已锁定', listClass: 'success' },
  { value: '3', label: '已驳回可改', listClass: 'danger' }
]

/**
 * 教室借用审批状态（Flowable 两级审批状态机）。
 * 口径来源：BrmClassroomBorrow.approveStatus 的 @Excel readConverterExp
 * （0=待院系审核 1=待教务处审核 2=已通过 3=已驳回 4=已撤销），
 * 与调停课/异动的四态 PORTAL_APPROVE_STATUS 值域不同，单独内置避免误渲染。
 */
export const PORTAL_BORROW_APPROVE_STATUS = [
  { value: '0', label: '待院系审核', listClass: 'warning' },
  { value: '1', label: '待教务处审核', listClass: 'primary' },
  { value: '2', label: '已通过', listClass: 'success' },
  { value: '3', label: '已驳回', listClass: 'danger' },
  { value: '4', label: '已撤销', listClass: 'info' }
]

/* ========== 毕业论文（设计）全过程（phase32，口径源自 SamThesis/SamThesisTopic/SamThesisProcess 的 readConverterExp） ========== */

/** 当前环节（student 提交阶段、stage_status 状态机共用） */
export const THESIS_STAGE = [
  { value: '1', label: '选题', listClass: 'info' },
  { value: '2', label: '开题', listClass: 'primary' },
  { value: '3', label: '中期检查', listClass: 'primary' },
  { value: '4', label: '查重', listClass: 'warning' },
  { value: '5', label: '答辩', listClass: 'warning' },
  { value: '6', label: '成绩归档', listClass: 'success' }
]

/** 环节状态（0待提交 1待审核 2已通过 3已退回） */
export const THESIS_STAGE_STATUS = [
  { value: '0', label: '待提交', listClass: 'info' },
  { value: '1', label: '待审核', listClass: 'warning' },
  { value: '2', label: '已通过', listClass: 'success' },
  { value: '3', label: '已退回', listClass: 'danger' }
]

/** 成绩等级（0优秀 1良好 2中等 3及格 4不及格） */
export const THESIS_GRADE_LEVEL = [
  { value: '0', label: '优秀', listClass: 'success' },
  { value: '1', label: '良好', listClass: 'primary' },
  { value: '2', label: '中等', listClass: 'warning' },
  { value: '3', label: '及格', listClass: 'info' },
  { value: '4', label: '不及格', listClass: 'danger' }
]

/** 选题库状态（0待审核 1可选题 2已选满 3已下架），门户 topics 仅返回 '1' */
export const THESIS_TOPIC_STATUS = [
  { value: '0', label: '待审核', listClass: 'info' },
  { value: '1', label: '可选题', listClass: 'success' },
  { value: '2', label: '已选满', listClass: 'warning' },
  { value: '3', label: '已下架', listClass: 'danger' }
]

/** 题目来源（0教师科研课题 1生产社会实践 2学生自拟 3学科竞赛） */
export const THESIS_TOPIC_SOURCE = [
  { value: '0', label: '教师科研课题', listClass: 'primary' },
  { value: '1', label: '生产社会实践', listClass: 'success' },
  { value: '2', label: '学生自拟', listClass: 'info' },
  { value: '3', label: '学科竞赛', listClass: 'warning' }
]

/** 难度（1基础 2中等 3较高） */
export const THESIS_DIFFICULTY = [
  { value: '1', label: '基础', listClass: 'success' },
  { value: '2', label: '中等', listClass: 'warning' },
  { value: '3', label: '较高', listClass: 'danger' }
]

/** 环节留痕结果（0退回 1通过 2仅记录） */
export const THESIS_PROCESS_RESULT = [
  { value: '0', label: '退回', listClass: 'danger' },
  { value: '1', label: '通过', listClass: 'success' },
  { value: '2', label: '仅记录', listClass: 'info' }
]
