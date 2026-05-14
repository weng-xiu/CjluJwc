import request from '@/utils/request'

// 查询外部系统列表
export function listSystem(query) {
  return request({ url: '/dis/system/list', method: 'get', params: query })
}

// 查询外部系统详细
export function getSystem(systemId) {
  return request({ url: '/dis/system/' + systemId, method: 'get' })
}

// 新增外部系统
export function addSystem(data) {
  return request({ url: '/dis/system', method: 'post', data: data })
}

// 修改外部系统
export function updateSystem(data) {
  return request({ url: '/dis/system', method: 'put', data: data })
}

// 删除外部系统
export function delSystem(systemId) {
  return request({ url: '/dis/system/' + systemId, method: 'delete' })
}

// 导出外部系统
export function exportSystem(query) {
  return request({ url: '/dis/system/export', method: 'post', params: query })
}
