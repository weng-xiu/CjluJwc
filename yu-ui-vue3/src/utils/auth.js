import Cookies from 'js-cookie'

// 与原 yu-ui 保持一致的 Token 键名，两端登录态互不影响由端口/域隔离
const TokenKey = 'Admin-Token'

/**
 * Cookie 安全属性（V4.0 §7.3/C3）。
 * - sameSite=Lax：阻断跨站 POST 携带登录态，抵御 CSRF；同时保留顶层 GET 跳转带 Cookie 的可用性。
 * - secure：仅在 https 下置 true。开发环境为 http://localhost，若强制 Secure 会导致 Cookie
 *   写入被浏览器静默丢弃、登录后即掉线，因此按协议判定而非写死。
 * - 需要跨子域共享时，通过部署侧改写本函数（如 domain='.cjlu.edu.cn'）。
 */
export const safeCookieOptions = {
  sameSite: 'Lax',
  secure: typeof window !== 'undefined' && window.location.protocol === 'https:'
}

export function getToken() {
  return Cookies.get(TokenKey)
}

export function setToken(token) {
  return Cookies.set(TokenKey, token, safeCookieOptions)
}

export function removeToken() {
  // js-cookie 的 remove 只按 path/domain 匹配，属性不影响删除结果
  return Cookies.remove(TokenKey)
}
