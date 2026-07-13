import Vue from 'vue'
import Element from 'element-ui'
import './assets/styles/element-variables.scss'
import './assets/styles/index.scss'
import App from './App'
import store from './store'
import router from './router'

Vue.use(Element, { size: 'medium' })
Vue.config.productionTip = false

router.beforeEach((to, from, next) => {
  // 公开页面无需登录，直接放行
  if (to.matched.some(record => record.meta.isPublic)) {
    next()
    return
  }
  // 登录页放行
  if (to.path === '/login') {
    next()
    return
  }

  const token = store.state.user.token
  if (token) {
    // 已登录用户访问根路径时，重定向到门户首页
    if (to.path === '/') {
      next({ path: '/home' })
      return
    }
    const checkAuth = () => {
      const userCategory = store.state.user.userCategory
      const roles = store.state.user.roles
      const permissions = store.state.user.permissions
      // 管理员跳转至管理后台
      if (userCategory === 'admin' || roles.includes('admin')) {
        store.dispatch('FedLogOut')
        window.location.href = 'http://localhost:80/login'
        return
      }
      // 路由权限校验：基于 meta.permission 检查后端分配的菜单权限
      if (to.meta && to.meta.permission) {
        const ALL_PERM = '*:*:*'
        const hasPerm = permissions.includes(ALL_PERM) || permissions.includes(to.meta.permission)
        if (!hasPerm) {
          next({ path: '/home' })
          return
        }
      }
      next()
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
  } else {
    // 未登录访问非公开页面，重定向到登录页
    next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
  }
})

new Vue({
  el: '#app',
  router,
  store,
  render: h => h(App)
})
