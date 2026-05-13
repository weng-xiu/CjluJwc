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
