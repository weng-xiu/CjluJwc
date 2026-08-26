import request from '@/utils/request'

// 查询评教问题列表
export function listQuestion(query) {
  return request({ url: '/aem/question/list', method: 'get', params: query })
}

// 查询评教问题详细
export function getQuestion(questionId) {
  return request({ url: '/aem/question/' + questionId, method: 'get' })
}

// 新增评教问题
export function addQuestion(data) {
  return request({ url: '/aem/question', method: 'post', data: data })
}

// 修改评教问题
export function updateQuestion(data) {
  return request({ url: '/aem/question', method: 'put', data: data })
}

// 删除评教问题
export function delQuestion(questionId) {
  return request({ url: '/aem/question/' + questionId, method: 'delete' })
}

// 批量导入评教题目
export function importQuestion(data) {
  return request({ url: '/aem/question/importData', method: 'post', headers: { 'Content-Type': 'multipart/form-data' }, data: data })
}
