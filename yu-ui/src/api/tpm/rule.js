import request from '@/utils/request'

// 查询选课规则列表
export function listRule(query) {
  return request({ url: '/tpm/rule/list', method: 'get', params: query })
}

// 查询选课规则详细
export function getRule(ruleId) {
  return request({ url: '/tpm/rule/' + ruleId, method: 'get' })
}

// 新增选课规则
export function addRule(data) {
  return request({ url: '/tpm/rule', method: 'post', data: data })
}

// 修改选课规则
export function updateRule(data) {
  return request({ url: '/tpm/rule', method: 'put', data: data })
}

// 删除选课规则
export function delRule(ruleId) {
  return request({ url: '/tpm/rule/' + ruleId, method: 'delete' })
}
