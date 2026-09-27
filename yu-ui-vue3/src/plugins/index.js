import tab from './tab'
import auth from './auth'
import cache from './cache'
import modal from './modal'
import download from './download'

// Vue3 迁移：install(Vue){ Vue.prototype.$xxx } → install(app){ app.config.globalProperties.$xxx }
export default {
  install(app) {
    app.config.globalProperties.$tab = tab
    app.config.globalProperties.$auth = auth
    app.config.globalProperties.$cache = cache
    app.config.globalProperties.$modal = modal
    app.config.globalProperties.$download = download
  }
}
