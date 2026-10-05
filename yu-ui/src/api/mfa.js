import request from '@/utils/request'

// 查询当前用户 MFA 状态（K3-2）
export function getMfaStatus() {
  return request({
    url: '/mfa/status',
    method: 'get'
  })
}

// 发起绑定：返回 Base32 密钥与 otpauth URI
export function bindMfa() {
  return request({
    url: '/mfa/bind',
    method: 'post'
  })
}

// 获取绑定二维码（服务端 zxing 渲染的 PNG base64）
export function getMfaQrcode() {
  return request({
    url: '/mfa/qrcode',
    method: 'get'
  })
}

// 确认绑定：校验身份验证器 6 位口令并启用
export function confirmMfa(code) {
  return request({
    url: '/mfa/confirm',
    method: 'post',
    data: { code }
  })
}

// 解绑 MFA
export function unbindMfa(code) {
  return request({
    url: '/mfa/unbind',
    method: 'post',
    data: { code }
  })
}
