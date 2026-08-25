import request from '@/utils/request'

// 查询成绩记录列表
export function listGradeRecord(query) {
  return request({ url: '/aem/gradeRecord/list', method: 'get', params: query })
}

// 查询成绩记录详细
export function getGradeRecord(gradeId) {
  return request({ url: '/aem/gradeRecord/' + gradeId, method: 'get' })
}

// 查询成绩记录明细（含复核记录子表）
export function getGradeRecordDetail(gradeId) {
  return request({ url: '/aem/gradeRecord/detail/' + gradeId, method: 'get' })
}

// 新增成绩记录
export function addGradeRecord(data) {
  return request({ url: '/aem/gradeRecord', method: 'post', data: data })
}

// 修改成绩记录
export function updateGradeRecord(data) {
  return request({ url: '/aem/gradeRecord', method: 'put', data: data })
}

// 删除成绩记录
export function delGradeRecord(gradeId) {
  return request({ url: '/aem/gradeRecord/' + gradeId, method: 'delete' })
}
