import hasRole from './permission/hasRole'
import hasPermi from './permission/hasPermi'

// Vue3 迁移：Vue.directive(name, def) → app.directive(name, def)
export default {
  install(app) {
    app.directive('hasRole', hasRole)
    app.directive('hasPermi', hasPermi)
  }
}
