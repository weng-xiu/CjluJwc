/**
 * U1/V2 暗色模式工具 —— yu-portal-ui（师生门户 Vue2）
 *
 * - element-ui 2.x 无官方暗色 css-vars，组件换肤由 assets/styles/element-dark.scss 在 html.dark 下覆盖；
 * - 本工具只负责在 <html> 上挂/摘 dark 类并持久化偏好（localStorage）；
 * - 项目自有令牌（--dt-*）在 tokens.scss 的 html.dark 段重映射；
 * - 与 yu-ui / yu-ui-vue3 utils/theme.js 契约一致。
 */
const STORAGE_KEY = 'app-dark-mode'

export function isDarkEnabled() {
  return localStorage.getItem(STORAGE_KEY) === 'true'
}

function applyDark(dark) {
  document.documentElement.classList.toggle('dark', dark)
}

/** 主题变更全局通知（图表联动挂点，门户暂无 echarts，保留契约一致性）。 */
export function notifyThemeChanged() {
  window.dispatchEvent(new CustomEvent('app-theme-change'))
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
  notifyThemeChanged()
  return dark
}
