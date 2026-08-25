import request from '@/utils/request'

// 查询评教问卷列表
export function listQuestionnaire(query) {
  return request({ url: '/aem/questionnaire/list', method: 'get', params: query })
}

// 查询评教问卷详细
export function getQuestionnaire(questionnaireId) {
  return request({ url: '/aem/questionnaire/' + questionnaireId, method: 'get' })
}

// 查询评教问卷明细（含题目子表）
export function getQuestionnaireDetail(questionnaireId) {
  return request({ url: '/aem/questionnaire/detail/' + questionnaireId, method: 'get' })
}

// 新增评教问卷
export function addQuestionnaire(data) {
  return request({ url: '/aem/questionnaire', method: 'post', data: data })
}

// 修改评教问卷
export function updateQuestionnaire(data) {
  return request({ url: '/aem/questionnaire', method: 'put', data: data })
}

// 删除评教问卷
export function delQuestionnaire(questionnaireId) {
  return request({ url: '/aem/questionnaire/' + questionnaireId, method: 'delete' })
}
