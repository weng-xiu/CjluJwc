import store from '@/store'

/**
 * 门户菜单配置
 * permission 对应 sys_menu 表中的 perms 字段
 * 管理员在 yu-ui 后台通过「角色管理→菜单权限」控制各角色可见菜单
 *
 * Vue3 迁移：icon 由 element-ui 类名字符串（el-icon-date）改为
 * @element-plus/icons-vue 的组件名（Calendar），模板内经 <el-icon><component :is="icon"/></el-icon> 渲染。
 */
export const portalMenuConfig = [
  // 学生端菜单
  { permission: 'portal:schedule:list',       path: '/schedule',        title: '课表查询',     icon: 'Calendar',      group: '学生服务' },
  { permission: 'portal:grade:list',          path: '/grade',           title: '成绩查询',     icon: 'Document',      group: '学生服务' },
  { permission: 'portal:selection:list',      path: '/selection',       title: '选课中心',     icon: 'EditPen',       group: '学生服务' },
  { permission: 'portal:exam:list',           path: '/exam',            title: '考试安排',     icon: 'Tickets',       group: '学生服务' },
  { permission: 'portal:evaluation:list',     path: '/evaluation',      title: '评教入口',     icon: 'StarFilled',    group: '学生服务' },
  { permission: 'portal:status:list',         path: '/studentStatus',   title: '学籍服务',     icon: 'Postcard',      group: '学生服务' },
  { permission: 'portal:graduation:list',     path: '/graduation',      title: '毕业预审',     icon: 'Finished',      group: '学生服务' },
  { permission: 'portal:thesis:list',         path: '/thesis',          title: '毕业论文',     icon: 'Collection',    group: '学生服务' },
  { permission: 'portal:credential:list',     path: '/myCredential',    title: '我的凭证',     icon: 'Printer',       group: '学生服务' },
  { permission: 'portal:notice:list',         path: '/notice',          title: '教务通知',     icon: 'Message',       group: '学生服务' },
  // Phase34 AI 应用试点（推荐与画像需要学籍成绩数据，仅学生角色分配）
  { permission: 'portal:ai:chat',             path: '/aiChat',          title: '智能问答',     icon: 'ChatDotRound',  group: '学生服务' },
  { permission: 'portal:ai:recommend',        path: '/aiRecommend',     title: '选课推荐',     icon: 'MagicStick',    group: '学生服务' },
  { permission: 'portal:ai:portrait',         path: '/aiProfile',       title: '学业画像',     icon: 'DataAnalysis',  group: '学生服务' },
  // 教师端菜单
  { permission: 'portal:teacherSchedule:list', path: '/teacherSchedule', title: '个人课表',     icon: 'Calendar',      group: '教师服务' },
  { permission: 'portal:gradeEntry:list',      path: '/gradeEntry',      title: '成绩录入',     icon: 'Edit',          group: '教师服务' },
  { permission: 'portal:teachingTask:list',    path: '/teachingTask',    title: '教学任务查询', icon: 'Notebook',      group: '教师服务' },
  { permission: 'portal:invigilation:list',    path: '/invigilation',    title: '监考安排',     icon: 'View',          group: '教师服务' },
  { permission: 'portal:evalResult:list',      path: '/evalResult',      title: '评教结果查询', icon: 'DataLine',      group: '教师服务' },
  { permission: 'portal:adjustment:list',      path: '/adjustment',      title: '调停课申请',   icon: 'Refresh',       group: '教师服务' },
  { permission: 'portal:borrow:list',          path: '/borrow',          title: '教室借用申请', icon: 'OfficeBuilding', group: '教师服务' },
  { permission: 'portal:thesis:audit',         path: '/thesis',          title: '毕业论文指导', icon: 'Collection',    group: '教师服务' },
  { permission: 'portal:credential:list',      path: '/myCredential',    title: '我的凭证',     icon: 'Printer',       group: '教师服务' },
  { permission: 'portal:notice:list',          path: '/notice',          title: '教务通知',     icon: 'Message',       group: '教师服务' },
  // Phase34 AI 应用试点：教师可用同一政策问答口径答疑
  { permission: 'portal:ai:chat',              path: '/aiChat',          title: '智能问答',     icon: 'ChatDotRound',  group: '教师服务' }
]

const ALL_PERMISSION = '*:*:*'

/**
 * 检查当前用户是否拥有指定权限
 */
export function hasPermission(permission) {
  const permissions = store.state.user.permissions
  if (!permissions || permissions.length === 0) return false
  if (permissions.includes(ALL_PERMISSION)) return true
  return permissions.includes(permission)
}

/**
 * 检查当前用户是否拥有给定权限列表中的任意一个
 */
export function hasPermissionOr(perms) {
  if (!perms || perms.length === 0) return true
  return perms.some(p => hasPermission(p))
}

/**
 * 获取当前用户可见的门户菜单列表（已按权限过滤）
 */
export function getVisibleMenus() {
  return portalMenuConfig.filter(item => hasPermission(item.permission))
}

/**
 * 获取按分组组织的菜单结构
 */
export function getGroupedMenus() {
  const visible = getVisibleMenus()
  const groups = []
  visible.forEach(item => {
    let g = groups.find(g => g.group === item.group)
    if (!g) {
      g = { group: item.group, items: [] }
      groups.push(g)
    }
    g.items.push(item)
  })
  return groups
}

/**
 * 获取首页快捷入口（取前4个可见菜单）
 */
export function getQuickEntries() {
  return getVisibleMenus().slice(0, 4)
}
