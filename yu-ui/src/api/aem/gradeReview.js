import request from '@/utils/request'

// 查询成绩复核列表
export function listGradeReview(query) {
  return request({ url: '/aem/gradeReview/list', method: 'get', params: query })
}

// 查询成绩复核详细
export function getGradeReview(reviewId) {
  return request({ url: '/aem/gradeReview/' + reviewId, method: 'get' })
}

// 新增成绩复核
export function addGradeReview(data) {
  return request({ url: '/aem/gradeReview', method: 'post', data: data })
}

// 修改成绩复核
export function updateGradeReview(data) {
  return request({ url: '/aem/gradeReview', method: 'put', data: data })
}

// 删除成绩复核
export function delGradeReview(reviewId) {
  return request({ url: '/aem/gradeReview/' + reviewId, method: 'delete' })
}

// 审批成绩复核
export function approveReview(reviewId, approved, opinion) {
  return request({ url: '/aem/gradeReview/approve/' + reviewId, method: 'post', params: { approved, opinion } })
}
