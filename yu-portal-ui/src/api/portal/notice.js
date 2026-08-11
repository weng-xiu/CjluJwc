import request from '@/utils/request'

export function listNotice(query) {
  return request({ url: '/portal/notice/list', method: 'get', params: query })
}

export function getNotice(noticeId) {
  return request({ url: '/portal/notice/detail', method: 'get', params: { noticeId } })
}

export function addNotice(data) {
  return request({ url: '/portal/noticeManage', method: 'post', data })
}

export function updateNotice(data) {
  return request({ url: '/portal/noticeManage', method: 'put', data })
}

export function deleteNotice(noticeIds) {
  return request({ url: '/portal/noticeManage/' + noticeIds, method: 'delete' })
}
