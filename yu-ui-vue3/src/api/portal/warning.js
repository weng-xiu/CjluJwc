import request from '@/utils/request'

// 学生端：查询本人学业预警列表
export function listMyWarning(query) {
  return request({ url: '/portal/warning/list', method: 'get', params: query })
}

// 学生端：本人预警统计（按类型/级别汇总）
export function myWarningStatistics(semesterId) {
  return request({ url: '/portal/warning/statistics', method: 'get', params: { semesterId } })
}
