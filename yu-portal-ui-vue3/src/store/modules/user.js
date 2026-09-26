import { login, logout, getInfo } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'

const user = {
  state: {
    token: getToken(),
    name: '',
    nickName: '',
    avatar: '',
    roles: [],
    permissions: [],
    userId: '',
    userCategory: ''
  },
  mutations: {
    SET_TOKEN: (state, token) => { state.token = token },
    SET_NAME: (state, name) => { state.name = name },
    SET_NICK_NAME: (state, nickName) => { state.nickName = nickName },
    SET_AVATAR: (state, avatar) => { state.avatar = avatar },
    SET_ROLES: (state, roles) => { state.roles = roles },
    SET_PERMISSIONS: (state, permissions) => { state.permissions = permissions },
    SET_USER_ID: (state, userId) => { state.userId = userId },
    SET_USER_CATEGORY: (state, userCategory) => { state.userCategory = userCategory }
  },
  actions: {
    Login({ commit, dispatch }, userInfo) {
      const { username, password, code, uuid } = userInfo
      return new Promise((resolve, reject) => {
        login(username.trim(), password, code, uuid).then(res => {
          setToken(res.token)
          commit('SET_TOKEN', res.token)
          dispatch('GetInfo').then(() => resolve()).catch(error => reject(error))
        }).catch(error => reject(error))
      })
    },
    GetInfo({ commit }) {
      return new Promise((resolve, reject) => {
        getInfo().then(res => {
          const user = res.user
          commit('SET_NAME', user.userName)
          commit('SET_NICK_NAME', user.nickName)
          commit('SET_AVATAR', user.avatar || '')
          commit('SET_USER_ID', user.userId)
          commit('SET_USER_CATEGORY', user.userCategory || '')
          if (res.roles && res.roles.length > 0) {
            commit('SET_ROLES', res.roles)
            commit('SET_PERMISSIONS', res.permissions)
          } else {
            commit('SET_ROLES', ['ROLE_DEFAULT'])
          }
          resolve(res)
        }).catch(error => reject(error))
      })
    },
    LogOut({ commit }) {
      return new Promise((resolve, reject) => {
        logout().then(() => {
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
