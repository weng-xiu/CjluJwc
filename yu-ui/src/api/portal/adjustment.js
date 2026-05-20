import request from '@/utils/request'

// 查询调停课申请列表（教师端）
export function listAdjustment(query) {
  return request({ url: '/portal/adjustment/list', method: 'get', params: query })
}

// 提交调停课申请
export function addAdjustment(data) {
  return request({ url: '/portal/adjustment', method: 'post', data: data })
}
