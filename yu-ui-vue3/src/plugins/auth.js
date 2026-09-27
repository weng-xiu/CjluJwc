import store from '@/store'

export default {
  // 是否包含某个权限
  hasPermi(permission) {
    return authPermission(permission)
  },
  hasPermiOr(permissions) {
    return permissions.some(item => authPermission(item))
  },
  hasPermiAnd(permissions) {
    return permissions.every(item => authPermission(item))
  },
  hasRole(role) {
    return authRole(role)
  },
  hasRoleOr(roles) {
    return roles.some(item => authRole(item))
  },
  hasRoleAnd(roles) {
    return roles.every(item => authRole(item))
  }
}

function authPermission(permission) {
  const all_permission = '*:*:*'
  const permissions = store.getters && store.getters.permissions
  if (permission && permission.length > 0) {
    return permissions.some(v => all_permission === v || v === permission)
  }
  return false
}

function authRole(role) {
  const super_admin = 'admin'
  const roles = store.getters && store.getters.roles
  if (role && role.length > 0) {
    return roles.some(v => super_admin === v || v === role)
  }
  return false
}
