/**
 * 门户（portal）域前端内置字典（Vue2 主栈）。
 *
 * 背景：Vue2 存量门户页在表格中以 dict-tag 渲染 portal_exam_type / portal_adjust_type /
 * portal_approve_status / portal_duty_type 等字典，但这些 dict_key 从未写入
 * sys_dict_type / sys_dict_data（sql/ 目录已核对，仅 portal_column_type、portal_article_status 入库），
 * 且页面漏写 dicts 声明，于是模板求值 dict.type.xxx 时 dict 为 undefined 直接抛 TypeError，
 * 被 main.js 的全局 errorHandler 捕获后弹「页面出现异常，请刷新重试」，表格行渲染中断。
 *
 * 处置口径与 yu-ui-vue3/src/views/portal/dicts.js 保持一致：不因纯展示问题改数据库，
 * 改为在内置常量中给出同义项，结构与 DictTag 期望的 { value, label, raw: { listClass, cssClass } }
 * 对齐，可直接喂给 <dict-tag>；后续若正式入库，页面改回 dicts: ['portal_xxx'] 即可。
 */

/** 生成 DictTag 可消费的字典项（raw.listClass 决定 el-tag 配色，空串则渲染纯文本） */
function item(value, label, listClass) {
  return { value, label, raw: { listClass: listClass || '', cssClass: '' } }
}

/** 考试类型：aem_exam_plan.exam_type（0 期末考试 / 1 补考 / 2 重修考试） */
export const PORTAL_EXAM_TYPE = [
  item('0', '期末考试', 'primary'),
  item('1', '补考', 'warning'),
  item('2', '重修考试', 'danger')
]

/** 调停课类型：tpm_schedule_adjustment.adjust_type（1 调课 / 2 停课 / 3 补课） */
export const PORTAL_ADJUST_TYPE = [
  item('1', '调课', 'primary'),
  item('2', '停课', 'danger'),
  item('3', '补课', 'success')
]

/**
 * 审批状态（调停课共用）：0 待审批 / 1 已通过 / 2 已驳回 / 3 已撤销
 * 与教室借用的四态（0 待院系审核 / 1 待教务处审核 …）值域不同，不可混用。
 */
export const PORTAL_APPROVE_STATUS = [
  item('0', '待审批', 'warning'),
  item('1', '已通过', 'success'),
  item('2', '已驳回', 'danger'),
  item('3', '已撤销', 'info')
]

/** 监考职责：aem_exam_invigilation.duty_type（0 主监考 / 1 副监考 / 2 巡考） */
export const PORTAL_DUTY_TYPE = [
  item('0', '主监考', 'danger'),
  item('1', '副监考', 'warning'),
  item('2', '巡考', 'info')
]

/**
 * 评教问卷完成状态（学生视角）：门户 questionnaireList 回填的 status
 * 页面语义为「未完成 / 已完成」，故不复用 sys_normal_disable（其标签为正常/停用，会读错业务状态）。
 */
export const PORTAL_EVAL_STATUS = [
  item('0', '未完成', 'info'),
  item('1', '已完成', 'success')
]
