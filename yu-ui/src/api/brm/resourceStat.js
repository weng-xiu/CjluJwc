import request from '@/utils/request'

// 资源总览
export function resourceOverview(query) {
  return request({ url: '/brm/resourceStat/overview', method: 'get', params: query })
}

// 教室利用率明细
export function classroomUtilization(query) {
  return request({ url: '/brm/resourceStat/classroomUtilization', method: 'get', params: query })
}

// 教师工作量明细
export function teacherWorkload(query) {
  return request({ url: '/brm/resourceStat/teacherWorkload', method: 'get', params: query })
}

// 维保到期提醒
export function maintenanceDue() {
  return request({ url: '/brm/resourceStat/maintenanceDue', method: 'get' })
}
