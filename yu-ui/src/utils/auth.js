import Cookies from 'js-cookie'

const TokenKey = 'Admin-Token'

/**
 * Cookie 安全属性（V4.0 §7.3/C3，与 yu-ui-vue3 同步）。
 * - sameSite=Lax：阻断跨站 POST 携带登录态，抵御 CSRF；保留顶层 GET 跳转带 Cookie 的可用性。
 * - secure：仅在 https 下置 true。开发环境为 http://localhost，若强制 Secure 会导致浏览器
 *   静默丢弃 Cookie、登录后即掉线，故按协议判定而非写死。
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
  return Cookies.remove(TokenKey)
}
