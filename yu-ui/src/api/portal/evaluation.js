import request from '@/utils/request'

// 查询待评教问卷（学生端）
export function listQuestionnaire(query) {
  return request({ url: '/portal/evaluation/questionnaireList', method: 'get', params: query })
}

// 提交评教
export function submitEvaluation(data) {
  return request({ url: '/portal/evaluation/submit', method: 'post', data: data })
}

// 查询评教结果（教师端）
export function listEvalResult(query) {
  return request({ url: '/portal/evaluation/resultList', method: 'get', params: query })
}
