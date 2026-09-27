import request from '@/utils/request'

// 查询接口配置列表
export function listInterface(query) {
  return request({ url: '/dis/interface/list', method: 'get', params: query })
}

// 查询接口配置详细
export function getInterface(interfaceId) {
  return request({ url: '/dis/interface/' + interfaceId, method: 'get' })
}

// 新增接口配置
export function addInterface(data) {
  return request({ url: '/dis/interface', method: 'post', data: data })
}

// 修改接口配置
export function updateInterface(data) {
  return request({ url: '/dis/interface', method: 'put', data: data })
}

// 删除接口配置
export function delInterface(interfaceId) {
  return request({ url: '/dis/interface/' + interfaceId, method: 'delete' })
}

// 导出接口配置
export function exportInterface(query) {
  return request({ url: '/dis/interface/export', method: 'post', params: query })
}
