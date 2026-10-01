/**
 * U2 响应式屏幕工具 —— 管理端 yu-ui-vue3
 *
 * 提供全局响应式 $screen，业务页可据断点隐藏表格次要列 / 次要操作，
 * 与 assets/styles/responsive.scss 的三档断点保持一致：
 *   isNarrow：≤1366 窄屏（笔记本）——隐藏次要列
 *   isWide  ：≥1920 大屏（4K）——可展示补充内容
 *   isMobile：<992 移动端抽屉
 *
 * 用法：this.$screen.isNarrow（模板内直接 $screen.isNarrow）。
 * 纯 CSS 侧亦可对 el-table-column 加 class-name="u2-hide-narrow" 实现同等效果。
 */
import { reactive } from 'vue'

const NARROW = 1366
const WIDE = 1920
const MOBILE = 992

const screen = reactive({
  width: typeof window !== 'undefined' ? window.innerWidth : 1440,
  get isNarrow() {
    return this.width <= NARROW
  },
  get isWide() {
    return this.width >= WIDE
  },
  get isMobile() {
    return this.width < MOBILE
  }
})

let installed = false
function bind() {
  if (installed || typeof window === 'undefined') return
  installed = true
  const update = () => {
    screen.width = window.innerWidth
  }
  window.addEventListener('resize', update)
  update()
}

export default {
  install(app) {
    bind()
    app.config.globalProperties.$screen = screen
  }
}

export { screen }
