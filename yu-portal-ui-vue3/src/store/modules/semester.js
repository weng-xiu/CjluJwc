import request from '@/utils/request'

// E2 全局学期 store：学期下拉统一由 /portal/common/semesters（brm_semester 数据）供给，
// 换学期只改数据即生效、零发版。模块级缓存一次加载，多页共享；失败不置 loaded，下次进入页面自动重试。
const state = {
  list: [],
  loaded: false
}

// 进行中的请求（模块作用域，不进入响应式 state）
let pending = null

const mutations = {
  SET_LIST(state, list) {
    state.list = list
    state.loaded = true
  }
}

const actions = {
  // 已加载直接返回缓存；并发调用复用同一请求，避免多页重复拉取
  load({ state, commit }) {
    if (state.loaded) return Promise.resolve(state.list)
    if (pending) return pending
    pending = request({ url: '/portal/common/semesters', method: 'get' })
      .then(r => {
        const list = r.data || []
        commit('SET_LIST', list)
        pending = null
        return list
      })
      .catch(err => {
        pending = null
        throw err
      })
    return pending
  }
}

const getters = {
  semesterOptions: state => state.list,
  // 列表按学期ID倒序，首项即最新学期
  currentSemester: state => (state.list.length ? state.list[0] : null)
}

export default { namespaced: true, state, mutations, actions, getters }
