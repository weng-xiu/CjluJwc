import auth from '@/plugins/auth'
import router, { constantRoutes, dynamicRoutes } from '@/router'
import { getRouters } from '@/api/menu'
import Layout from '@/layout/index'
import ParentView from '@/components/ParentView'
import InnerLink from '@/layout/components/InnerLink'

// Vite 迁移：Vue2 的动态 require 改为 import.meta.glob 预索引全部业务页
// 供 loadView 按后端返回的 component 字符串（如 system/config/index）映射为异步组件
const modules = import.meta.glob('../../views/**/*.vue')

const permission = {
  state: {
    routes: [],
    addRoutes: [],
    defaultRoutes: [],
    topbarRouters: [],
    sidebarRouters: []
  },
  mutations: {
    SET_ROUTES: (state, routes) => {
      state.addRoutes = routes
      state.routes = constantRoutes.concat(routes)
    },
    SET_DEFAULT_ROUTES: (state, routes) => {
      state.defaultRoutes = constantRoutes.concat(routes)
    },
    SET_TOPBAR_ROUTES: (state, routes) => {
      state.topbarRouters = routes
    },
    SET_SIDEBAR_ROUTERS: (state, routes) => {
      state.sidebarRouters = routes
    }
  },
  actions: {
    GenerateRoutes({ commit }) {
      return new Promise(resolve => {
        getRouters().then(res => {
          const sdata = JSON.parse(JSON.stringify(res.data))
          const rdata = JSON.parse(JSON.stringify(res.data))
          const sidebarRoutes = filterAsyncRouter(sdata)
          const rewriteRoutes = filterAsyncRouter(rdata, false, true)
          const asyncRoutes = filterDynamicRoutes(dynamicRoutes)
          // vue-router 4 迁移关键修复：后端菜单存在跨子树重名（如两个 System、多个
          // Notice/Student/Schedule 等）。vue-router 3 仅告警，vue-router 4 的 addRoute
          // 会以“同名即替换”删除先注册的整棵子树，导致 /system/* 等动态页 404。
          // 这里在注册前对 rewriteRoutes 的名称做去重：保留首次出现的原名（利于 keep-alive），
          // 后续重名者追加路径无关的唯一后缀，确保每棵子树都能注册。
          const usedNames = new Set()
          constantRoutes.forEach(r => r.name && usedNames.add(r.name))
          asyncRoutes.forEach(r => r.name && usedNames.add(r.name))
          dedupeRouteNames(rewriteRoutes, usedNames)
          rewriteRoutes.push({ path: '/:pathMatch(.*)*', redirect: '/404', hidden: true })
          // Vue Router 4：addRoutes 已移除，改为逐条 addRoute
          asyncRoutes.forEach(route => router.addRoute(route))
          commit('SET_ROUTES', rewriteRoutes)
          commit('SET_SIDEBAR_ROUTERS', constantRoutes.concat(sidebarRoutes))
          commit('SET_DEFAULT_ROUTES', sidebarRoutes)
          commit('SET_TOPBAR_ROUTES', sidebarRoutes)
          resolve(rewriteRoutes)
        })
      })
    }
  }
}

function filterAsyncRouter(asyncRouterMap, lastRouter = false, type = false) {
  return asyncRouterMap.filter(route => {
    if (type && route.children) {
      route.children = filterChildren(route.children)
    }
    if (route.component) {
      if (route.component === 'Layout') {
        route.component = Layout
      } else if (route.component === 'ParentView') {
        route.component = ParentView
      } else if (route.component === 'InnerLink') {
        route.component = InnerLink
      } else {
        route.component = loadView(route.component)
      }
    }
    if (route.children != null && route.children && route.children.length) {
      route.children = filterAsyncRouter(route.children, route, type)
    } else {
      delete route['children']
      delete route['redirect']
    }
    return true
  })
}

function filterChildren(childrenMap, lastRouter = false) {
  let children = []
  childrenMap.forEach(el => {
    el.path = lastRouter ? lastRouter.path + '/' + el.path : el.path
    if (el.children && el.children.length && el.component === 'ParentView') {
      children = children.concat(filterChildren(el.children, el))
    } else {
      children.push(el)
    }
  })
  return children
}

// vue-router 4 路由名去重：递归遍历，首次出现的 name 保留（keep-alive 依赖组件名匹配），
// 后续重名者追加唯一后缀，避免 addRoute 的“同名替换”删除先注册的子树。
function dedupeRouteNames(routes, usedNames) {
  routes.forEach(route => {
    if (route.name) {
      if (usedNames.has(route.name)) {
        let i = 1
        let cand = `${route.name}__${i}`
        while (usedNames.has(cand)) { i++; cand = `${route.name}__${i}` }
        route.name = cand
      }
      usedNames.add(route.name)
    }
    if (route.children && route.children.length) {
      dedupeRouteNames(route.children, usedNames)
    }
  })
}

export function filterDynamicRoutes(routes) {
  const res = []
  routes.forEach(route => {
    if (route.permissions) {
      if (auth.hasPermiOr(route.permissions)) res.push(route)
    } else if (route.roles) {
      if (auth.hasRoleOr(route.roles)) res.push(route)
    }
  })
  return res
}

// Vue3/Vite 的视图懒加载：命中已迁移页面返回真实异步组件，
// 未迁移页面返回占位组件（保证全部后端菜单可点开且不破坏构建）
export const loadView = (view) => {
  for (const path in modules) {
    const dir = path.split('views/')[1].split('.vue')[0]
    if (dir === view) {
      return () => modules[path]()
    }
  }
  return () => import('@/views/Unmigrated.vue').then(m => ({ ...m.default, name: view }))
}

export default permission
