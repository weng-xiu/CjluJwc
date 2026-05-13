import request from '@/utils/request'

export function listGraduationProcedure(query) {
  return request({ url: '/sam/graduationProcedure/list', method: 'get', params: query })
}
export function getGraduationProcedure(procedureId) {
  return request({ url: '/sam/graduationProcedure/' + procedureId, method: 'get' })
}
export function addGraduationProcedure(data) {
  return request({ url: '/sam/graduationProcedure', method: 'post', data: data })
}
export function updateGraduationProcedure(data) {
  return request({ url: '/sam/graduationProcedure', method: 'put', data: data })
}
export function delGraduationProcedure(procedureId) {
  return request({ url: '/sam/graduationProcedure/' + procedureId, method: 'delete' })
}
