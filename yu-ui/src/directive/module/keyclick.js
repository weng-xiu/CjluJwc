/**
 * U5 无障碍 · v-keyclick 指令（Vue2 yu-ui）
 *
 * 用途：把 RuoYi 常见的「无 href 的可点 <a>/<span>」（如列表里点开详情抽屉的 link-type 文本）
 * 变为键盘可达——补 tabindex=0 使其可 Tab 聚焦、role=button 使读屏播报为按钮，
 * 并绑定 Enter / Space 键等价触发原点击逻辑（WCAG 2.1.1 键盘）。
 *
 * 用法：v-keyclick="() => handleViewData(scope.row)"（绑定值为回调函数，事件对象作参传入）。
 * 指令同时接管 click，故使用本指令的元素无需再写 @click，避免重复触发。
 */

function apply(el, binding) {
  // 始终刷新最新回调（列表重渲染时 row 变化）
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
  bind(el, binding) {
    apply(el, binding)
  },
  update(el, binding) {
    apply(el, binding)
  },
  unbind(el) {
    detach(el)
  }
}
