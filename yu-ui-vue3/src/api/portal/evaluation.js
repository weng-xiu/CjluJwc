import request from '@/utils/request'

// 查询待评教问卷（学生端）
export function listQuestionnaire(query) {
  return request({ url: '/portal/evaluation/questionnaireList', method: 'get', params: query })
}

// 提交评教
export function submitEvaluation(data) {
  return request({ url: '/portal/evaluation/submit', method: 'post', data: data })
}

// 查询评教结果（教师端，原始逐条记录）
export function listEvalResult(query) {
  return request({ url: '/portal/evaluation/resultList', method: 'get', params: query })
}

// 教师本人评教结果汇总：按课程聚合的真实课程名/学期名/参评人数/均分/最高最低分/满意度
export function getTeacherResults() {
  return request({ url: '/portal/evaluation/teacherResults', method: 'get' })
}

// 教师本人某门课程的真实评语列表
export function getCourseComments(courseId) {
  return request({ url: '/portal/evaluation/comments/' + courseId, method: 'get' })
}
