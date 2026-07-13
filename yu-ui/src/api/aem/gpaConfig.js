import request from '@/utils/request'

// 查询GPA算法配置列表
export function listGpaConfig(query) {
  return request({ url: '/aem/gpaConfig/list', method: 'get', params: query })
}

// 查询GPA算法配置详细
export function getGpaConfig(configId) {
  return request({ url: '/aem/gpaConfig/' + configId, method: 'get' })
}

// 新增GPA算法配置
export function addGpaConfig(data) {
  return request({ url: '/aem/gpaConfig', method: 'post', data })
}

// 修改GPA算法配置
export function updateGpaConfig(data) {
  return request({ url: '/aem/gpaConfig', method: 'put', data })
}

// 删除GPA算法配置
export function delGpaConfig(configId) {
  return request({ url: '/aem/gpaConfig/' + configId, method: 'delete' })
}

// 设为默认算法
export function setDefault(configId) {
  return request({ url: '/aem/gpaConfig/setDefault/' + configId, method: 'post' })
}

// 查询分数段映射
export function getMappings(configId) {
  return request({ url: '/aem/gpaConfig/mappings/' + configId, method: 'get' })
}

// 保存分数段映射
export function saveMappings(configId, data) {
  return request({ url: '/aem/gpaConfig/mappings/' + configId, method: 'post', data })
}

// 批量重算GPA
export function recalculateGpa(data) {
  return request({ url: '/aem/gpaConfig/recalculate', method: 'post', data })
}
