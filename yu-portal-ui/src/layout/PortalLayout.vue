<template>
  <div class="portal-layout">
    <!-- 顶部导航 -->
    <header class="portal-header">
      <div class="header-left">
        <h1 class="logo" @click="$router.push('/home')">师生互动服务门户</h1>
      </div>
      <div class="header-nav">
        <div class="role-tag">
          <el-tag size="small" type="info">{{ roleTagText }}</el-tag>
        </div>
        <el-dropdown trigger="click" class="user-dropdown">
          <span class="user-info">
            <i class="el-icon-user-solid"></i> {{ userName }} <i class="el-icon-arrow-down"></i>
          </span>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item icon="el-icon-switch-button" @click.native="handleLogout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
    </header>
    <div class="portal-body">
      <!-- 侧边菜单（基于后端权限动态渲染） -->
      <aside class="portal-sidebar">
        <el-menu :default-active="activeMenu" router class="sidebar-menu" @select="handleMenuSelect">
          <el-menu-item index="/home">
            <i class="el-icon-s-home"></i><span>首页</span>
          </el-menu-item>
          <template v-for="grp in groupedMenus">
            <el-menu-item-group :key="grp.group" :title="grp.group">
              <el-menu-item v-for="item in grp.items" :key="item.path" :index="item.path">
                <i :class="item.icon"></i><span>{{ item.title }}</span>
              </el-menu-item>
            </el-menu-item-group>
          </template>
        </el-menu>
      </aside>
      <!-- 内容区 -->
      <main class="portal-main">
        <el-breadcrumb separator="/" class="breadcrumb">
          <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item v-if="pageTitle && pageTitle !== '首页'">{{ pageTitle }}</el-breadcrumb-item>
        </el-breadcrumb>
        <router-view />
      </main>
    </div>
  </div>
</template>

<script>
import { getGroupedMenus } from '@/utils/permission'

export default {
  name: 'PortalLayout',
  data() {
    return {}
  },
  computed: {
    userName() { return this.$store.state.user.nickName || this.$store.state.user.name || '用户' },
    activeMenu() { return this.$route.path },
    pageTitle() { return this.$route.meta?.title || '' },
    groupedMenus() { return getGroupedMenus() },
    roleTagText() {
      const cat = this.$store.state.user.userCategory
      if (cat === 'student') return '学生端'
      if (cat === 'teacher') return '教师端'
      return '用户端'
    }
  },
  methods: {
    handleMenuSelect() {},
    async handleLogout() {
      await this.$store.dispatch('LogOut')
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.portal-layout { display: flex; flex-direction: column; height: 100vh; }
.portal-header {
  display: flex; align-items: center; justify-content: space-between;
  height: 56px; background: linear-gradient(90deg, #003366, #007ab8); color: #fff;
  padding: 0 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.15); flex-shrink: 0;
}
.header-left { display: flex; align-items: center; }
.logo { font-size: 20px; margin: 0; cursor: pointer; font-weight: 600; letter-spacing: 1px; }
.header-nav { display: flex; align-items: center; gap: 20px; }
.user-info { color: #fff; cursor: pointer; font-size: 14px; }
.portal-body { display: flex; flex: 1; overflow: hidden; }
.portal-sidebar { width: 220px; background: #f5f7fa; border-right: 1px solid #e4e7ed; overflow-y: auto; flex-shrink: 0; }
.sidebar-menu { border-right: none; }
.portal-main { flex: 1; padding: 16px 24px; overflow-y: auto; background: #f0f2f5; }
.breadcrumb { margin-bottom: 16px; }
.sidebar-menu /deep/ .el-menu-item.is-active { color: #007ab8; background-color: #edf3f9; }
.sidebar-menu /deep/ .el-submenu.is-active > .el-submenu__title { color: #007ab8; }
</style>
