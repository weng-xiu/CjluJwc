import request from '@/utils/request'

// 查询监考分配列表
export function listInvigilation(query) {
  return request({ url: '/aem/invigilation/list', method: 'get', params: query })
}

// 查询监考分配详细
export function getInvigilation(invigilationId) {
  return request({ url: '/aem/invigilation/' + invigilationId, method: 'get' })
}

// 新增监考分配
export function addInvigilation(data) {
  return request({ url: '/aem/invigilation', method: 'post', data: data })
}

// 修改监考分配
export function updateInvigilation(data) {
  return request({ url: '/aem/invigilation', method: 'put', data: data })
}

// 删除监考分配
export function delInvigilation(invigilationId) {
  return request({ url: '/aem/invigilation/' + invigilationId, method: 'delete' })
}

// 批量导入监考安排
export function importInvigilation(data) {
  return request({ url: '/aem/invigilation/importData', method: 'post', headers: { 'Content-Type': 'multipart/form-data' }, data: data })
}
