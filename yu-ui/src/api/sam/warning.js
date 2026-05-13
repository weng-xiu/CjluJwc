import request from '@/utils/request'

export function listWarning(query) {
  return request({ url: '/sam/warning/list', method: 'get', params: query })
}
export function getWarning(warningId) {
  return request({ url: '/sam/warning/' + warningId, method: 'get' })
}
export function addWarning(data) {
  return request({ url: '/sam/warning', method: 'post', data: data })
}
export function updateWarning(data) {
  return request({ url: '/sam/warning', method: 'put', data: data })
}
export function delWarning(warningId) {
  return request({ url: '/sam/warning/' + warningId, method: 'delete' })
}
