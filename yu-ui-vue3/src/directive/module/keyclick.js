/**
 * U5 无障碍 · v-keyclick 指令（Vue3 yu-ui-vue3 同步版）
 *
 * 与 Vue2 (yu-ui) 版行为完全一致：把无 href 的可点 <a>/<span> 变键盘可达
 * （补 tabindex=0 + role=button + Enter/Space 触发），并接管 click，避免重复触发。
 * Vue3 自定义指令钩子名：bind→mounted，update→updated，unbind→unmounted。
 *
 * 用法：v-keyclick="() => handleViewData(scope.row)"。
 */

function apply(el, binding) {
  el.__kcFn = binding.value
  if (el.__kcInit) return
  el.__kcInit = true
  if (!el.getAttribute('role')) el.setAttribute('role', 'button')
  if (!el.hasAttribute('tabindex')) el.setAttribute('tabindex', '0')
  el.__kcClick = e => { el.__kcFn && el.__kcFn(e) }
  el.__kcKey = e => {
    if (e.key === 'Enter' || e.key === ' ' || e.key === 'Spacebar') {
      e.preventDefault()
      el.__kcFn && el.__kcFn(e)
    }
  }
  el.addEventListener('click', el.__kcClick)
  el.addEventListener('keydown', el.__kcKey)
}

function detach(el) {
  if (!el.__kcInit) return
  el.removeEventListener('click', el.__kcClick)
  el.removeEventListener('keydown', el.__kcKey)
  el.__kcInit = false
}

export default {
  mounted(el, binding) {
    apply(el, binding)
  },
  updated(el, binding) {
    apply(el, binding)
  },
  unmounted(el) {
    detach(el)
  }
}
