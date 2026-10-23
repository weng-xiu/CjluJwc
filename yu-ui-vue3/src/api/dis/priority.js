import request from '@/utils/request'

// 查询主数据裁决优先级配置列表
export function listPriority(query) {
  return request({ url: '/dis/priority/list', method: 'get', params: query })
}

// 查询优先级配置详细
export function getPriority(priorityId) {
  return request({ url: '/dis/priority/' + priorityId, method: 'get' })
}

// 新增优先级配置
export function addPriority(data) {
  return request({ url: '/dis/priority', method: 'post', data: data })
}

// 修改优先级配置
export function updatePriority(data) {
  return request({ url: '/dis/priority', method: 'put', data: data })
}

// 删除优先级配置
export function delPriority(priorityId) {
  return request({ url: '/dis/priority/' + priorityId, method: 'delete' })
}

// 试裁决：给定多源候选，按当前优先级裁决权威版本（纯预览）
export function previewArbitration(data) {
  return request({ url: '/dis/priority/preview', method: 'post', data: data })
}
