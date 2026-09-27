import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout'

/**
 * 路由配置项说明同 Vue2 版（hidden/alwaysShow/redirect/name/meta 等）。
 * Vue Router 4 迁移要点：
 *  - new Router({...}) → createRouter({ history: createWebHistory(), ... })
 *  - 通配路由 '*' → '/:pathMatch(.*)*'（在 permission.js 动态注入）
 *  - 不再需要原型 patch push/replace 抑制重复导航报错
 */
export const constantRoutes = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      { path: '/redirect/:path(.*)', component: () => import('@/views/redirect.vue') }
    ]
  },
  { path: '/login', component: () => import('@/views/login.vue'), hidden: true },
  { path: '/404', component: () => import('@/views/error/404.vue'), hidden: true },
  { path: '/401', component: () => import('@/views/error/401.vue'), hidden: true },
  {
    path: '',
    component: Layout,
    redirect: '/index',
    children: [
      {
        path: 'index',
        component: () => import('@/views/index.vue'),
        name: 'Index',
        meta: { title: '首页', icon: 'dashboard', affix: true }
      }
    ]
  },
  { path: '/lock', component: () => import('@/views/lock.vue'), hidden: true, meta: { title: '锁定屏幕' } },
  {
    path: '/user',
    component: Layout,
    hidden: true,
    redirect: 'noRedirect',
    children: [
      {
        path: 'profile',
        component: () => import('@/views/system/user/profile/index.vue'),
        name: 'Profile',
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  }
]

export const dynamicRoutes = [
  {
    path: '/system/dict-data',
    component: Layout,
    hidden: true,
    permissions: ['system:dict:list'],
    children: [
      { path: 'index/:dictId(\\d+)', component: () => import('@/views/system/dict/data.vue'), name: 'Data', meta: { title: '字典数据', activeMenu: '/system/dict' } }
    ]
  },
  {
    path: '/monitor/job-log',
    component: Layout,
    hidden: true,
    permissions: ['monitor:job:list'],
    children: [
      { path: 'index/:jobId(\\d+)', component: () => import('@/views/monitor/job/log.vue'), name: 'JobLog', meta: { title: '调度日志', activeMenu: '/monitor/job' } }
    ]
  },
  {
    path: '/tool/gen-edit',
    component: Layout,
    hidden: true,
    permissions: ['tool:gen:edit'],
    children: [
      { path: 'index/:tableId(\\d+)', component: () => import('@/views/tool/gen/editTable.vue'), name: 'GenEdit', meta: { title: '修改生成配置', activeMenu: '/tool/gen' } }
    ]
  }
]

const router = createRouter({
  scrollBehavior: () => ({ top: 0 }),
  history: createWebHistory(),
  routes: constantRoutes
})

export default router
