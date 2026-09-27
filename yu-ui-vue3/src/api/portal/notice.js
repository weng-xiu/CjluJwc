import request from '@/utils/request'

// 查询教务通知列表（前台）
export function listNotice(query) {
  return request({ url: '/portal/notice/list', method: 'get', params: query })
}

// 查询教务通知详情
export function getNoticeDetail(query) {
  return request({ url: '/portal/notice/detail', method: 'get', params: query })
}

// 查询教务通知列表（后台管理）
export function listNoticeManage(query) {
  return request({ url: '/portal/noticeManage/list', method: 'get', params: query })
}

// 查询教务通知详细（后台管理）
export function getNoticeManage(noticeId) {
  return request({ url: '/portal/noticeManage/' + noticeId, method: 'get' })
}

// 新增教务通知
export function addNotice(data) {
  return request({ url: '/portal/noticeManage', method: 'post', data: data })
}

// 修改教务通知
export function updateNotice(data) {
  return request({ url: '/portal/noticeManage', method: 'put', data: data })
}

// 删除教务通知
export function delNotice(noticeId) {
  return request({ url: '/portal/noticeManage/' + noticeId, method: 'delete' })
}
