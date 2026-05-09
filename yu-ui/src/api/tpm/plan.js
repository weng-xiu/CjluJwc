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
