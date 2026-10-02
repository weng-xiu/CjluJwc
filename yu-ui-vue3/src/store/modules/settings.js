import defaultSettings from '@/settings'
import { useDynamicTitle } from '@/utils/dynamicTitle'

const { sideTheme, showSettings, navType, tagsView, tagsViewPersist, tagsIcon, tagsViewStyle, fixedHeader, sidebarLogo, dynamicTitle, footerVisible, footerContent } = defaultSettings

const storageSetting = JSON.parse(localStorage.getItem('layout-setting')) || ''
const state = {
  title: '',
  theme: storageSetting.theme || '#007ab8',
  sideTheme: storageSetting.sideTheme || sideTheme,
  showSettings: showSettings,
  navType: storageSetting.navType === undefined ? navType : storageSetting.navType,
  tagsView: storageSetting.tagsView === undefined ? tagsView : storageSetting.tagsView,
  tagsViewPersist: storageSetting.tagsViewPersist === undefined ? tagsViewPersist : storageSetting.tagsViewPersist,
  tagsIcon: storageSetting.tagsIcon === undefined ? tagsIcon : storageSetting.tagsIcon,
  tagsViewStyle: storageSetting.tagsViewStyle === undefined ? tagsViewStyle : storageSetting.tagsViewStyle,
  fixedHeader: storageSetting.fixedHeader === undefined ? fixedHeader : storageSetting.fixedHeader,
  sidebarLogo: storageSetting.sidebarLogo === undefined ? sidebarLogo : storageSetting.sidebarLogo,
  dynamicTitle: storageSetting.dynamicTitle === undefined ? dynamicTitle : storageSetting.dynamicTitle,
  footerVisible: storageSetting.footerVisible === undefined ? footerVisible : storageSetting.footerVisible,
  footerContent: footerContent
}
const mutations = {
  CHANGE_SETTING: (state, { key, value }) => {
    // 用原型 call 而非 state.hasOwnProperty：Vuex state 为响应式代理，
    // 直接调用继承方法存在被自身 key 遮蔽的风险（ESLint no-prototype-builtins）
    if (Object.prototype.hasOwnProperty.call(state, key)) {
      state[key] = value
    }
  },
  SET_TITLE: (state, title) => {
    state.title = title
  }
}

const actions = {
  // 修改布局设置
  changeSetting({ commit }, data) {
    commit('CHANGE_SETTING', data)
  },
  // 设置网页标题
  setTitle({ commit }, title) {
    commit('SET_TITLE', title)
    useDynamicTitle()
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}

