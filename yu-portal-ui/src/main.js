import Vue from 'vue'
import Element from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import App from './App'
import store from './store'
import router from './router'

Vue.use(Element, { size: 'medium' })
Vue.config.productionTip = false

router.beforeEach((to, from, next) => {
  const token = store.state.user.token
  if (token) {
    if (to.path === '/login') {
      next({ path: '/' })
    } else {
      const checkAuth = () => {
        const userCategory = store.state.user.userCategory
        const roles = store.state.user.roles
        // 管理员跳转至管理后台
        if (userCategory === 'admin' || roles.includes('admin')) {
          store.dispatch('FedLogOut')
          window.location.href = 'http://localhost:80/login'
          return
        }
        // 非法角色兜底
        if (userCategory !== 'student' && userCategory !== 'teacher') {
          next({ path: '/home' })
          return
        }
        // 路由角色权限校验
        if (to.meta.roles && !to.meta.roles.includes(userCategory)) {
          next({ path: '/home' })
        } else {
          next()
        }
      }
      if (store.state.user.roles.length === 0) {
        store.dispatch('GetInfo').then(() => {
          checkAuth()
        }).catch(() => {
          store.dispatch('FedLogOut')
          next({ path: '/login', query: { redirect: to.fullPath } })
        })
      } else {
        checkAuth()
      }
    }
  } else {
    if (to.path === '/login') {
      next()
    } else {
      next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
    }
  }
})

new Vue({
  el: '#app',
  router,
  store,
  render: h => h(App)
})
