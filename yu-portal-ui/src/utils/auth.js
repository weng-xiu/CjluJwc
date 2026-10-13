import Cookies from 'js-cookie'

const TokenKey = 'Portal-Token'

/**
 * Cookie 安全属性（V4.0 §7.3/C3，与 yu-portal-ui-vue3 同步）。
 * - sameSite=Lax：阻断跨站 POST 携带登录态，抵御 CSRF。
 * - secure：仅在 https 下置 true。开发环境为 http://localhost，若强制 Secure
 *   会让浏览器静默丢弃 Cookie、登录后立即掉线，故按协议判定而非写死。
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
