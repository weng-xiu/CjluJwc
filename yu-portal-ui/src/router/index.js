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
      { path: 'schedule', component: () => import('@/views/schedule/index'), name: 'Schedule', meta: { title: '课表查询' } },
      { path: 'grade', component: () => import('@/views/grade/index'), name: 'Grade', meta: { title: '成绩查询' } },
      { path: 'selection', component: () => import('@/views/selection/index'), name: 'Selection', meta: { title: '选课中心' } },
      { path: 'exam', component: () => import('@/views/exam/index'), name: 'Exam', meta: { title: '考试安排' } },
      { path: 'evaluation', component: () => import('@/views/evaluation/index'), name: 'Evaluation', meta: { title: '评教入口' } },
      { path: 'studentStatus', component: () => import('@/views/studentStatus/index'), name: 'StudentStatus', meta: { title: '学籍服务' } },
      { path: 'notice', component: () => import('@/views/notice/index'), name: 'Notice', meta: { title: '教务通知' } }
    ]
  },
  // 教师端
  {
    path: '/',
    component: () => import('@/layout/PortalLayout'),
    children: [
      { path: 'teacherSchedule', component: () => import('@/views/teacherSchedule/index'), name: 'TeacherSchedule', meta: { title: '个人课表' } },
      { path: 'gradeEntry', component: () => import('@/views/gradeEntry/index'), name: 'GradeEntry', meta: { title: '成绩录入' } },
      { path: 'teachingTask', component: () => import('@/views/teachingTask/index'), name: 'TeachingTask', meta: { title: '教学任务查询' } },
      { path: 'invigilation', component: () => import('@/views/invigilation/index'), name: 'Invigilation', meta: { title: '监考安排' } },
      { path: 'evalResult', component: () => import('@/views/evalResult/index'), name: 'EvalResult', meta: { title: '评教结果查询' } },
      { path: 'adjustment', component: () => import('@/views/adjustment/index'), name: 'Adjustment', meta: { title: '调停课申请' } }
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
