import store from '@/store'

// Vue3 自定义指令：Vue2 的 inserted 钩子 → mounted
export default {
  mounted(el, binding) {
    const { value } = binding
    const super_admin = 'admin'
    const roles = store.getters && store.getters.roles

    if (value && value instanceof Array && value.length > 0) {
      const roleFlag = value
      const hasRole = roles.some(role => {
        return super_admin === role || roleFlag.includes(role)
      })
      if (!hasRole) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    } else {
      throw new Error('请设置角色权限标签值')
    }
  }
}
