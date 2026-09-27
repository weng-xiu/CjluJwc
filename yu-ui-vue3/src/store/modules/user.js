import store from '@/store'
import router from '@/router'
import cache from '@/plugins/cache'
import { ElMessageBox } from 'element-plus'
import { login, logout, getInfo } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { isHttp, isEmpty } from '@/utils/validate'
import defAva from '@/assets/images/profile.jpg'

const user = {
  state: {
    token: getToken(),
    id: '',
    name: '',
    nickName: '',
    avatar: '',
    roles: [],
    permissions: []
  },
  mutations: {
    SET_TOKEN: (state, token) => { state.token = token },
    SET_ID: (state, id) => { state.id = id },
    SET_NAME: (state, name) => { state.name = name },
    SET_NICK_NAME: (state, nickName) => { state.nickName = nickName },
    SET_AVATAR: (state, avatar) => { state.avatar = avatar },
    SET_ROLES: (state, roles) => { state.roles = roles },
    SET_PERMISSIONS: (state, permissions) => { state.permissions = permissions }
  },
  actions: {
    Login({ commit }, userInfo) {
      const username = userInfo.username.trim()
      const password = userInfo.password
      const code = userInfo.code
      const uuid = userInfo.uuid
      return new Promise((resolve, reject) => {
        login(username, password, code, uuid).then(res => {
          setToken(res.token)
          commit('SET_TOKEN', res.token)
          store.dispatch('lock/unlockScreen')
          resolve()
        }).catch(error => reject(error))
      })
    },
    GetInfo({ commit }) {
      return new Promise((resolve, reject) => {
        getInfo().then(res => {
          const user = res.user
          let avatar = user.avatar || ''
          if (!isHttp(avatar)) {
            // Vite 迁移：process.env.VUE_APP_BASE_API → import.meta.env.VITE_APP_BASE_API
            avatar = (isEmpty(avatar)) ? defAva : import.meta.env.VITE_APP_BASE_API + avatar
          }
          if (res.roles && res.roles.length > 0) {
            commit('SET_ROLES', res.roles)
            commit('SET_PERMISSIONS', res.permissions)
          } else {
            commit('SET_ROLES', ['ROLE_DEFAULT'])
          }
          commit('SET_ID', user.userId)
          commit('SET_NAME', user.userName)
          commit('SET_NICK_NAME', user.nickName)
          commit('SET_AVATAR', avatar)
          cache.session.set('pwrChrtype', res.pwdChrtype)
          if (res.isDefaultModifyPwd) {
            ElMessageBox.confirm('您的密码还是初始密码，请修改密码！', '安全提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }).then(() => {
              router.push({ name: 'Profile', params: { activeTab: 'resetPwd' } })
            }).catch(() => {})
          }
          if (!res.isDefaultModifyPwd && res.isPasswordExpired) {
            ElMessageBox.confirm('您的密码已过期，请尽快修改密码！', '安全提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }).then(() => {
              router.push({ name: 'Profile', params: { activeTab: 'resetPwd' } })
            }).catch(() => {})
          }
          resolve(res)
        }).catch(error => reject(error))
      })
    },
    LogOut({ commit, state }) {
      return new Promise((resolve, reject) => {
        logout(state.token).then(() => {
          commit('SET_TOKEN', '')
          commit('SET_ROLES', [])
          commit('SET_PERMISSIONS', [])
          removeToken()
          resolve()
        }).catch(error => reject(error))
      })
    },
    FedLogOut({ commit }) {
      return new Promise(resolve => {
        commit('SET_TOKEN', '')
        removeToken()
        resolve()
      })
    }
  }
}

export default user
