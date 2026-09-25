import store from '@/store'

/**
 * 门户菜单配置
 * permission 对应 sys_menu 表中的 perms 字段
 * 管理员在 yu-ui 后台通过「角色管理→菜单权限」控制各角色可见菜单
 */
export const portalMenuConfig = [
  // 学生端菜单
  { permission: 'portal:schedule:list',       path: '/schedule',        title: '课表查询',     icon: 'el-icon-date',           group: '学生服务' },
  { permission: 'portal:grade:list',          path: '/grade',           title: '成绩查询',     icon: 'el-icon-document',       group: '学生服务' },
  { permission: 'portal:selection:list',      path: '/selection',       title: '选课中心',     icon: 'el-icon-edit-outline',   group: '学生服务' },
  { permission: 'portal:exam:list',           path: '/exam',            title: '考试安排',     icon: 'el-icon-tickets',        group: '学生服务' },
  { permission: 'portal:evaluation:list',     path: '/evaluation',      title: '评教入口',     icon: 'el-icon-star-on',        group: '学生服务' },
  { permission: 'portal:status:list',         path: '/studentStatus',   title: '学籍服务',     icon: 'el-icon-postcard',       group: '学生服务' },
  { permission: 'portal:graduation:list',     path: '/graduation',      title: '毕业预审',     icon: 'el-icon-s-check',        group: '学生服务' },
  { permission: 'portal:credential:list',     path: '/myCredential',    title: '我的凭证',     icon: 'el-icon-printer',        group: '学生服务' },
  { permission: 'portal:notice:list',         path: '/notice',          title: '教务通知',     icon: 'el-icon-message-solid',  group: '学生服务' },
  // 教师端菜单
  { permission: 'portal:teacherSchedule:list', path: '/teacherSchedule', title: '个人课表',     icon: 'el-icon-date',           group: '教师服务' },
  { permission: 'portal:gradeEntry:list',      path: '/gradeEntry',      title: '成绩录入',     icon: 'el-icon-edit',           group: '教师服务' },
  { permission: 'portal:teachingTask:list',    path: '/teachingTask',    title: '教学任务查询', icon: 'el-icon-notebook-2',     group: '教师服务' },
  { permission: 'portal:invigilation:list',    path: '/invigilation',    title: '监考安排',     icon: 'el-icon-view',           group: '教师服务' },
  { permission: 'portal:evalResult:list',      path: '/evalResult',      title: '评教结果查询', icon: 'el-icon-data-line',      group: '教师服务' },
  { permission: 'portal:adjustment:list',      path: '/adjustment',      title: '调停课申请',   icon: 'el-icon-refresh',        group: '教师服务' },
  { permission: 'portal:borrow:list',          path: '/borrow',          title: '教室借用申请', icon: 'el-icon-office-building', group: '教师服务' },
  { permission: 'portal:credential:list',      path: '/myCredential',    title: '我的凭证',     icon: 'el-icon-printer',        group: '教师服务' },
  { permission: 'portal:notice:list',          path: '/notice',          title: '教务通知',     icon: 'el-icon-message-solid',  group: '教师服务' }
]

const ALL_PERMISSION = '*:*:*'

/**
 * 检查当前用户是否拥有指定权限
 * @param {String} permission 权限标识，如 'portal:schedule:list'
 * @returns {Boolean}
 */
export function hasPermission(permission) {
  const permissions = store.state.user.permissions
  if (!permissions || permissions.length === 0) return false
  if (permissions.includes(ALL_PERMISSION)) return true
  return permissions.includes(permission)
}

/**
 * 检查当前用户是否拥有给定权限列表中的任意一个
 * @param {String[]} perms 权限标识数组
 * @returns {Boolean}
 */
export function hasPermissionOr(perms) {
  if (!perms || perms.length === 0) return true
  return perms.some(p => hasPermission(p))
}

/**
 * 获取当前用户可见的门户菜单列表（已按权限过滤）
 * @returns {Array} 菜单项数组
 */
export function getVisibleMenus() {
  return portalMenuConfig.filter(item => hasPermission(item.permission))
}

/**
 * 获取按分组组织的菜单结构
 * @returns {Array} [{ group: '学生服务', items: [...] }, { group: '教师服务', items: [...] }]
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
 * @returns {Array}
 */
export function getQuickEntries() {
  return getVisibleMenus().slice(0, 4)
}
