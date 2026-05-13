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
