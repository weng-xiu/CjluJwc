import hasRole from './permission/hasRole'
import hasPermi from './permission/hasPermi'
import dialogDrag from './dialog/drag'
import dialogDragWidth from './dialog/dragWidth'
import dialogDragHeight from './dialog/dragHeight'
import clipboard from './module/clipboard'
import keyclick from './module/keyclick'

const install = function(Vue) {
  Vue.directive('hasRole', hasRole)
  Vue.directive('hasPermi', hasPermi)
  Vue.directive('clipboard', clipboard)
  Vue.directive('keyclick', keyclick) // U5 无障碍：无 href 可点元素键盘可达（tabindex+role+Enter/Space）
  Vue.directive('dialogDrag', dialogDrag)
  Vue.directive('dialogDragWidth', dialogDragWidth)
  Vue.directive('dialogDragHeight', dialogDragHeight)
}

if (window.Vue) {
  window['hasRole'] = hasRole
  window['hasPermi'] = hasPermi
  // 此分支仅在页面通过外链脚本引入 Vue（window.Vue 存在）时生效，故显式取全局引用
  window.Vue.use(install)
}

export default install
