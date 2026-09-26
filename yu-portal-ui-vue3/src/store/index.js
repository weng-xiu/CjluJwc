import { createStore } from 'vuex'
import user from './modules/user'

// Vue3 迁移：new Vuex.Store(...)（Vuex3）→ createStore(...)（Vuex4）
const store = createStore({
  modules: { user }
})

export default store
