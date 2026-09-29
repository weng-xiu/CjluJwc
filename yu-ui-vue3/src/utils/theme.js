/**
 * U1 暗色模式工具：html.dark 类切换 + 偏好持久化（localStorage）。
 *
 * - Element Plus 暗色变量由 main.js 引入 theme-chalk/dark/css-vars.css 提供，
 *   本工具只负责在 <html> 上挂/摘 dark 类；
 * - 项目自有令牌（--dt-*）在 tokens.scss 的 html.dark 段重映射；
 * - 偏好键与布局设置（layout-setting）分开存储，避免互相覆写。
 */
const STORAGE_KEY = 'app-dark-mode'

export function isDarkEnabled() {
  return localStorage.getItem(STORAGE_KEY) === 'true'
}

function applyDark(dark) {
  document.documentElement.classList.toggle('dark', dark)
}

/** 应用启动时按已存偏好初始化（main.js mount 前调用，避免首屏闪白）。 */
export function initDarkMode() {
  applyDark(isDarkEnabled())
}

/** 切换暗色模式，返回切换后的状态。 */
export function toggleDarkMode(force) {
  const dark = typeof force === 'boolean' ? force : !isDarkEnabled()
  localStorage.setItem(STORAGE_KEY, String(dark))
  applyDark(dark)
  return dark
}
