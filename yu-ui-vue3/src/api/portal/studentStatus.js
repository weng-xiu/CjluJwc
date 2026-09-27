import request from '@/utils/request'

// 查询学籍信息
export function listStudentStatus(query) {
  return request({ url: '/portal/studentStatus/list', method: 'get', params: query })
}

// 学籍异动申请
export function addStatusChange(data) {
  return request({ url: '/portal/studentStatus/change', method: 'post', data: data })
}
