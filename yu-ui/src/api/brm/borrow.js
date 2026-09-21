import request from '@/utils/request'

export function listBorrow(query) {
  return request({ url: '/brm/borrow/list', method: 'get', params: query })
}

export function getBorrow(borrowId) {
  return request({ url: '/brm/borrow/' + borrowId, method: 'get' })
}

export function addBorrow(data) {
  return request({ url: '/brm/borrow', method: 'post', data: data })
}

export function updateBorrow(data) {
  return request({ url: '/brm/borrow', method: 'put', data: data })
}

export function delBorrow(borrowId) {
  return request({ url: '/brm/borrow/' + borrowId, method: 'delete' })
}

// ================= B1 借用审批流程 =================

// 提交申请，启动院系->教务处两级审批
export function submitBorrow(borrowId) {
  return request({ url: '/brm/borrowFlow/submit/' + borrowId, method: 'post' })
}

// 院系初审
export function deptApproveBorrow(borrowId, data) {
  return request({ url: '/brm/borrowFlow/deptApprove/' + borrowId, method: 'put', data })
}

// 教务处终审
export function aaApproveBorrow(borrowId, data) {
  return request({ url: '/brm/borrowFlow/aaApprove/' + borrowId, method: 'put', data })
}

// 撤销申请
export function cancelBorrow(borrowId) {
  return request({ url: '/brm/borrowFlow/cancel/' + borrowId, method: 'put' })
}

// 冲突校验（返回冲突描述数组，空表示可借）
export function checkBorrowConflict(query) {
  return request({ url: '/brm/borrowFlow/checkConflict', method: 'get', params: query })
}

// 教室占用日历
export function borrowOccupancy(query) {
  return request({ url: '/brm/borrowFlow/occupancy', method: 'get', params: query })
}

// 全流程追溯（Flowable 历史任务链）
export function borrowTrace(borrowId) {
  return request({ url: '/brm/borrowFlow/trace/' + borrowId, method: 'get' })
}
