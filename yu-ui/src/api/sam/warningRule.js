import request from '@/utils/request'

// 查询预警规则配置列表
export function listWarningRule(query) {
  return request({ url: '/sam/warningRule/list', method: 'get', params: query })
}

// 查询预警规则配置详细
export function getWarningRule(ruleId) {
  return request({ url: '/sam/warningRule/' + ruleId, method: 'get' })
}

// 新增预警规则配置
export function addWarningRule(data) {
  return request({ url: '/sam/warningRule', method: 'post', data: data })
}

// 修改预警规则配置
export function updateWarningRule(data) {
  return request({ url: '/sam/warningRule', method: 'put', data: data })
}

// 删除预警规则配置
export function delWarningRule(ruleId) {
  return request({ url: '/sam/warningRule/' + ruleId, method: 'delete' })
}
