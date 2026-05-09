import request from '@/utils/request'

// 查询调停课管理列表
export function listAdjust(query) {
  return request({ url: '/tpm/adjust/list', method: 'get', params: query })
}

// 查询调停课管理详细
export function getAdjust(adjustId) {
  return request({ url: '/tpm/adjust/' + adjustId, method: 'get' })
}

// 新增调停课管理
export function addAdjust(data) {
  return request({ url: '/tpm/adjust', method: 'post', data: data })
}

// 修改调停课管理
export function updateAdjust(data) {
  return request({ url: '/tpm/adjust', method: 'put', data: data })
}

// 删除调停课管理
export function delAdjust(adjustId) {
  return request({ url: '/tpm/adjust/' + adjustId, method: 'delete' })
}
