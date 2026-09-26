import { createRouter, createWebHistory } from 'vue-router'
import store from '@/store'

/** 判断是否为移动设备（口径同 Vue2 版） */
function isMobile() {
  const ua = navigator.userAgent || navigator.vendor || window.opera
  return /android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini/i.test(ua) ||
    window.innerWidth <= 768
}

/**
 * P1 渐进搬迁：已迁移登录 / 门户框架 / 首页 / 20 个 PC 业务页 / 公开门户（/public/*）/ 移动端（/mobile、/m/*）。
 * 说明：守卫与鉴权口径与原 Vue2 版保持一致。
 */
export const constantRoutes = [
  // 根路径去向由守卫决定：未登录→公开门户首页；已登录→业务门户 /home
  { path: '/', redirect: () => (store.state.user.token ? '/home' : '/public/home') },
  // P1 第4批：公开门户（游客可访问，meta.isPublic 守卫直接放行）
  {
    path: '/public',
    component: () => import('@/layout/PublicLayout.vue'),
    children: [
      { path: '', redirect: 'home' },
      { path: 'home', component: () => import('@/views/public/home.vue'), name: 'PublicHome', meta: { title: '首页', isPublic: true } },
      { path: 'column/:code', component: () => import('@/views/public/column.vue'), name: 'PublicColumn', meta: { title: '栏目', isPublic: true } },
      { path: 'article/:id', component: () => import('@/views/public/article.vue'), name: 'PublicArticle', meta: { title: '文章详情', isPublic: true } },
      { path: 'search', component: () => import('@/views/public/search.vue'), name: 'PublicSearch', meta: { title: '搜索', isPublic: true } },
      { path: 'verify', component: () => import('@/views/public/verify.vue'), name: 'PublicVerify', meta: { title: '凭证验真', isPublic: true } }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    hidden: true
  },
  {
    path: '/',
    component: () => import('@/layout/PortalLayout.vue'),
    redirect: '/home',
    children: [
      { path: 'home', component: () => import('@/views/home/index.vue'), name: 'Home', meta: { title: '首页' } },
      // P1 第1批：学生/教师查询类页面（权限口径与 meta.permission 对齐后端 sys_menu.perms）
      { path: 'schedule', component: () => import('@/views/schedule/index.vue'), name: 'Schedule', meta: { title: '课表查询', permission: 'portal:schedule:list' } },
      { path: 'grade', component: () => import('@/views/grade/index.vue'), name: 'Grade', meta: { title: '成绩查询', permission: 'portal:grade:list' } },
      { path: 'exam', component: () => import('@/views/exam/index.vue'), name: 'Exam', meta: { title: '考试安排', permission: 'portal:exam:list' } },
      { path: 'notice', component: () => import('@/views/notice/index.vue'), name: 'Notice', meta: { title: '教务通知', permission: 'portal:notice:list' } },
      { path: 'teacherSchedule', component: () => import('@/views/teacherSchedule/index.vue'), name: 'TeacherSchedule', meta: { title: '个人课表', permission: 'portal:teacherSchedule:list' } },
      { path: 'teachingTask', component: () => import('@/views/teachingTask/index.vue'), name: 'TeachingTask', meta: { title: '教学任务查询', permission: 'portal:teachingTask:list' } },
      { path: 'invigilation', component: () => import('@/views/invigilation/index.vue'), name: 'Invigilation', meta: { title: '监考安排', permission: 'portal:invigilation:list' } },
      // P1 第2批：PC 业务办理页（选课/成绩录入/评教/学籍/调停课）
      { path: 'selection', component: () => import('@/views/selection/index.vue'), name: 'Selection', meta: { title: '选课中心', permission: 'portal:selection:list' } },
      { path: 'gradeEntry', component: () => import('@/views/gradeEntry/index.vue'), name: 'GradeEntry', meta: { title: '成绩录入', permission: 'portal:gradeEntry:list' } },
      { path: 'evaluation', component: () => import('@/views/evaluation/index.vue'), name: 'Evaluation', meta: { title: '评教入口', permission: 'portal:evaluation:list' } },
      { path: 'evalResult', component: () => import('@/views/evalResult/index.vue'), name: 'EvalResult', meta: { title: '评教结果查询', permission: 'portal:evalResult:list' } },
      { path: 'studentStatus', component: () => import('@/views/studentStatus/index.vue'), name: 'StudentStatus', meta: { title: '学籍服务', permission: 'portal:status:list' } },
      { path: 'adjustment', component: () => import('@/views/adjustment/index.vue'), name: 'Adjustment', meta: { title: '调停课申请', permission: 'portal:adjustment:list' } },
      // P1 第3批：PC 业务页（借用/毕业/凭证/论文/AI 三件套）
      { path: 'borrow', component: () => import('@/views/borrow/index.vue'), name: 'ClassroomBorrow', meta: { title: '教室借用申请', permission: 'portal:borrow:list' } },
      { path: 'graduation', component: () => import('@/views/graduation/index.vue'), name: 'Graduation', meta: { title: '毕业预审', permission: 'portal:graduation:list' } },
      { path: 'myCredential', component: () => import('@/views/credential/index.vue'), name: 'MyCredential', meta: { title: '我的凭证', permission: 'portal:credential:list' } },
      { path: 'thesis', component: () => import('@/views/thesis/index.vue'), name: 'PortalThesis', meta: { title: '毕业论文', permission: 'portal:thesis:list' } },
      { path: 'aiChat', component: () => import('@/views/aiChat/index.vue'), name: 'PortalAiChat', meta: { title: '智能问答', permission: 'portal:ai:chat' } },
      { path: 'aiRecommend', component: () => import('@/views/aiRecommend/index.vue'), name: 'PortalAiRecommend', meta: { title: '选课推荐', permission: 'portal:ai:recommend' } },
      { path: 'aiProfile', component: () => import('@/views/aiProfile/index.vue'), name: 'PortalAiPortrait', meta: { title: '学业画像', permission: 'portal:ai:portrait' } }
    ]
  },
  // P1 第5批：移动端主页面（/mobile，MobileLayout 承载底部 Tab）
  {
    path: '/mobile',
    component: () => import('@/layout/MobileLayout.vue'),
    meta: { isMobile: true },
    children: [
      { path: '', component: () => import('@/views/mobile/index.vue'), name: 'MobileHome', meta: { title: '首页', isMobile: true } },
      { path: 'schedule', component: () => import('@/views/mobile/schedule.vue'), name: 'MobileSchedule', meta: { title: '课表', isMobile: true } },
      { path: 'grades', component: () => import('@/views/mobile/grades.vue'), name: 'MobileGrades', meta: { title: '成绩', isMobile: true } },
      { path: 'selection', component: () => import('@/views/mobile/selection.vue'), name: 'MobileSelection', meta: { title: '选课', isMobile: true } },
      { path: 'warning', component: () => import('@/views/mobile/warning.vue'), name: 'MobileWarning', meta: { title: '预警', isMobile: true } }
    ]
  },
  // P1 第5批：移动端考核评价与办事页面（/m 前缀，复用 MobileLayout）
  {
    path: '/m',
    component: () => import('@/layout/MobileLayout.vue'),
    redirect: '/mobile',
    meta: { isMobile: true },
    children: [
      { path: 'exam', component: () => import('@/views/mobile/exam.vue'), name: 'MobileExam', meta: { title: '考试安排', isMobile: true } },
      { path: 'invigilation', component: () => import('@/views/mobile/invigilation.vue'), name: 'MobileInvigilation', meta: { title: '我的监考', isMobile: true } },
      { path: 'evaluation', component: () => import('@/views/mobile/evaluation.vue'), name: 'MobileEvaluation', meta: { title: '教学评价', isMobile: true } },
      { path: 'studentStatus', component: () => import('@/views/mobile/studentStatus.vue'), name: 'MobileStudentStatus', meta: { title: '学籍服务', isMobile: true } },
      { path: 'borrow', component: () => import('@/views/mobile/borrow.vue'), name: 'MobileBorrow', meta: { title: '教室借用', isMobile: true } },
      { path: 'adjustment', component: () => import('@/views/mobile/adjustment.vue'), name: 'MobileAdjustment', meta: { title: '调停课', isMobile: true } },
      { path: 'messages', component: () => import('@/views/mobile/messages.vue'), name: 'MobileMessages', meta: { title: '消息待办', isMobile: true } }
    ]
  }
]

const router = createRouter({
  // Vue3 迁移：mode:'history' → createWebHistory()
  history: createWebHistory(),
  scrollBehavior: () => ({ left: 0, top: 0 }),
  routes: constantRoutes
})

router.beforeEach((to, from, next) => {
  // 公开页面无需登录，直接放行（口径同 Vue2 main.js 守卫）
  if (to.matched.some(record => record.meta.isPublic)) {
    next()
    return
  }
  if (to.path === '/login') {
    next()
    return
  }
  const token = store.state.user.token
  if (token) {
    if (to.path === '/') {
      next({ path: '/home' })
      return
    }
    const checkAuth = () => {
      const userCategory = store.state.user.userCategory
      const roles = store.state.user.roles
      const permissions = store.state.user.permissions
      if (userCategory === 'admin' || roles.includes('admin')) {
        store.dispatch('FedLogOut')
        window.location.href = 'http://localhost:80/login'
        return
      }
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
    next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
  }
})

/**
 * 移动端设备检测路由守卫（口径同 Vue2 router/index.js）
 * - 移动设备访问 PC 页面时，提示可跳转移动版（不强制）
 * - 用户选择后可通过 sessionStorage 标记偏好
 */
router.beforeEach((to, from, next) => {
  if (sessionStorage.getItem('preferPC') === 'true') {
    next()
    return
  }
  if (isMobile() && !to.meta.isMobile && !to.meta.isPublic && to.path !== '/login' && to.path !== '/mobile') {
    if (!sessionStorage.getItem('mobileTipShown')) {
      sessionStorage.setItem('mobileTipShown', 'true')
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

router.afterEach((to) => {
  const title = to.meta && to.meta.title
  // 文章详情页由页面内自行设置标题（口径同 Vue2）
  if (title && to.name !== 'PublicArticle') {
    document.title = title + ' - 长江大学教务系统'
  }
})

export default router
