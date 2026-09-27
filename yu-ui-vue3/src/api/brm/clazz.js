import request from '@/utils/request'

export function listClazz(query) {
  return request({ url: '/brm/clazz/list', method: 'get', params: query })
}

export function getClazz(classId) {
  return request({ url: '/brm/clazz/' + classId, method: 'get' })
}

export function addClazz(data) {
  return request({ url: '/brm/clazz', method: 'post', data: data })
}

export function updateClazz(data) {
  return request({ url: '/brm/clazz', method: 'put', data: data })
}

export function delClazz(classId) {
  return request({ url: '/brm/clazz/' + classId, method: 'delete' })
}
