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

// 批量导入成绩
export function importGrade(data) {
  return request({ url: '/aem/gradeRecord/importData', method: 'post', headers: { 'Content-Type': 'multipart/form-data' }, data: data })
}

// A5：查询成绩录入开放期状态
export function getEntryWindow() {
  return request({ url: '/aem/gradeRecord/entryWindow', method: 'get' })
}

// A5：教师提交成绩（批量）
export function submitGrade(gradeIds) {
  return request({ url: '/aem/gradeRecord/submit/' + gradeIds, method: 'put' })
}

// A5：教研室审核成绩（approved=true 锁定，false 驳回）
export function auditGrade(gradeIds, approved) {
  return request({ url: '/aem/gradeRecord/audit/' + gradeIds, method: 'put', params: { approved } })
}

// A5：解锁已锁定成绩
export function unlockGrade(gradeIds) {
  return request({ url: '/aem/gradeRecord/unlock/' + gradeIds, method: 'put' })
}
