import request from '@/utils/request'

export function getStudentInfo() {
  return request({ url: '/portal/studentStatus/info', method: 'get' })
}

export function applyStatusChange(data) {
  return request({ url: '/portal/studentStatus/apply', method: 'post', data })
}

export function listStatusChanges(query) {
  return request({ url: '/portal/studentStatus/changeList', method: 'get', params: query })
}
