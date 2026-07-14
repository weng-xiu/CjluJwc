import request from '@/utils/request'

// 查询日程列表
export function listSchedule(query) {
  return request({ url: '/oa/schedule/list', method: 'get', params: query })
}

// 查询日程详细
export function getSchedule(scheduleId) {
  return request({ url: '/oa/schedule/' + scheduleId, method: 'get' })
}

// 新增日程
export function addSchedule(data) {
  return request({ url: '/oa/schedule', method: 'post', data: data })
}

// 修改日程
export function updateSchedule(data) {
  return request({ url: '/oa/schedule', method: 'put', data: data })
}

// 删除日程
export function delSchedule(scheduleId) {
  return request({ url: '/oa/schedule/' + scheduleId, method: 'delete' })
}

// 导出日程
export function exportSchedule(query) {
  return request({ url: '/oa/schedule/export', method: 'post', params: query, responseType: 'blob' })
}
