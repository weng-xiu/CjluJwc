import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

export const constantRoutes = [
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
  // 学生端
  {
    path: '/',
    component: () => import('@/layout/PortalLayout'),
    children: [
      { path: 'schedule', component: () => import('@/views/schedule/index'), name: 'Schedule', meta: { title: '课表查询', roles: ['student'] } },
      { path: 'grade', component: () => import('@/views/grade/index'), name: 'Grade', meta: { title: '成绩查询', roles: ['student'] } },
      { path: 'selection', component: () => import('@/views/selection/index'), name: 'Selection', meta: { title: '选课中心', roles: ['student'] } },
      { path: 'exam', component: () => import('@/views/exam/index'), name: 'Exam', meta: { title: '考试安排', roles: ['student'] } },
      { path: 'evaluation', component: () => import('@/views/evaluation/index'), name: 'Evaluation', meta: { title: '评教入口', roles: ['student'] } },
      { path: 'studentStatus', component: () => import('@/views/studentStatus/index'), name: 'StudentStatus', meta: { title: '学籍服务', roles: ['student'] } },
      { path: 'notice', component: () => import('@/views/notice/index'), name: 'Notice', meta: { title: '教务通知', roles: ['student'] } }
    ]
  },
  // 教师端
  {
    path: '/',
    component: () => import('@/layout/PortalLayout'),
    children: [
      { path: 'teacherSchedule', component: () => import('@/views/teacherSchedule/index'), name: 'TeacherSchedule', meta: { title: '个人课表', roles: ['teacher'] } },
      { path: 'gradeEntry', component: () => import('@/views/gradeEntry/index'), name: 'GradeEntry', meta: { title: '成绩录入', roles: ['teacher'] } },
      { path: 'teachingTask', component: () => import('@/views/teachingTask/index'), name: 'TeachingTask', meta: { title: '教学任务查询', roles: ['teacher'] } },
      { path: 'invigilation', component: () => import('@/views/invigilation/index'), name: 'Invigilation', meta: { title: '监考安排', roles: ['teacher'] } },
      { path: 'evalResult', component: () => import('@/views/evalResult/index'), name: 'EvalResult', meta: { title: '评教结果查询', roles: ['teacher'] } },
      { path: 'adjustment', component: () => import('@/views/adjustment/index'), name: 'Adjustment', meta: { title: '调停课申请', roles: ['teacher'] } }
    ]
  }
]

const routerPush = Router.prototype.push
Router.prototype.push = function push(location) {
  return routerPush.call(this, location).catch(err => err)
}

export default new Router({
  mode: 'history',
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes
})
