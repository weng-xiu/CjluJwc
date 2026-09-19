import request from '@/utils/request'

export function listGraduationReview(query) {
  return request({ url: '/sam/graduationReview/list', method: 'get', params: query })
}
export function getGraduationReview(reviewId) {
  return request({ url: '/sam/graduationReview/' + reviewId, method: 'get' })
}
export function addGraduationReview(data) {
  return request({ url: '/sam/graduationReview', method: 'post', data: data })
}
export function updateGraduationReview(data) {
  return request({ url: '/sam/graduationReview', method: 'put', data: data })
}
export function delGraduationReview(reviewId) {
  return request({ url: '/sam/graduationReview/' + reviewId, method: 'delete' })
}
// S1：单人自动审核
export function autoReviewGraduation(studentId) {
  return request({ url: '/sam/graduationReview/autoReview/' + studentId, method: 'post' })
}
// S1：批量自动审核
export function batchReviewGraduation(studentIds) {
  return request({ url: '/sam/graduationReview/batchReview', method: 'post', data: studentIds })
}
