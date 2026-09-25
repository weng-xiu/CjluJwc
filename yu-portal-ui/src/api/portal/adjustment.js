import request from '@/utils/request'

export function listAdjustments(query) {
  return request({ url: '/portal/adjustment/list', method: 'get', params: query })
}

export function applyAdjustment(data) {
  return request({ url: '/portal/adjustment/apply', method: 'post', data })
}

// 本人任课排课列表（调停课申请选择源）
export function listMySchedules(query) {
  return request({ url: '/portal/adjustment/mySchedules', method: 'get', params: query })
}

// 撤销本人待审的调停课申请
export function cancelAdjustment(adjustId) {
  return request({ url: '/portal/adjustment/cancel/' + adjustId, method: 'put' })
}

// 审批人：待审列表（P5 移动端审批）
export function listPendingAdjustments(query) {
  return request({ url: '/portal/adjustment/pendingList', method: 'get', params: query })
}

// 审批人：通过
export function approveAdjustment(adjustId, data) {
  return request({ url: '/portal/adjustment/approve/' + adjustId, method: 'post', data })
}

// 审批人：驳回
export function rejectAdjustment(adjustId, data) {
  return request({ url: '/portal/adjustment/reject/' + adjustId, method: 'post', data })
}
