import request from '@/utils/request'

// 查询排课管理列表
export function listSchedule(query) {
  return request({ url: '/tpm/schedule/list', method: 'get', params: query })
}

// 查询排课管理详细
export function getSchedule(scheduleId) {
  return request({ url: '/tpm/schedule/' + scheduleId, method: 'get' })
}

// 新增排课管理
export function addSchedule(data) {
  return request({ url: '/tpm/schedule', method: 'post', data: data })
}

// 修改排课管理
export function updateSchedule(data) {
  return request({ url: '/tpm/schedule', method: 'put', data: data })
}

// 删除排课管理
export function delSchedule(scheduleId) {
  return request({ url: '/tpm/schedule/' + scheduleId, method: 'delete' })
}
