import request from '@/utils/request'

// 检测排课冲突
export function detectConflicts(semesterId) {
  return request({ url: '/tpm/scheduleOpt/detectConflicts', method: 'get', params: { semesterId } })
}

// 检查指定教室在某时间段是否可用
export function canAssignClassroom(params) {
  return request({ url: '/tpm/scheduleOpt/canAssign', method: 'get', params })
}

// 查询可用教室
export function findAvailableClassrooms(params) {
  return request({ url: '/tpm/scheduleOpt/availableClassrooms', method: 'get', params })
}

// 自动分配教室
export function autoAssignClassrooms(semesterId) {
  return request({ url: '/tpm/scheduleOpt/autoAssign', method: 'post', params: { semesterId } })
}

// 时间片自动排课预览（T1，不落库）
export function autoSchedulePreview(params) {
  return request({ url: '/tpm/scheduleOpt/autoSchedulePreview', method: 'post', params })
}

// 时间片自动排课落库（T1）
export function autoScheduleApply(params) {
  return request({ url: '/tpm/scheduleOpt/autoScheduleApply', method: 'post', params })
}
