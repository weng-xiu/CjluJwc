import request from '@/utils/request'

// 查询调停课申请列表（教师端）
export function listAdjustment(query) {
  return request({ url: '/portal/adjustment/list', method: 'get', params: query })
}

// 提交调停课申请
export function addAdjustment(data) {
  return request({ url: '/portal/adjustment/apply', method: 'post', data: data })
}

// 本人任课排课列表（申请源，后端按登录教师强制过滤）
export function listMyAdjustSources(query) {
  return request({ url: '/portal/adjustment/mySchedules', method: 'get', params: query })
}

// 撤销本人待审的调停课申请
export function cancelAdjustment(adjustId) {
  return request({ url: '/portal/adjustment/cancel/' + adjustId, method: 'put' })
}
