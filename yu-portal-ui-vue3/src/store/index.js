import { createStore } from 'vuex'
import user from './modules/user'
import semester from './modules/semester'

// Vue3 迁移：new Vuex.Store(...)（Vuex3）→ createStore(...)（Vuex4）
const store = createStore({
  modules: { user, semester }
})

export default store
