import request from '@/utils/request'

// 查询公告列表
export function listNotice(query) {
  return request({ url: '/oa/notice/list', method: 'get', params: query })
}

// 查询公告详细
export function getNotice(noticeId) {
  return request({ url: '/oa/notice/' + noticeId, method: 'get' })
}

// 新增公告
export function addNotice(data) {
  return request({ url: '/oa/notice', method: 'post', data: data })
}

// 修改公告
export function updateNotice(data) {
  return request({ url: '/oa/notice', method: 'put', data: data })
}

// 删除公告
export function delNotice(noticeId) {
  return request({ url: '/oa/notice/' + noticeId, method: 'delete' })
}

// 导出公告
export function exportNotice(query) {
  return request({ url: '/oa/notice/export', method: 'post', params: query, responseType: 'blob' })
}

// 发布公告
export function publishNotice(noticeId) {
  return request({ url: '/oa/notice/publish/' + noticeId, method: 'post' })
}

// 撤回公告
export function revokeNotice(noticeId) {
  return request({ url: '/oa/notice/revoke/' + noticeId, method: 'post' })
}

// 已读回执
export function readNotice(noticeId) {
  return request({ url: '/oa/notice/read/' + noticeId, method: 'post' })
}
