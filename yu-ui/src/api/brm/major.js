import request from '@/utils/request'

export function listMajor(query) {
  return request({ url: '/brm/major/list', method: 'get', params: query })
}

export function getMajor(majorId) {
  return request({ url: '/brm/major/' + majorId, method: 'get' })
}

export function addMajor(data) {
  return request({ url: '/brm/major', method: 'post', data: data })
}

export function updateMajor(data) {
  return request({ url: '/brm/major', method: 'put', data: data })
}

export function delMajor(majorId) {
  return request({ url: '/brm/major/' + majorId, method: 'delete' })
}
