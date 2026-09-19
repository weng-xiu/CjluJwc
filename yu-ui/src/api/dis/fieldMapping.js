import request from '@/utils/request'

// 查询字段映射列表
export function listFieldMapping(query) {
  return request({ url: '/dis/fieldMapping/list', method: 'get', params: query })
}

// 查询字段映射详细
export function getFieldMapping(mappingId) {
  return request({ url: '/dis/fieldMapping/' + mappingId, method: 'get' })
}

// 新增字段映射
export function addFieldMapping(data) {
  return request({ url: '/dis/fieldMapping', method: 'post', data: data })
}

// 修改字段映射
export function updateFieldMapping(data) {
  return request({ url: '/dis/fieldMapping', method: 'put', data: data })
}

// 删除字段映射
export function delFieldMapping(mappingId) {
  return request({ url: '/dis/fieldMapping/' + mappingId, method: 'delete' })
}
