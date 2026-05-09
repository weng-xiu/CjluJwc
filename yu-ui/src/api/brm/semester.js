import request from '@/utils/request'

export function listSemester(query) {
  return request({ url: '/brm/semester/list', method: 'get', params: query })
}

export function getSemester(semesterId) {
  return request({ url: '/brm/semester/' + semesterId, method: 'get' })
}

export function addSemester(data) {
  return request({ url: '/brm/semester', method: 'post', data: data })
}

export function updateSemester(data) {
  return request({ url: '/brm/semester', method: 'put', data: data })
}

export function delSemester(semesterId) {
  return request({ url: '/brm/semester/' + semesterId, method: 'delete' })
}
