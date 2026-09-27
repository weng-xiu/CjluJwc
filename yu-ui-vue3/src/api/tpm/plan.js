import request from '@/utils/request'

// 查询培养方案列表
export function listPlan(query) {
  return request({ url: '/tpm/plan/list', method: 'get', params: query })
}

// 查询培养方案详细
export function getPlan(planId) {
  return request({ url: '/tpm/plan/' + planId, method: 'get' })
}

// 新增培养方案
export function addPlan(data) {
  return request({ url: '/tpm/plan', method: 'post', data: data })
}

// 修改培养方案
export function updatePlan(data) {
  return request({ url: '/tpm/plan', method: 'put', data: data })
}

// 删除培养方案
export function delPlan(planId) {
  return request({ url: '/tpm/plan/' + planId, method: 'delete' })
}

// 发布培养方案
export function publishPlan(planId) {
  return request({ url: '/tpm/plan/publish/' + planId, method: 'put' })
}

// 废止培养方案
export function deprecatePlan(planId) {
  return request({ url: '/tpm/plan/deprecate/' + planId, method: 'put' })
}

// 保存培养方案主表及子表（课程库、学分结构）
export function savePlanWithChildren(data) {
  return request({ url: '/tpm/plan/saveWithChildren', method: 'post', data: data })
}

// T3：复制培养方案为新草稿版本
export function copyPlan(planId) {
  return request({ url: '/tpm/plan/copy/' + planId, method: 'post' })
}
