import request from '@/utils/request'

export function listDegreeReview(query) {
  return request({ url: '/sam/degreeReview/list', method: 'get', params: query })
}
export function getDegreeReview(reviewId) {
  return request({ url: '/sam/degreeReview/' + reviewId, method: 'get' })
}
export function addDegreeReview(data) {
  return request({ url: '/sam/degreeReview', method: 'post', data: data })
}
export function updateDegreeReview(data) {
  return request({ url: '/sam/degreeReview', method: 'put', data: data })
}
export function delDegreeReview(reviewId) {
  return request({ url: '/sam/degreeReview/' + reviewId, method: 'delete' })
}
// S1：单人自动审核
export function autoReviewDegree(studentId) {
  return request({ url: '/sam/degreeReview/autoReview/' + studentId, method: 'post' })
}
// S1：批量自动审核
export function batchReviewDegree(studentIds) {
  return request({ url: '/sam/degreeReview/batchReview', method: 'post', data: studentIds })
}
