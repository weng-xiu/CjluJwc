import { mapState } from 'vuex'

const WIDTH = 992 // 影响布局的断点

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
    resizeHandler() {
      if (!document.hidden) {
        const isMobile = this.isMobile()
        this.$store.dispatch('app/toggleDevice', isMobile ? 'mobile' : 'desktop')
        if (isMobile) {
          this.$store.dispatch('app/closeSideBar', { withoutAnimation: true })
        }
      }
    }
  }
}
