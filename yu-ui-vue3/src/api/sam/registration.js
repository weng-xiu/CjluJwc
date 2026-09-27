import request from '@/utils/request'

// 查询学期注册列表
export function listRegistration(query) {
  return request({ url: '/sam/registration/list', method: 'get', params: query })
}

// 查询学期注册详细
export function getRegistration(registrationId) {
  return request({ url: '/sam/registration/' + registrationId, method: 'get' })
}

// 当前学期ID
export function getCurrentSemester() {
  return request({ url: '/sam/registration/currentSemester', method: 'get' })
}

// 报到初始化（按学期批量生成未注册记录）
export function initRegistration(semesterId) {
  return request({ url: '/sam/registration/init', method: 'post', params: { semesterId } })
}

// 批量注册办理
export function batchRegister(data) {
  return request({ url: '/sam/registration/batchRegister', method: 'put', data })
}

// 修改学期注册
export function updateRegistration(data) {
  return request({ url: '/sam/registration', method: 'put', data })
}

// 删除学期注册
export function delRegistration(registrationIds) {
  return request({ url: '/sam/registration/' + registrationIds, method: 'delete' })
}

// 注册情况总览
export function statOverview(semesterId) {
  return request({ url: '/sam/registration/stat/overview', method: 'get', params: { semesterId } })
}

// 按院系注册率统计
export function statByDept(semesterId) {
  return request({ url: '/sam/registration/stat/byDept', method: 'get', params: { semesterId } })
}
