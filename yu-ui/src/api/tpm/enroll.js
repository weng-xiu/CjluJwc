import request from '@/utils/request'

// 查询选课名单列表
export function listEnroll(query) {
  return request({ url: '/tpm/enroll/list', method: 'get', params: query })
}

// 查询选课名单详细
export function getEnroll(enrollId) {
  return request({ url: '/tpm/enroll/' + enrollId, method: 'get' })
}

// 新增选课名单
export function addEnroll(data) {
  return request({ url: '/tpm/enroll', method: 'post', data: data })
}

// 修改选课名单
export function updateEnroll(data) {
  return request({ url: '/tpm/enroll', method: 'put', data: data })
}

// 删除选课名单
export function delEnroll(enrollId) {
  return request({ url: '/tpm/enroll/' + enrollId, method: 'delete' })
}

// 选课冲突检测
export function validateSelection(data) {
  return request({ url: '/tpm/enroll/validate', method: 'post', data: data })
}

// 带验证选课（含冲突检测+Redis并发控制）
export function enrollWithValidation(data) {
  return request({ url: '/tpm/enroll/enrollWithValidation', method: 'post', data: data })
}

// 获取替代课程建议
export function getSuggestions(courseOfferingId, query) {
  return request({ url: '/tpm/enroll/suggestions/' + courseOfferingId, method: 'get', params: query })
}

// 发起抽签（可传随机种子以复现）
export function runLottery(roundId, seed) {
  return request({ url: '/tpm/enroll/lottery/' + roundId, method: 'post', params: { seed } })
}

// T6：候补递补（按空出容量顺序递补候补队列）
export function promoteWaitlist(offeringId) {
  return request({ url: '/tpm/enroll/promoteWaitlist/' + offeringId, method: 'post' })
}

// 学生退课
export function dropCourse(enrollId) {
  return request({ url: '/tpm/enroll/drop/' + enrollId, method: 'post' })
}
