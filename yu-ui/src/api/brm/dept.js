import request from '@/utils/request'

export function listDept(query) {
  return request({ url: '/brm/dept/list', method: 'get', params: query })
}

export function getDept(deptId) {
  return request({ url: '/brm/dept/' + deptId, method: 'get' })
}

export function addDept(data) {
  return request({ url: '/brm/dept', method: 'post', data: data })
}

export function updateDept(data) {
  return request({ url: '/brm/dept', method: 'put', data: data })
}

export function delDept(deptId) {
  return request({ url: '/brm/dept/' + deptId, method: 'delete' })
}
