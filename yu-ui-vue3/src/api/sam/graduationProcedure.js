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

// S7c：初始化离校手续与环节明细（studentId 为空则全部已毕业生）
export function initProcedure(studentId) {
  return request({ url: '/sam/graduationProcedure/init', method: 'post', params: { studentId } })
}

// S7c：某手续的环节办理明细
export function listProcedureItems(procedureId) {
  return request({ url: '/sam/graduationProcedure/items/' + procedureId, method: 'get' })
}

// S7c：人工勾选/取消某环节办理
export function toggleProcedureItem(data) {
  return request({ url: '/sam/graduationProcedure/toggle', method: 'put', data: data })
}

// S7c：自动判定
export function autoCheckProcedure(studentId) {
  return request({ url: '/sam/graduationProcedure/autoCheck', method: 'post', params: { studentId } })
}

// S7c：离校办理总览统计
export function procedureStatOverview() {
  return request({ url: '/sam/graduationProcedure/stat/overview', method: 'get' })
}
