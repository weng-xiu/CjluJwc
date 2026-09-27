import store from '@/store'
import router from '@/router'

export default {
  // 刷新当前tab页签
  refreshPage(obj) {
    const { path, query, matched } = router.currentRoute.value
    if (path.startsWith('/redirect/')) {
      return Promise.resolve()
    }
    if (obj === undefined) {
      matched.forEach((m) => {
        if (m.components && m.components.default && m.components.default.name) {
          if (!['Layout', 'ParentView'].includes(m.components.default.name)) {
            obj = { name: m.components.default.name, path: path, query: query }
          }
        }
      })
    }
    return store.dispatch('tagsView/delCachedView', obj).then(() => {
      const { path, query } = obj
      router.replace({
        path: '/redirect' + path,
        query: query
      })
    })
  },
  closeOpenPage(obj) {
    store.dispatch('tagsView/delView', router.currentRoute.value)
    if (obj !== undefined) {
      return router.push(obj)
    }
  },
  closePage(obj) {
    if (obj === undefined) {
      return store.dispatch('tagsView/delView', router.currentRoute.value).then(({ visitedViews }) => {
        const latestView = visitedViews.slice(-1)[0]
        if (latestView) {
          return router.push(latestView.fullPath)
        }
        return router.push('/')
      })
    }
    return store.dispatch('tagsView/delView', obj)
  },
  closeAllPage() {
    return store.dispatch('tagsView/delAllViews')
  },
  closeLeftPage(obj) {
    return store.dispatch('tagsView/delLeftTags', obj || router.currentRoute.value)
  },
  closeRightPage(obj) {
    return store.dispatch('tagsView/delRightTags', obj || router.currentRoute.value)
  },
  closeOtherPage(obj) {
    return store.dispatch('tagsView/delOthersViews', obj || router.currentRoute.value)
  },
  openPage(title, url, params) {
    const obj = { path: url, meta: { title: title } }
    store.dispatch('tagsView/addView', obj)
    return router.push({ path: url, query: params })
  },
  updatePage(obj) {
    return store.dispatch('tagsView/updateVisitedView', obj)
  }
}
