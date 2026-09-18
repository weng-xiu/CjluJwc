import request from '@/utils/request'

// ==================== 课表 ====================

/** 查询我的课表 */
export function getMySchedule(params) {
  return request({ url: '/portal/schedule/myList', method: 'get', params })
}

// ==================== 成绩 ====================

/** 查询我的成绩列表 */
export function getMyGrades(params) {
  return request({ url: '/portal/grade/list', method: 'get', params })
}

/** 查询成绩统计 */
export function getGradeStatistics() {
  return request({ url: '/portal/grade/statistics', method: 'get' })
}

// ==================== 选课 ====================

/** 查询选课轮次 */
export function listSelectionRound(params) {
  return request({ url: '/portal/selection/roundList', method: 'get', params })
}

/** 查询可选课程列表 */
export function getCourseList(params) {
  return request({ url: '/portal/selection/courseList', method: 'get', params })
}

/** 选课冲突验证 */
export function validateSelection(data) {
  return request({ url: '/portal/selection/validate', method: 'post', data })
}

/** 选课（带验证） */
export function enrollCourse(data) {
  return request({ url: '/portal/selection/enrollWithValidation', method: 'post', data })
}

// ==================== 预警 ====================

/** 查询我的预警列表 */
export function getMyWarnings(params) {
  return request({ url: '/portal/warning/list', method: 'get', params })
}

/** 查询预警统计 */
export function getWarningStatistics(params) {
  return request({ url: '/portal/warning/statistics', method: 'get', params })
}
