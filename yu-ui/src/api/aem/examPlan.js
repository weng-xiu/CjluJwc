import request from '@/utils/request'

// 查询考试安排列表
export function listExamPlan(query) {
  return request({ url: '/aem/examPlan/list', method: 'get', params: query })
}

// 查询考试安排详细
export function getExamPlan(examId) {
  return request({ url: '/aem/examPlan/' + examId, method: 'get' })
}

// 查询考试安排明细（含座位、监考子表）
export function getExamPlanDetail(examId) {
  return request({ url: '/aem/examPlan/detail/' + examId, method: 'get' })
}

// 新增考试安排
export function addExamPlan(data) {
  return request({ url: '/aem/examPlan', method: 'post', data: data })
}

// 修改考试安排
export function updateExamPlan(data) {
  return request({ url: '/aem/examPlan', method: 'put', data: data })
}

// 删除考试安排
export function delExamPlan(examId) {
  return request({ url: '/aem/examPlan/' + examId, method: 'delete' })
}

// 自动编排座位
export function autoArrangeSeat(examId, classroomId) {
  return request({ url: '/aem/examPlan/autoArrangeSeat/' + examId, method: 'post', params: { classroomId } })
}

// 自动派监考
export function autoDispatch(examId) {
  return request({ url: '/aem/examPlan/autoDispatch/' + examId, method: 'post' })
}
