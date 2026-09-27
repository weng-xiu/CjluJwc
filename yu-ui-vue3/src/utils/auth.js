import Cookies from 'js-cookie'

// 与原 yu-ui 保持一致的 Token 键名，两端登录态互不影响由端口/域隔离
const TokenKey = 'Admin-Token'

export function getToken() {
  return Cookies.get(TokenKey)
}

export function setToken(token) {
  return Cookies.set(TokenKey, token)
}

export function removeToken() {
  return Cookies.remove(TokenKey)
}
