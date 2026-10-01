import { mapState } from 'vuex'

const WIDTH = 992 // 影响布局的断点（低于此值为移动端抽屉侧栏）
const NARROW = 1366 // U2 响应式窄屏断点：≤1366 桌面端自动收起侧栏为图标栏，让出主区宽度

// Vue3 迁移：beforeDestroy → beforeUnmount；mapState 保留
export default {
  computed: {
    ...mapState({
      sidebar: (state) => state.app.sidebar,
      device: (state) => state.app.device
    })
  },
  watch: {
    $route(route) {
      if (this.device === 'mobile' && this.sidebar.opened) {
        this.$store.dispatch('app/closeSideBar', { withoutAnimation: false })
      }
    }
  },
  mounted() {
    window.addEventListener('resize', this.resizeHandler)
    const isMobile = this.isMobile()
    if (isMobile) {
      this.$store.dispatch('app/toggleDevice', 'mobile')
      this.$store.dispatch('app/closeSideBar', { withoutAnimation: true })
    } else if (this.isNarrow()) {
      // U2：窄屏笔记本首次进入自动收起侧栏
      this.$store.dispatch('app/closeSideBar', { withoutAnimation: true })
    }
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.resizeHandler)
  },
  methods: {
    isMobile() {
      const rect = document.body.getBoundingClientRect()
      return rect.width - 1 < WIDTH
    },
    isNarrow() {
      return document.documentElement.clientWidth <= NARROW
    },
    resizeHandler() {
      if (!document.hidden) {
        const isMobile = this.isMobile()
        this.$store.dispatch('app/toggleDevice', isMobile ? 'mobile' : 'desktop')
        if (isMobile) {
          this.$store.dispatch('app/closeSideBar', { withoutAnimation: true })
        } else if (this.isNarrow() && this.sidebar.opened) {
          // U2：跨入窄屏阈值时收起侧栏（不覆盖用户手动展开后的更宽视口场景）
          this.$store.dispatch('app/closeSideBar', { withoutAnimation: false })
        }
      }
    }
  }
}
