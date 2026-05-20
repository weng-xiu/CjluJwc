import request from '@/utils/request'

export function listSchedule(query) {
  return request({ url: '/portal/schedule/list', method: 'get', params: query })
}

export function listTeacherSchedule(query) {
  return request({ url: '/portal/schedule/teacherList', method: 'get', params: query })
}
