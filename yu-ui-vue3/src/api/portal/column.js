import request from '@/utils/request'

// 查询栏目列表
export function listColumn(query) {
  return request({ url: '/portal/columnManage/list', method: 'get', params: query })
}

// 查询栏目详细
export function getColumn(columnId) {
  return request({ url: '/portal/columnManage/' + columnId, method: 'get' })
}

// 新增栏目
export function addColumn(data) {
  return request({ url: '/portal/columnManage', method: 'post', data: data })
}

// 修改栏目
export function updateColumn(data) {
  return request({ url: '/portal/columnManage', method: 'put', data: data })
}

// 删除栏目
export function delColumn(columnIds) {
  return request({ url: '/portal/columnManage/' + columnIds, method: 'delete' })
}
