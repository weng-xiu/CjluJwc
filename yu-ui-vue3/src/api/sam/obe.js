import request from '@/utils/request'

// ===== OBE 毕业要求（N1）——对应后端 SamObeController /sam/obe =====

// 毕业要求列表
export function listRequirement(query) {
  return request({ url: '/sam/obe/requirement/list', method: 'get', params: query })
}

// 毕业要求详情
export function getRequirement(grId) {
  return request({ url: '/sam/obe/requirement/' + grId, method: 'get' })
}

// 新增毕业要求
export function addRequirement(data) {
  return request({ url: '/sam/obe/requirement', method: 'post', data: data })
}

// 修改毕业要求
export function updateRequirement(data) {
  return request({ url: '/sam/obe/requirement', method: 'put', data: data })
}

// 删除毕业要求（级联软删指标点/课程支撑）
export function delRequirement(grIds) {
  return request({ url: '/sam/obe/requirement/' + grIds, method: 'delete' })
}

// ===== 指标点 =====

export function listIndicator(query) {
  return request({ url: '/sam/obe/indicator/list', method: 'get', params: query })
}

export function getIndicator(ipId) {
  return request({ url: '/sam/obe/indicator/' + ipId, method: 'get' })
}

export function addIndicator(data) {
  return request({ url: '/sam/obe/indicator', method: 'post', data: data })
}

export function updateIndicator(data) {
  return request({ url: '/sam/obe/indicator', method: 'put', data: data })
}

export function delIndicator(ipIds) {
  return request({ url: '/sam/obe/indicator/' + ipIds, method: 'delete' })
}

// ===== 课程支撑矩阵 =====

export function listSupport(query) {
  return request({ url: '/sam/obe/support/list', method: 'get', params: query })
}

export function getSupport(supportId) {
  return request({ url: '/sam/obe/support/' + supportId, method: 'get' })
}

export function addSupport(data) {
  return request({ url: '/sam/obe/support', method: 'post', data: data })
}

export function updateSupport(data) {
  return request({ url: '/sam/obe/support', method: 'put', data: data })
}

export function delSupport(supportIds) {
  return request({ url: '/sam/obe/support/' + supportIds, method: 'delete' })
}

// ===== 达成度计算 =====

// 群体口径：某培养方案下各毕业要求达成度（semesterId 可空）
export function calculateCohort(planId, semesterId) {
  return request({ url: '/sam/obe/calculate/cohort', method: 'get', params: { planId, semesterId } })
}

// 个体口径：某学生在某培养方案下各毕业要求达成度
export function calculateByStudent(planId, studentId) {
  return request({ url: '/sam/obe/calculate/student', method: 'get', params: { planId, studentId } })
}

// 达成度看板：群体口径 + 汇总统计
export function obeDashboard(planId, semesterId) {
  return request({ url: '/sam/obe/dashboard', method: 'get', params: { planId, semesterId } })
}
