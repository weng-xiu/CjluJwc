import request from '@/utils/request'

// ---------------- 选题库（sam_thesis_topic） ----------------

// 查询毕业论文选题库列表
export function listThesisTopic(query) {
  return request({ url: '/sam/thesisTopic/list', method: 'get', params: query })
}

// 查询选题详细
export function getThesisTopic(topicId) {
  return request({ url: '/sam/thesisTopic/' + topicId, method: 'get' })
}

// 新增选题
export function addThesisTopic(data) {
  return request({ url: '/sam/thesisTopic', method: 'post', data })
}

// 修改选题
export function updateThesisTopic(data) {
  return request({ url: '/sam/thesisTopic', method: 'put', data })
}

// 选题审核（通过=可选题，不通过=下架）
export function auditThesisTopic(topicId, pass, opinion) {
  return request({ url: '/sam/thesisTopic/audit/' + topicId, method: 'put', params: { pass, opinion } })
}

// 选题上架/下架
export function changeThesisTopicStatus(topicId, status) {
  return request({ url: '/sam/thesisTopic/status/' + topicId, method: 'put', params: { status } })
}

// 删除选题
export function delThesisTopic(topicId) {
  return request({ url: '/sam/thesisTopic/' + topicId, method: 'delete' })
}

// ---------------- 论文全过程（sam_thesis） ----------------

// 查询论文档案列表
export function listThesis(query) {
  return request({ url: '/sam/thesis/list', method: 'get', params: query })
}

// 论文档案详情（含环节留痕）
export function getThesis(thesisId) {
  return request({ url: '/sam/thesis/' + thesisId, method: 'get' })
}

// 过程统计（汇总指标 + 各环节进度）
export function statThesis(query) {
  return request({ url: '/sam/thesis/stat', method: 'get', params: query })
}

// 新增论文档案（管理员代建）
export function addThesis(data) {
  return request({ url: '/sam/thesis', method: 'post', data })
}

// 修改论文档案
export function updateThesis(data) {
  return request({ url: '/sam/thesis', method: 'put', data })
}

// 删除论文档案
export function delThesis(thesisId) {
  return request({ url: '/sam/thesis/' + thesisId, method: 'delete' })
}

// 环节审核（开题、中期检查、答辩）
export function auditThesisStage(data) {
  return request({ url: '/sam/thesis/stage/audit', method: 'post', data })
}

// 代学生补录环节材料
export function submitThesisStage(data) {
  return request({ url: '/sam/thesis/stage/submit', method: 'post', data })
}

// 查重结果登记
export function recordThesisCheck(data) {
  return request({ url: '/sam/thesis/check', method: 'post', data })
}

// 成绩归档
export function archiveThesisGrade(data) {
  return request({ url: '/sam/thesis/archive', method: 'post', data })
}

// 抽检状态维护
export function markThesisSample(data) {
  return request({ url: '/sam/thesis/sample', method: 'put', data })
}
