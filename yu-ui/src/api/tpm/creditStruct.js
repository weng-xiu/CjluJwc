import request from '@/utils/request'

// 查询学分结构列表
export function listCreditStruct(query) {
  return request({ url: '/tpm/creditStruct/list', method: 'get', params: query })
}

// 查询学分结构详细
export function getCreditStruct(structId) {
  return request({ url: '/tpm/creditStruct/' + structId, method: 'get' })
}

// 新增学分结构
export function addCreditStruct(data) {
  return request({ url: '/tpm/creditStruct', method: 'post', data: data })
}

// 修改学分结构
export function updateCreditStruct(data) {
  return request({ url: '/tpm/creditStruct', method: 'put', data: data })
}

// 删除学分结构
export function delCreditStruct(structId) {
  return request({ url: '/tpm/creditStruct/' + structId, method: 'delete' })
}
