import request from '@/utils/request'

export function listGrade(query) {
  return request({ url: '/portal/grade/list', method: 'get', params: query })
}

export function listGradeForEntry(query) {
  return request({ url: '/portal/grade/entryList', method: 'get', params: query })
}

export function submitGrade(data) {
  return request({ url: '/portal/grade/submit', method: 'post', data })
}

export function getGradeStatistics() {
  return request({ url: '/portal/grade/statistics', method: 'get' })
}
