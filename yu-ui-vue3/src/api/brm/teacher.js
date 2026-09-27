import request from '@/utils/request'

export function listTeacher(query) {
  return request({ url: '/brm/teacher/list', method: 'get', params: query })
}

export function getTeacher(teacherId) {
  return request({ url: '/brm/teacher/' + teacherId, method: 'get' })
}

export function addTeacher(data) {
  return request({ url: '/brm/teacher', method: 'post', data: data })
}

export function updateTeacher(data) {
  return request({ url: '/brm/teacher', method: 'put', data: data })
}

export function delTeacher(teacherId) {
  return request({ url: '/brm/teacher/' + teacherId, method: 'delete' })
}
