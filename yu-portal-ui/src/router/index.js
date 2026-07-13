import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

/** 判断是否为移动设备 */
function isMobile() {
  const ua = navigator.userAgent || navigator.vendor || window.opera
  return /android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini/i.test(ua) ||
    window.innerWidth <= 768
}

export const constantRoutes = [
  // 根路径重定向：未登录用户→公开门户首页，已登录用户由路由守卫处理
  {
    path: '/',
    redirect: '/public/home'
  },
  // 公开门户路由（游客可访问，无需登录）
  {
    path: '/public',
    component: () => import('@/layout/PublicLayout'),
    children: [
      { path: '', redirect: 'home' },
      { path: 'home', component: () => import('@/views/public/home'), name: 'PublicHome', meta: { title: '首页', isPublic: true } },
      { path: 'column/:code', component: () => import('@/views/public/column'), name: 'PublicColumn', meta: { title: '栏目', isPublic: true } },
      { path: 'article/:id', component: () => import('@/views/public/article'), name: 'PublicArticle', meta: { title: '文章详情', isPublic: true } },
      { path: 'search', component: () => import('@/views/public/search'), name: 'PublicSearch', meta: { title: '搜索', isPublic: true } }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/login/index'),
    hidden: true
  },
  {
    path: '/',
    component: () => import('@/layout/PortalLayout'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        component: () => import('@/views/home/index'),
        name: 'Home',
        meta: { title: '首页' }
      }
    ]
  },
  // 门户功能路由（权限由 meta.permission 控制，对应 sys_menu.perms）
  {
    path: '/',
    component: () => import('@/layout/PortalLayout'),
    children: [
      { path: 'schedule',        component: () => import('@/views/schedule/index'),        name: 'Schedule',        meta: { title: '课表查询',     permission: 'portal:schedule:list' } },
      { path: 'grade',           component: () => import('@/views/grade/index'),           name: 'Grade',           meta: { title: '成绩查询',     permission: 'portal:grade:list' } },
      { path: 'selection',       component: () => import('@/views/selection/index'),       name: 'Selection',       meta: { title: '选课中心',     permission: 'portal:selection:list' } },
      { path: 'exam',            component: () => import('@/views/exam/index'),            name: 'Exam',            meta: { title: '考试安排',     permission: 'portal:exam:list' } },
      { path: 'evaluation',      component: () => import('@/views/evaluation/index'),      name: 'Evaluation',      meta: { title: '评教入口',     permission: 'portal:evaluation:list' } },
      { path: 'studentStatus',   component: () => import('@/views/studentStatus/index'),   name: 'StudentStatus',   meta: { title: '学籍服务',     permission: 'portal:status:list' } },
      { path: 'notice',          component: () => import('@/views/notice/index'),          name: 'Notice',          meta: { title: '教务通知',     permission: 'portal:notice:list' } },
      { path: 'teacherSchedule', component: () => import('@/views/teacherSchedule/index'), name: 'TeacherSchedule', meta: { title: '个人课表',     permission: 'portal:teacherSchedule:list' } },
      { path: 'gradeEntry',      component: () => import('@/views/gradeEntry/index'),      name: 'GradeEntry',      meta: { title: '成绩录入',     permission: 'portal:gradeEntry:list' } },
      { path: 'teachingTask',    component: () => import('@/views/teachingTask/index'),    name: 'TeachingTask',    meta: { title: '教学任务查询', permission: 'portal:teachingTask:list' } },
      { path: 'invigilation',    component: () => import('@/views/invigilation/index'),    name: 'Invigilation',    meta: { title: '监考安排',     permission: 'portal:invigilation:list' } },
      { path: 'evalResult',      component: () => import('@/views/evalResult/index'),      name: 'EvalResult',      meta: { title: '评教结果查询', permission: 'portal:evalResult:list' } },
      { path: 'adjustment',      component: () => import('@/views/adjustment/index'),      name: 'Adjustment',      meta: { title: '调停课申请',   permission: 'portal:adjustment:list' } }
    ]
  },
  // 移动端路由
  {
    path: '/mobile',
    component: () => import('@/layout/MobileLayout'),
    redirect: '/mobile',
    meta: { isMobile: true },
    children: [
      { path: '', component: () => import('@/views/mobile/index.vue'), name: 'MobileHome', meta: { title: '首页', isMobile: true } },
      { path: 'schedule', component: () => import('@/views/mobile/schedule.vue'), name: 'MobileSchedule', meta: { title: '课表', isMobile: true } },
      { path: 'grades', component: () => import('@/views/mobile/grades.vue'), name: 'MobileGrades', meta: { title: '成绩', isMobile: true } },
      { path: 'selection', component: () => import('@/views/mobile/selection.vue'), name: 'MobileSelection', meta: { title: '选课', isMobile: true } },
      { path: 'warning', component: () => import('@/views/mobile/warning.vue'), name: 'MobileWarning', meta: { title: '预警', isMobile: true } }
    ]
  }
]

const routerPush = Router.prototype.push
Router.prototype.push = function push(location) {
  return routerPush.call(this, location).catch(err => err)
}

const router = new Router({
  mode: 'history',
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes
})

/**
 * 移动端设备检测路由守卫
 * - 移动设备访问PC页面时，提示可跳转移动版（不强制）
 * - 用户选择后可通过 sessionStorage 标记偏好
 */
router.beforeEach((to, from, next) => {
  // 已选择留在PC版的不做处理
  if (sessionStorage.getItem('preferPC') === 'true') {
    next()
    return
  }

  // 移动端访问PC首页时，提示跳转
  if (isMobile() && !to.meta.isMobile && !to.meta.isPublic && to.path !== '/login' && to.path !== '/mobile') {
    // 使用 Element UI 的 MessageBox 提示（仅在首次访问时）
    if (!sessionStorage.getItem('mobileTipShown')) {
      sessionStorage.setItem('mobileTipShown', 'true')
      // 延迟到 next tick，确保页面已渲染
      setTimeout(() => {
        if (typeof window !== 'undefined' && window.confirm) {
          const jump = window.confirm('检测到您正在使用移动设备，是否切换到移动版？')
          if (jump) {
            sessionStorage.removeItem('preferPC')
            router.push('/mobile')
          }
        }
      }, 500)
    }
  }

  next()
})

export default router
