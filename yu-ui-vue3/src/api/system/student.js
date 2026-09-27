import request from '@/utils/request'

// 查询学生列表
export function listStudent(query) {
  return request({
    url: '/system/student/list',
    method: 'get',
    params: query
  })
}

// 查询学生详细
export function getStudent(studentId) {
  return request({
    url: '/system/student/' + studentId,
    method: 'get'
  })
}

// 新增学生
export function addStudent(data) {
  return request({
    url: '/system/student',
    method: 'post',
    data: data
  })
}

// 修改学生
export function updateStudent(data) {
  return request({
    url: '/system/student',
    method: 'put',
    data: data
  })
}

// 删除学生
export function delStudent(studentId) {
  return request({
    url: '/system/student/' + studentId,
    method: 'delete'
  })
}

// 学籍状态变更
export function changeStudentStatus(data) {
  return request({
    url: '/system/student/status',
    method: 'put',
    data: data
  })
}

// 导出学生
export function exportStudent(query) {
  return request({
    url: '/system/student/export',
    method: 'post',
    params: query,
    responseType: 'blob'
  })
}
