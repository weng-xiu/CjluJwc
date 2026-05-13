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
