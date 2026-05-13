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
