import request from '@/utils/request'

export function listAdjustments(query) {
  return request({ url: '/portal/adjustment/list', method: 'get', params: query })
}

export function applyAdjustment(data) {
  return request({ url: '/portal/adjustment/apply', method: 'post', data })
}
