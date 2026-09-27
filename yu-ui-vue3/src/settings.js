// Vue3/Vite 迁移：module.exports → export default；process.env.VUE_APP_TITLE → import.meta.env.VITE_APP_TITLE
export default {
  title: import.meta.env.VITE_APP_TITLE,
  sideTheme: 'theme-dark',
  showSettings: true,
  navType: 1,
  tagsView: true,
  tagsViewPersist: false,
  tagsIcon: false,
  tagsViewStyle: 'card',
  fixedHeader: true,
  sidebarLogo: true,
  dynamicTitle: false,
  footerVisible: false,
  footerContent: 'Copyright © 2026 长江大学教务处'
}
