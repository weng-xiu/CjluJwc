import request from '@/utils/request'

// 查询证书补办申请列表
export function listCertReissue(query) {
  return request({ url: '/sam/certReissue/list', method: 'get', params: query })
}

// 查询补办申请详细
export function getCertReissue(applyId) {
  return request({ url: '/sam/certReissue/' + applyId, method: 'get' })
}

// 某学生可补办的原证书列表
export function certsOfStudent(studentId) {
  return request({ url: '/sam/certReissue/certsOfStudent', method: 'get', params: { studentId } })
}

// 提交补办申请
export function submitCertReissue(data) {
  return request({ url: '/sam/certReissue/submit', method: 'post', data: data })
}

// 受理通过（自动生成补办证书）
export function approveCertReissue(data) {
  return request({ url: '/sam/certReissue/approve', method: 'put', data: data })
}

// 驳回
export function rejectCertReissue(data) {
  return request({ url: '/sam/certReissue/reject', method: 'put', data: data })
}

// 删除补办申请
export function delCertReissue(applyIds) {
  return request({ url: '/sam/certReissue/' + applyIds, method: 'delete' })
}
