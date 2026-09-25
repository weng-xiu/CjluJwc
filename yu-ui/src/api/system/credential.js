import request from '@/utils/request'

// 查询凭证发放记录列表
export function listCredential(query) {
  return request({ url: '/system/credential/list', method: 'get', params: query })
}

// 查询凭证发放记录详细
export function getCredential(recordId) {
  return request({ url: '/system/credential/' + recordId, method: 'get' })
}

// 渲染凭证预览（不落库）
export function renderCredential(bizType, bizId, semesterId) {
  return request({ url: '/system/credential/render', method: 'get', params: { bizType, bizId, semesterId } })
}

// 发放凭证（生成编号/验证码并落记录，返回凭证 HTML）
export function issueCredential(bizType, bizId, semesterId) {
  return request({ url: '/system/credential/issue', method: 'post', params: { bizType, bizId, semesterId } })
}

// 批量发放凭证
export function batchIssueCredential(bizType, scopeId) {
  return request({ url: '/system/credential/batchIssue', method: 'post', params: { bizType, scopeId } })
}

// 重新打开已发放凭证
export function printCredential(recordId) {
  return request({ url: '/system/credential/print/' + recordId, method: 'get' })
}

// 作废凭证
export function revokeCredential(recordId) {
  return request({ url: '/system/credential/revoke/' + recordId, method: 'put' })
}

// 公开验真
export function verifyCredential(serialNo, verifyCode) {
  return request({ url: '/system/credential/verify', method: 'get', params: { serialNo, verifyCode } })
}
