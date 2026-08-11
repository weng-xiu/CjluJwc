import request from '@/utils/request'

export function listQuestionnaires() {
  return request({ url: '/portal/evaluation/questionnaireList', method: 'get' })
}

export function getQuestions(questionnaireId) {
  return request({ url: '/portal/evaluation/questions/' + questionnaireId, method: 'get' })
}

export function submitEvaluation(data) {
  return request({ url: '/portal/evaluation/submit', method: 'post', data })
}

export function getTeacherEvalResults() {
  return request({ url: '/portal/evaluation/teacherResults', method: 'get' })
}

// 获取某门课程的评教评语列表
export function getCourseComments(courseId) {
  return request({ url: '/portal/evaluation/comments/' + courseId, method: 'get' })
}

export function getEvaluationStatus() {
  return request({ url: '/portal/evaluation/status', method: 'get' })
}
