import request from '@/utils/request'

// 可选教室列表（正常状态，供申请表单下拉，后端已收敛权限，门户不直连管理端接口）
export function listBorrowClassrooms(query) {
  return request({ url: '/portal/borrow/classrooms', method: 'get', params: query })
}

// 我的借用申请列表（后端按登录用户强制过滤，防越权）
export function listMyBorrow(query) {
  return request({ url: '/portal/borrow/myList', method: 'get', params: query })
}

// 提交借用申请：登记 + 即时启动院系->教务处两级审批（含冲突校验，冲突时后端抛异常不落流程）
export function applyBorrow(data) {
  return request({ url: '/portal/borrow/apply', method: 'post', data: data })
}

// 撤销本人审批中的申请
export function cancelBorrow(borrowId) {
  return request({ url: '/portal/borrow/cancel/' + borrowId, method: 'put' })
}

// 提交前冲突预检（返回冲突提示文本数组，只读）
export function checkBorrowConflict(query) {
  return request({ url: '/portal/borrow/checkConflict', method: 'get', params: query })
}
