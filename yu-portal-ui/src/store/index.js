import Vue from 'vue'
import Vuex from 'vuex'
import user from './modules/user'
import semester from './modules/semester'

Vue.use(Vuex)

const store = new Vuex.Store({
  modules: { user, semester }
})

export default store
