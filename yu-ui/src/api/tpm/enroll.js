import request from '@/utils/request'

// 查询选课名单列表
export function listEnroll(query) {
  return request({ url: '/tpm/enroll/list', method: 'get', params: query })
}

// 查询选课名单详细
export function getEnroll(enrollId) {
  return request({ url: '/tpm/enroll/' + enrollId, method: 'get' })
}

// 新增选课名单
export function addEnroll(data) {
  return request({ url: '/tpm/enroll', method: 'post', data: data })
}

// 修改选课名单
export function updateEnroll(data) {
  return request({ url: '/tpm/enroll', method: 'put', data: data })
}

// 删除选课名单
export function delEnroll(enrollId) {
  return request({ url: '/tpm/enroll/' + enrollId, method: 'delete' })
}
