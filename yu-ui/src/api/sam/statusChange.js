import request from '@/utils/request'

export function listStatusChange(query) {
  return request({ url: '/sam/statusChange/list', method: 'get', params: query })
}
export function getStatusChange(changeId) {
  return request({ url: '/sam/statusChange/' + changeId, method: 'get' })
}
export function addStatusChange(data) {
  return request({ url: '/sam/statusChange', method: 'post', data: data })
}
export function updateStatusChange(data) {
  return request({ url: '/sam/statusChange', method: 'put', data: data })
}
export function delStatusChange(changeId) {
  return request({ url: '/sam/statusChange/' + changeId, method: 'delete' })
}
// S5：提交异动申请并启动多级审批流程
export function submitStatusChange(changeId) {
  return request({ url: '/sam/statusChange/submit/' + changeId, method: 'post' })
}
// S5：审批通过（需 taskId + comment）
export function approveStatusChange(changeId, data) {
  return request({ url: '/sam/statusChange/approve/' + changeId, method: 'post', data: data })
}
// S5：驳回异动申请（需 taskId + comment）
export function rejectStatusChange(changeId, data) {
  return request({ url: '/sam/statusChange/reject/' + changeId, method: 'post', data: data })
}
