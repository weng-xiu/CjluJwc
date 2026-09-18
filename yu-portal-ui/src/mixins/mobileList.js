/**
 * 移动端列表页公共逻辑：无限滚动加载 + 下拉刷新
 *
 * 使用方要求：
 * 1. 组件混入本 mixin 后，实现 fetchList() 方法，返回 Promise，
 *    resolve 形如 { rows: [...], total: number } 的结构（total 可省略）
 * 2. 模板根节点绑定 @touchstart/@touchmove/@touchend 三个事件
 * 3. 可选覆盖 data：queryParams（需含 pageNum/pageSize）
 */
export default {
  data() {
    return {
      loading: false,
      refreshing: false,
      list: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10 },
      // 下拉刷新
      pullDistance: 0,
      triggerDistance: 50,
      startY: 0,
      pulling: false,
      scrollEl: null
    }
  },
  computed: {
    finished() {
      return this.list.length >= this.total && this.list.length > 0
    },
    refreshText() {
      if (this.refreshing) return '正在刷新...'
      if (this.pullDistance >= this.triggerDistance) return '松开立即刷新'
      return '下拉刷新'
    }
  },
  mounted() {
    this.getList()
    this.bindScroll()
  },
  activated() {
    this.bindScroll()
  },
  beforeDestroy() {
    this.unbindScroll()
  },
  methods: {
    /** 分页加载：调用页面实现的 fetchList，合并结果并处理 total */
    getList() {
      this.loading = true
      return Promise.resolve(this.fetchList()).then(r => {
        const res = r || {}
        const rows = res.rows || res.data || []
        if (this.queryParams.pageNum === 1) {
          this.list = rows
        } else {
          this.list = this.list.concat(rows)
        }
        this.total = res.total || 0
        // 后端未返回 total 时，按本页条数判断是否结束
        if (!res.total && rows.length < this.queryParams.pageSize) {
          this.total = this.list.length
        }
      }).finally(() => {
        this.loading = false
        this.refreshing = false
        this.pullDistance = 0
      })
    },
    loadMore() {
      if (this.loading || this.finished) return
      this.queryParams.pageNum++
      this.getList()
    },
    refresh() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置查询条件后重新加载（筛选切换时调用） */
    resetQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    // 滚动容器查找（MobileLayout 的 .mobile-content）
    getScrollContainer() {
      let node = this.$el
      while (node && node.tagName !== 'BODY') {
        if (node.classList && node.classList.contains('mobile-content')) return node
        node = node.parentNode
      }
      return window
    },
    bindScroll() {
      this.scrollEl = this.getScrollContainer()
      if (this.scrollEl) {
        this.scrollEl.addEventListener('scroll', this.onScroll, { passive: true })
      }
    },
    unbindScroll() {
      if (this.scrollEl) {
        this.scrollEl.removeEventListener('scroll', this.onScroll)
        this.scrollEl = null
      }
    },
    onScroll() {
      if (this.loading || this.finished) return
      const el = this.scrollEl
      let scrollTop, clientHeight, scrollHeight
      if (el === window) {
        scrollTop = window.pageYOffset || document.documentElement.scrollTop
        clientHeight = window.innerHeight
        scrollHeight = document.documentElement.scrollHeight
      } else {
        scrollTop = el.scrollTop
        clientHeight = el.clientHeight
        scrollHeight = el.scrollHeight
      }
      // 距底部 80px 触发加载
      if (scrollTop + clientHeight >= scrollHeight - 80) {
        this.loadMore()
      }
    },
    // 下拉刷新
    onTouchStart(e) {
      const el = this.scrollEl
      const top = el === window ? (window.pageYOffset || document.documentElement.scrollTop) : el.scrollTop
      if (top <= 0 && !this.refreshing) {
        this.startY = e.touches[0].clientY
        this.pulling = true
      }
    },
    onTouchMove(e) {
      if (!this.pulling || this.refreshing) return
      const delta = e.touches[0].clientY - this.startY
      if (delta > 0) {
        // 阻尼效果
        this.pullDistance = Math.min(delta * 0.5, 80)
      }
    },
    onTouchEnd() {
      if (!this.pulling) return
      this.pulling = false
      if (this.pullDistance >= this.triggerDistance) {
        this.refreshing = true
        this.pullDistance = 40
        this.refresh()
      } else {
        this.pullDistance = 0
      }
    }
  }
}
