import request from '@/utils/request'

// 查询课程库列表
export function listCourseLib(query) {
  return request({ url: '/tpm/courseLib/list', method: 'get', params: query })
}

// 查询课程库详细
export function getCourseLib(courseId) {
  return request({ url: '/tpm/courseLib/' + courseId, method: 'get' })
}

// 新增课程库
export function addCourseLib(data) {
  return request({ url: '/tpm/courseLib', method: 'post', data: data })
}

// 修改课程库
export function updateCourseLib(data) {
  return request({ url: '/tpm/courseLib', method: 'put', data: data })
}

// 删除课程库
export function delCourseLib(courseId) {
  return request({ url: '/tpm/courseLib/' + courseId, method: 'delete' })
}
