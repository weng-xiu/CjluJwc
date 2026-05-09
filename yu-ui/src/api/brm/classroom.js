import request from '@/utils/request'

export function listClassroom(query) {
  return request({ url: '/brm/classroom/list', method: 'get', params: query })
}

export function getClassroom(classroomId) {
  return request({ url: '/brm/classroom/' + classroomId, method: 'get' })
}

export function addClassroom(data) {
  return request({ url: '/brm/classroom', method: 'post', data: data })
}

export function updateClassroom(data) {
  return request({ url: '/brm/classroom', method: 'put', data: data })
}

export function delClassroom(classroomId) {
  return request({ url: '/brm/classroom/' + classroomId, method: 'delete' })
}
