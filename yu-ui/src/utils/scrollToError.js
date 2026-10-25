/**
 * E5 表单校验统一 · 校验失败滚动定位 mixin（Vue2 主栈 yu-ui）。
 *
 * <p>背景：Element UI 2.x 的 el-form 不内建 `scroll-to-error`（该能力 element-plus 2.4+ 才有）。
 * 长表单校验失败时，错误项可能滚出视口，用户看不到红色提示。本 mixin 提供一个通用方法：
 * 在校验回调的失败分支调用，滚动到第一个 `.el-form-item.is-error`。</p>
 *
 * <p>用法（opt-in，逐页接入）：
 * <ul>
 *   <li>import scrollToError from '@/utils/scrollToError'，组件 mixins: [scrollToError]；</li>
 *   <li>submitForm 的 validate 回调补失败分支：
 *       `this.$refs["form"].validate(valid => { if (valid) { ... } else { this.scrollToError() } })`。</li>
 *   <li>对话框内滚动时用容器选择器：`this.scrollToError('.el-dialog')`；默认在整个表单内查找。</li>
 * </ul>
 */
export default {
  methods: {
    /** 滚动到当前页面第一个校验失败项；scopeSelector 可选，用于限定滚动查找范围 */
    scrollToError(scopeSelector) {
      this.$nextTick(() => {
        const root = scopeSelector ? document.querySelector(scopeSelector) : document
        if (!root) return
        const errEl = root.querySelector('.el-form-item.is-error')
        if (errEl && typeof errEl.scrollIntoView === 'function') {
          errEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
          // 聚焦到首个可输入控件，便于用户直接改正
          const input = errEl.querySelector('input:not([type=hidden]), textarea')
          if (input && typeof input.focus === 'function') input.focus({ preventScroll: true })
        }
      })
    }
  }
}
