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

// 教师本人评教分析报告（汇总 + 分课程 + 分布 + 评语词频）
export function getMyEvalReport() {
  return request({ url: '/portal/evaluation/myReport', method: 'get' })
}

// 获取某门课程的评教评语列表
export function getCourseComments(courseId) {
  return request({ url: '/portal/evaluation/comments/' + courseId, method: 'get' })
}

export function getEvaluationStatus() {
  return request({ url: '/portal/evaluation/status', method: 'get' })
}

// 移动端：可评价问卷列表（含 completed 标记，权限 portal:evaluation:list）
export function listQuestionnaireForMobile(query) {
  return request({ url: '/portal/evaluation/questionnaireList', method: 'get', params: query })
}

// 移动端：根据问卷ID获取题目列表
export function listQuestions(questionnaireId) {
  return request({ url: '/portal/evaluation/questions/' + questionnaireId, method: 'get' })
}

// 移动端：提交评教结果（权限 portal:evaluation:submit）
export function submitEvaluationResult(data) {
  return request({ url: '/portal/evaluation/submit', method: 'post', data })
}
