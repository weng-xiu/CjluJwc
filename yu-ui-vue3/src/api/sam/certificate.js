import request from '@/utils/request'

export function listCertificate(query) {
  return request({ url: '/sam/certificate/list', method: 'get', params: query })
}
export function getCertificate(certId) {
  return request({ url: '/sam/certificate/' + certId, method: 'get' })
}
export function addCertificate(data) {
  return request({ url: '/sam/certificate', method: 'post', data: data })
}
export function updateCertificate(data) {
  return request({ url: '/sam/certificate', method: 'put', data: data })
}
export function delCertificate(certId) {
  return request({ url: '/sam/certificate/' + certId, method: 'delete' })
}

// S7a：按规则预生成唯一证书编号
export function previewNumber(certType, year) {
  return request({ url: '/sam/certificate/previewNumber', method: 'get', params: { certType, year } })
}

// S7a：批量生成证书
export function batchGenerate(certType, gradYear) {
  return request({ url: '/sam/certificate/batchGenerate', method: 'post', params: { certType, gradYear } })
}

// 发放登记
export function issueCertificate(data) {
  return request({ url: '/sam/certificate/issue', method: 'put', data: data })
}
