import hasRole from './permission/hasRole'
import hasPermi from './permission/hasPermi'
import keyclick from './module/keyclick'

// Vue3 迁移：Vue.directive(name, def) → app.directive(name, def)
export default {
  install(app) {
    app.directive('hasRole', hasRole)
    app.directive('hasPermi', hasPermi)
    app.directive('keyclick', keyclick) // U5 无障碍：无 href 可点元素键盘可达（tabindex+role+Enter/Space）
  }
}
