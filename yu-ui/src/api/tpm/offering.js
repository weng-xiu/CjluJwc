import request from '@/utils/request'

// 查询开课计划列表
export function listOffering(query) {
  return request({ url: '/tpm/offering/list', method: 'get', params: query })
}

// 查询开课计划详细
export function getOffering(offeringId) {
  return request({ url: '/tpm/offering/' + offeringId, method: 'get' })
}

// 新增开课计划
export function addOffering(data) {
  return request({ url: '/tpm/offering', method: 'post', data: data })
}

// 修改开课计划
export function updateOffering(data) {
  return request({ url: '/tpm/offering', method: 'put', data: data })
}

// 删除开课计划
export function delOffering(offeringId) {
  return request({ url: '/tpm/offering/' + offeringId, method: 'delete' })
}

// 确认开课
export function confirmOffering(offeringId) {
  return request({ url: '/tpm/offering/confirm/' + offeringId, method: 'put' })
}

// 取消开课
export function cancelOffering(offeringId) {
  return request({ url: '/tpm/offering/cancel/' + offeringId, method: 'put' })
}
