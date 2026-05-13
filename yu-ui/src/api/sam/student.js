import request from '@/utils/request'

// 查询学生学籍列表
export function listStudent(query) {
  return request({ url: '/sam/student/list', method: 'get', params: query })
}

// 查询学生学籍详细
export function getStudent(studentId) {
  return request({ url: '/sam/student/' + studentId, method: 'get' })
}

// 新增学生学籍
export function addStudent(data) {
  return request({ url: '/sam/student', method: 'post', data: data })
}

// 修改学生学籍
export function updateStudent(data) {
  return request({ url: '/sam/student', method: 'put', data: data })
}

// 删除学生学籍
export function delStudent(studentId) {
  return request({ url: '/sam/student/' + studentId, method: 'delete' })
}
