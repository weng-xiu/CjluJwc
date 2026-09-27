import request from '@/utils/request'

export function listQualification(query) {
  return request({ url: '/brm/qualification/list', method: 'get', params: query })
}

export function getQualification(qualId) {
  return request({ url: '/brm/qualification/' + qualId, method: 'get' })
}

export function addQualification(data) {
  return request({ url: '/brm/qualification', method: 'post', data: data })
}

export function updateQualification(data) {
  return request({ url: '/brm/qualification', method: 'put', data: data })
}

export function delQualification(qualId) {
  return request({ url: '/brm/qualification/' + qualId, method: 'delete' })
}
