<template>
  <div class="mobile-layout">
    <!-- 顶部标题栏 -->
    <header class="mobile-header">
      <div class="header-left" v-if="showBack" @click="goBack">
        <i class="el-icon-arrow-left"></i>
      </div>
      <div class="header-title">{{ pageTitle }}</div>
      <div class="header-right">
        <slot name="header-right"></slot>
      </div>
    </header>

    <!-- 内容区 -->
    <main class="mobile-content">
      <router-view />
    </main>

    <!-- 底部导航栏 -->
    <nav class="mobile-tabbar">
      <div
        v-for="tab in tabs"
        :key="tab.path"
        class="tab-item"
        :class="{ active: isActive(tab.path) }"
        @click="switchTab(tab.path)"
      >
        <div class="tab-icon">
          <i :class="tab.icon"></i>
          <span v-if="tab.badge > 0" class="tab-badge">{{ tab.badge > 99 ? '99+' : tab.badge }}</span>
        </div>
        <span class="tab-label">{{ tab.label }}</span>
      </div>
    </nav>
  </div>
</template>

<script>
export default {
  name: 'MobileLayout',
  data() {
    return {
      warningCount: 0,
      tabs: [
        { path: '/mobile', label: '首页', icon: 'el-icon-s-home' },
        { path: '/mobile/schedule', label: '课表', icon: 'el-icon-date' },
        { path: '/mobile/selection', label: '选课', icon: 'el-icon-edit-outline' },
        { path: '/mobile/grades', label: '成绩', icon: 'el-icon-document' },
        { path: '/mobile/warning', label: '预警', icon: 'el-icon-warning' }
      ]
    }
  },
  computed: {
    pageTitle() {
      return this.$route.meta?.title || '师生门户'
    },
    showBack() {
      // 首页不显示返回按钮
      return this.$route.path !== '/mobile' && this.$route.path !== '/mobile/'
    },
    // 动态更新tabs中的badge
    dynamicTabs() {
      return this.tabs.map(t => {
        if (t.path === '/mobile/warning') {
          return { ...t, badge: this.warningCount }
        }
        return { ...t, badge: 0 }
      })
    }
  },
  mounted() {
    this.loadWarningCount()
  },
  methods: {
    isActive(path) {
      if (path === '/mobile') {
        return this.$route.path === '/mobile' || this.$route.path === '/mobile/'
      }
      return this.$route.path.startsWith(path)
    },
    switchTab(path) {
      if (this.$route.path !== path) {
        this.$router.push(path)
      }
    },
    goBack() {
      this.$router.go(-1)
    },
    loadWarningCount() {
      // 尝试从后端加载未读预警数量
      const { getWarningStatistics } = require('@/api/mobile')
      getWarningStatistics().then(r => {
        this.warningCount = r.data?.totalCount || r.data?.unreadCount || 0
        // 更新tabs
        this.tabs = this.tabs.map(t => {
          if (t.path === '/mobile/warning') {
            return { ...t, badge: this.warningCount }
          }
          return t
        })
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.mobile-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-width: 768px;
  margin: 0 auto;
  background: #f5f7fa;
}

/* 顶部标题栏 */
.mobile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  background: linear-gradient(90deg, #1a5276, #2e86c1);
  color: #fff;
  padding: 0 16px;
  flex-shrink: 0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.15);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-left {
  width: 40px;
  display: flex;
  align-items: center;
  cursor: pointer;
  font-size: 18px;
}
.header-title {
  font-size: 17px;
  font-weight: 600;
  text-align: center;
  flex: 1;
}
.header-right {
  width: 40px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

/* 内容区 */
.mobile-content {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding-bottom: 56px; /* 为底部导航留出空间 */
}

/* 底部导航栏 */
.mobile-tabbar {
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 52px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  flex-shrink: 0;
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 768px;
  z-index: 100;
  padding-bottom: env(safe-area-inset-bottom, 0);
}
.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  cursor: pointer;
  color: #999;
  font-size: 12px;
  transition: color 0.2s;
  padding-top: 4px;
}
.tab-item.active {
  color: #2e86c1;
}
.tab-icon {
  font-size: 20px;
  margin-bottom: 2px;
  position: relative;
  line-height: 1;
}
.tab-badge {
  position: absolute;
  top: -6px;
  right: -10px;
  background: #e74c3c;
  color: #fff;
  font-size: 10px;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  border-radius: 8px;
  padding: 0 4px;
  text-align: center;
}
.tab-label {
  font-size: 11px;
  line-height: 1;
}
</style>
