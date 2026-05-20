import request from '@/utils/request'

export function listExam(query) {
  return request({ url: '/portal/exam/list', method: 'get', params: query })
}

export function listInvigilation(query) {
  return request({ url: '/portal/exam/invigilationList', method: 'get', params: query })
}
