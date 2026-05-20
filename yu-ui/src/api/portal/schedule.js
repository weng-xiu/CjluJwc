import request from '@/utils/request'

// 查询课表列表（学生端）
export function listSchedule(query) {
  return request({ url: '/portal/schedule/list', method: 'get', params: query })
}

// 查询课表列表（教师端）
export function listTeacherSchedule(query) {
  return request({ url: '/portal/schedule/teacherList', method: 'get', params: query })
}

// 查询课表详情
export function getScheduleDetail(query) {
  return request({ url: '/portal/schedule/detail', method: 'get', params: query })
}
