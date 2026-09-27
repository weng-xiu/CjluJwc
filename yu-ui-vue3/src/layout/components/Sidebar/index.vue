<template>
  <div :class="['sidebar-theme-wrapper', { 'has-logo': showLogo }, settings.sideTheme]">
    <logo v-if="showLogo" :collapse="isCollapse" />
    <el-scrollbar :class="settings.sideTheme">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :unique-opened="true"
        :collapse-transition="false"
        mode="vertical"
        :style="menuStyle"
      >
        <sidebar-item
          v-for="(route, index) in sidebarRouters"
          :key="route.path + index"
          :item="route"
          :base-path="route.path"
        />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script>
// Vue3 迁移：去掉 @/assets/styles/variables.scss 的 JS 导入（:export 在 Vite 不可用）；
// element-plus el-menu 弃用 background-color/text-color 属性，改用 --el-menu-* CSS 变量注入主题色。
import { mapState, mapGetters } from 'vuex'
import Logo from './Logo.vue'
import SidebarItem from './SidebarItem.vue'

export default {
  name: 'Sidebar',
  components: { SidebarItem, Logo },
  computed: {
    ...mapState(['settings']),
    ...mapGetters(['sidebarRouters', 'sidebar']),
    activeMenu() {
      const route = this.$route
      const { meta, path } = route
      if (meta && meta.activeMenu) {
        return meta.activeMenu
      }
      return path
    },
    showLogo() {
      return this.$store.state.settings.sidebarLogo
    },
    isCollapse() {
      return !this.sidebar.opened
    },
    isDark() {
      return this.settings.sideTheme === 'theme-dark'
    },
    menuStyle() {
      if (this.isDark) {
        return {
          '--el-menu-bg-color': '#1a1f2e',
          '--el-menu-text-color': '#bfcbd9',
          '--el-menu-hover-bg-color': '#141824',
          '--el-menu-active-color': this.settings.theme
        }
      }
      return {
        '--el-menu-bg-color': '#ffffff',
        '--el-menu-text-color': 'rgba(0,0,0,.70)',
        '--el-menu-hover-bg-color': '#f5f7fa',
        '--el-menu-active-color': this.settings.theme
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.sidebar-theme-wrapper {
  height: 100%;
  background-color: #1a1f2e;
}
.has-logo {
  .el-scrollbar {
    height: calc(100% - 50px);
  }
}
:deep(.el-menu) {
  border-right: none;
}
</style>
