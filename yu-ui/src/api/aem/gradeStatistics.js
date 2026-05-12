import request from '@/utils/request'

// 查询成绩统计列表
export function listGradeStatistics(query) {
  return request({ url: '/aem/gradeStatistics/list', method: 'get', params: query })
}

// 查询成绩统计详细
export function getGradeStatistics(statId) {
  return request({ url: '/aem/gradeStatistics/' + statId, method: 'get' })
}
