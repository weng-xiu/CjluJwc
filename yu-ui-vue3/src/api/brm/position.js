import request from '@/utils/request'

export function listPosition(query) {
  return request({ url: '/brm/position/list', method: 'get', params: query })
}

export function getPosition(posId) {
  return request({ url: '/brm/position/' + posId, method: 'get' })
}

export function addPosition(data) {
  return request({ url: '/brm/position', method: 'post', data: data })
}

export function updatePosition(data) {
  return request({ url: '/brm/position', method: 'put', data: data })
}

export function delPosition(posId) {
  return request({ url: '/brm/position/' + posId, method: 'delete' })
}
