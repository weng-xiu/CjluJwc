import request from '@/utils/request'

// 我的教室借用申请列表
export function listMyBorrow(query) {
  return request({ url: '/portal/borrow/myList', method: 'get', params: query })
}

// 可选教室列表
export function listBorrowClassrooms(query) {
  return request({ url: '/portal/borrow/classrooms', method: 'get', params: query })
}

// 提交借用申请（含冲突校验并启动两级审批流程）
export function applyBorrow(data) {
  return request({ url: '/portal/borrow/apply', method: 'post', data })
}

// 撤销申请
export function cancelBorrow(borrowId) {
  return request({ url: '/portal/borrow/cancel/' + borrowId, method: 'put' })
}

// 审批进度追溯
export function borrowTrace(borrowId) {
  return request({ url: '/portal/borrow/trace/' + borrowId, method: 'get' })
}

// 冲突预检
export function checkBorrowConflict(query) {
  return request({ url: '/portal/borrow/checkConflict', method: 'get', params: query })
}

// 教室占用查询
export function borrowOccupancy(query) {
  return request({ url: '/portal/borrow/occupancy', method: 'get', params: query })
}
