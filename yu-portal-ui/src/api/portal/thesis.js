import request from '@/utils/request'

// 可选题题目列表（仅返回可选题状态）
export function listAvailableTopic(query) {
  return request({ url: '/portal/thesis/topics', method: 'get', params: query })
}

// 学生选题
export function chooseTopic(topicId) {
  return request({ url: '/portal/thesis/choose', method: 'post', data: { topicId } })
}

// 本人论文档案（含环节留痕）
export function getMyThesis() {
  return request({ url: '/portal/thesis/my', method: 'get' })
}

// 提交环节材料（开题、中期检查、答辩）
export function submitStage(data) {
  return request({ url: '/portal/thesis/submit', method: 'post', data })
}

// 论文档案详情（含环节留痕）
export function getThesisDetail(thesisId) {
  return request({ url: '/portal/thesis/detail/' + thesisId, method: 'get' })
}

// 指导教师名下论文列表
export function listAdvisorThesis(query) {
  return request({ url: '/portal/thesis/advisorList', method: 'get', params: query })
}

// 环节审核（开题、中期检查、答辩）
export function auditStage(data) {
  return request({ url: '/portal/thesis/audit', method: 'post', data })
}

// 查重结果登记
export function recordCheck(data) {
  return request({ url: '/portal/thesis/check', method: 'post', data })
}

// 成绩归档
export function archiveGrade(data) {
  return request({ url: '/portal/thesis/archive', method: 'post', data })
}

// 结合论文结论的学位资格预审（只读试算）
export function degreePreview() {
  return request({ url: '/portal/thesis/degreePreview', method: 'get' })
}
