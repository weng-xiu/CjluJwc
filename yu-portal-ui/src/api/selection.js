import request from '@/utils/request'

/**
 * 选课冲突验证
 * @param {Object} data - { courseOfferingId }
 */
export function validateSelection(data) {
  return request({ url: '/tpm/enroll/validate', method: 'post', data })
}

/**
 * 获取替代课程建议
 * @param {Number|String} courseOfferingId
 */
export function getAlternatives(courseOfferingId) {
  return request({ url: '/tpm/enroll/suggestions/' + courseOfferingId, method: 'get' })
}

/**
 * 带冲突验证的选课
 * @param {Object} data - { courseOfferingId }
 */
export function enrollWithValidation(data) {
  return request({ url: '/tpm/enroll/enrollWithValidation', method: 'post', data })
}
