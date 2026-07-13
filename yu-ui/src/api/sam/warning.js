import request from '@/utils/request'

export function listWarning(query) {
  return request({ url: '/sam/warning/list', method: 'get', params: query })
}
export function getWarning(warningId) {
  return request({ url: '/sam/warning/' + warningId, method: 'get' })
}
export function addWarning(data) {
  return request({ url: '/sam/warning', method: 'post', data: data })
}
export function updateWarning(data) {
  return request({ url: '/sam/warning', method: 'put', data: data })
}
export function delWarning(warningId) {
  return request({ url: '/sam/warning/' + warningId, method: 'delete' })
}

// 预警统计
export function getWarningStatistics(semesterId) {
  return request({ url: '/sam/warning/statistics', method: 'get', params: { semesterId } })
}

// 手动批量生成预警
export function generateBatch(semesterId) {
  return request({ url: '/sam/warning/generateBatch', method: 'post', params: { semesterId } })
}

// 学生查看自己的预警
export function getMyWarnings(studentId, semesterId) {
  return request({ url: '/sam/warning/myWarnings', method: 'get', params: { studentId, semesterId } })
}
