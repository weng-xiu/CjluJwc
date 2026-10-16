import request from '@/utils/request'

// 查询教师禁排列表
export function listForbidden(query) {
  return request({ url: '/tpm/teacherForbidden/list', method: 'get', params: query })
}

// 查询教师禁排详细
export function getForbidden(forbiddenId) {
  return request({ url: '/tpm/teacherForbidden/' + forbiddenId, method: 'get' })
}

// 新增教师禁排
export function addForbidden(data) {
  return request({ url: '/tpm/teacherForbidden', method: 'post', data: data })
}

// 修改教师禁排
export function updateForbidden(data) {
  return request({ url: '/tpm/teacherForbidden', method: 'put', data: data })
}

// 删除教师禁排
export function delForbidden(forbiddenId) {
  return request({ url: '/tpm/teacherForbidden/' + forbiddenId, method: 'delete' })
}
