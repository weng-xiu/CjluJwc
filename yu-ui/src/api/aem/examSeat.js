import request from '@/utils/request'

// 查询座位编排列表
export function listExamSeat(query) {
  return request({ url: '/aem/examSeat/list', method: 'get', params: query })
}

// 查询座位编排详细
export function getExamSeat(seatId) {
  return request({ url: '/aem/examSeat/' + seatId, method: 'get' })
}

// 新增座位编排
export function addExamSeat(data) {
  return request({ url: '/aem/examSeat', method: 'post', data: data })
}

// 修改座位编排
export function updateExamSeat(data) {
  return request({ url: '/aem/examSeat', method: 'put', data: data })
}

// 删除座位编排
export function delExamSeat(seatId) {
  return request({ url: '/aem/examSeat/' + seatId, method: 'delete' })
}

// 批量导入座位
export function importExamSeat(data) {
  return request({ url: '/aem/examSeat/importData', method: 'post', headers: { 'Content-Type': 'multipart/form-data' }, data: data })
}
