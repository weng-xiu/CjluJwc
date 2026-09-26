import request from '@/utils/request'

export function listNotice(query) {
  return request({ url: '/portal/notice/list', method: 'get', params: query })
}

export function getNotice(noticeId) {
  return request({ url: '/portal/notice/detail', method: 'get', params: { noticeId } })
}
