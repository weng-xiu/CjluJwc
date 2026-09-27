import request from '@/utils/request'

// 查询评教结果列表
export function listEvaluationResult(query) {
  return request({ url: '/aem/evaluationResult/list', method: 'get', params: query })
}

// 查询评教结果详细
export function getEvaluationResult(resultId) {
  return request({ url: '/aem/evaluationResult/' + resultId, method: 'get' })
}

// 新增评教结果
export function addEvaluationResult(data) {
  return request({ url: '/aem/evaluationResult', method: 'post', data: data })
}

// 修改评教结果
export function updateEvaluationResult(data) {
  return request({ url: '/aem/evaluationResult', method: 'put', data: data })
}

// 删除评教结果
export function delEvaluationResult(resultId) {
  return request({ url: '/aem/evaluationResult/' + resultId, method: 'delete' })
}
