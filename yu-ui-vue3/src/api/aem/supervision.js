import request from '@/utils/request'

// 查询督导听课列表
export function listSupervision(query) {
  return request({ url: '/aem/supervision/list', method: 'get', params: query })
}

// 查询督导听课详细
export function getSupervision(supervisionId) {
  return request({ url: '/aem/supervision/' + supervisionId, method: 'get' })
}

// 新增督导听课
export function addSupervision(data) {
  return request({ url: '/aem/supervision', method: 'post', data: data })
}

// 修改督导听课
export function updateSupervision(data) {
  return request({ url: '/aem/supervision', method: 'put', data: data })
}

// 删除督导听课
export function delSupervision(supervisionId) {
  return request({ url: '/aem/supervision/' + supervisionId, method: 'delete' })
}
