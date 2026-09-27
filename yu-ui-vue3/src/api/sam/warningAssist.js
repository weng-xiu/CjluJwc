import request from '@/utils/request'

export function listWarningAssist(query) {
  return request({ url: '/sam/warningAssist/list', method: 'get', params: query })
}
export function getWarningAssist(assistId) {
  return request({ url: '/sam/warningAssist/' + assistId, method: 'get' })
}
export function dispatchWarningAssist(data) {
  return request({ url: '/sam/warningAssist/dispatch', method: 'post', data: data })
}
export function claimWarningAssist(assistId) {
  return request({ url: '/sam/warningAssist/claim/' + assistId, method: 'put' })
}
export function followWarningAssist(assistId, data) {
  return request({ url: '/sam/warningAssist/follow/' + assistId, method: 'post', data: data })
}
export function finishWarningAssist(assistId, finishRemark, resolveWarning) {
  return request({ url: '/sam/warningAssist/finish/' + assistId, method: 'put', params: { finishRemark, resolveWarning } })
}
export function closeWarningAssist(assistId, finishRemark) {
  return request({ url: '/sam/warningAssist/close/' + assistId, method: 'put', params: { finishRemark } })
}
export function delWarningAssist(assistId) {
  return request({ url: '/sam/warningAssist/' + assistId, method: 'delete' })
}
