import request from '@/utils/request'

// 查询学位授予条件配置列表
export function listDegreeConfig(query) {
  return request({ url: '/sam/degreeConfig/list', method: 'get', params: query })
}

// 查询学位授予条件配置详细
export function getDegreeConfig(configId) {
  return request({ url: '/sam/degreeConfig/' + configId, method: 'get' })
}

// 新增学位授予条件配置
export function addDegreeConfig(data) {
  return request({ url: '/sam/degreeConfig', method: 'post', data })
}

// 修改学位授予条件配置
export function updateDegreeConfig(data) {
  return request({ url: '/sam/degreeConfig', method: 'put', data })
}

// 删除学位授予条件配置
export function delDegreeConfig(configId) {
  return request({ url: '/sam/degreeConfig/' + configId, method: 'delete' })
}

// 设为默认配置
export function setDefaultDegreeConfig(configId) {
  return request({ url: '/sam/degreeConfig/default/' + configId, method: 'put' })
}

// 查询当前生效配置
export function getEffectiveDegreeConfig() {
  return request({ url: '/sam/degreeConfig/effective', method: 'get' })
}
