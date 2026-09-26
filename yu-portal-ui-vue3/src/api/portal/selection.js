import request from '@/utils/request'

export function listSelectionRound() {
  return request({ url: '/portal/selection/roundList', method: 'get' })
}

export function listCourses(query) {
  return request({ url: '/portal/selection/courseList', method: 'get', params: query })
}

export function enrollCourse(data) {
  return request({ url: '/portal/selection/enroll', method: 'post', data })
}

export function dropCourse(courseId) {
  return request({ url: '/portal/selection/drop/' + courseId, method: 'delete' })
}

export function getEnrollmentResult() {
  return request({ url: '/portal/selection/result', method: 'get' })
}
