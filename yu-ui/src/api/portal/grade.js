import request from '@/utils/request'

// 查询成绩列表（学生端）
export function listGrade(query) {
  return request({ url: '/portal/grade/list', method: 'get', params: query })
}

// 查询成绩录入列表（教师端）
export function listGradeEntry(query) {
  return request({ url: '/portal/grade/entryList', method: 'get', params: query })
}

// 录入成绩
export function addGradeEntry(data) {
  return request({ url: '/portal/grade/entry', method: 'post', data: data })
}

// 修改成绩
export function updateGradeEntry(data) {
  return request({ url: '/portal/grade/entry', method: 'put', data: data })
}
