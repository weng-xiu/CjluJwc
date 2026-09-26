import request from '@/utils/request'

export function getStudentInfo() {
  return request({ url: '/portal/studentStatus/info', method: 'get' })
}

export function applyStatusChange(data) {
  return request({ url: '/portal/studentStatus/apply', method: 'post', data })
}

export function listStatusChanges(query) {
  return request({ url: '/portal/studentStatus/changeList', method: 'get', params: query })
}

// 异动申请审批进度追溯（Flowable 历史任务链）
export function traceStatusChange(changeId) {
  return request({ url: '/portal/studentStatus/trace/' + changeId, method: 'get' })
}

// 撤销本人审批中的异动申请
export function cancelStatusChange(changeId) {
  return request({ url: '/portal/studentStatus/cancel/' + changeId, method: 'put' })
}
