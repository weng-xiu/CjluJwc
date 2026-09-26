import request from '@/utils/request'

// 我的证书列表（供选择打印）
export function myCertificates(query) {
  return request({ url: '/portal/credential/myCertificates', method: 'get', params: query })
}

// 本人考场座位列表（准考证选择源）
export function mySeats() {
  return request({ url: '/portal/credential/mySeats', method: 'get' })
}

// 本人监考安排列表（监考通知单选择源）
export function myInvigilations() {
  return request({ url: '/portal/credential/myInvigilations', method: 'get' })
}

// 凭证预览（不落库，返回 HTML）
export function renderCredential(bizType, bizId, semesterId) {
  return request({ url: '/portal/credential/render', method: 'get', params: { bizType, bizId, semesterId } })
}

// 自助发放电子凭证（返回含编号/验证码的凭证 HTML）
export function issueCredential(bizType, bizId, semesterId) {
  return request({ url: '/portal/credential/issue', method: 'post', params: { bizType, bizId, semesterId } })
}

// 打开本人已发放凭证
export function printCredential(recordId) {
  return request({ url: '/portal/credential/print/' + recordId, method: 'get' })
}

// 本人凭证发放记录
export function myRecords(query) {
  return request({ url: '/portal/credential/myRecords', method: 'get', params: query })
}

// 公开验真（无需登录）
export function verifyCredential(serialNo, verifyCode) {
  return request({ url: '/system/credential/verify', method: 'get', params: { serialNo, verifyCode }, headers: { isToken: false } })
}
