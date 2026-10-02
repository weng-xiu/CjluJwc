import request from '@/utils/request'

/* ========== 学生侧 ========== */

// 可选题题目列表（后端仅返回 status='1' 可选题）
export function listThesisTopics(query) {
  return request({ url: '/portal/thesis/topics', method: 'get', params: query })
}

// 学生选题（绑定本人学籍，返回生成的论文档案）
export function chooseThesisTopic(topicId) {
  return request({ url: '/portal/thesis/choose', method: 'post', data: { topicId } })
}

// 本人论文档案（含环节留痕 processes），未选题时 data 为 null
export function getMyThesis() {
  return request({ url: '/portal/thesis/my', method: 'get' })
}

// 学生提交环节材料（开题'2' / 中期'3' / 答辩'5'）
export function submitThesisStage(data) {
  return request({ url: '/portal/thesis/submit', method: 'post', data: data })
}

// 学生端学位资格预审（结合论文结论，只读试算）
export function getDegreePreview() {
  return request({ url: '/portal/thesis/degreePreview', method: 'get' })
}

/* ========== 教师 / 教务侧 ========== */

// 指导教师名下论文列表（教务管理员可见全部，分页返回）
export function listAdvisorThesis(query) {
  return request({ url: '/portal/thesis/advisorList', method: 'get', params: query })
}

// 论文档案详情（含环节留痕）
export function getThesisDetail(thesisId) {
  return request({ url: '/portal/thesis/detail/' + thesisId, method: 'get' })
}

// 环节审核（开题'2' / 中期'3' / 答辩'5'）
export function auditThesisStage(data) {
  return request({ url: '/portal/thesis/audit', method: 'post', data: data })
}

// 查重结果登记（score=重复率%）
export function recordThesisCheck(data) {
  return request({ url: '/portal/thesis/check', method: 'post', data: data })
}

// 成绩归档（总评 / 答辩成绩）
export function archiveThesisGrade(data) {
  return request({ url: '/portal/thesis/archive', method: 'post', data: data })
}
