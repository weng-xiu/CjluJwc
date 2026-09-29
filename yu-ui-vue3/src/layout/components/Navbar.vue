<template>
  <div class="navbar">
    <hamburger id="hamburger-container" :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />
    <breadcrumb id="breadcrumb-container" class="breadcrumb-container" v-if="!topNav" />

    <div class="right-menu">
      <template v-if="device !== 'mobile'">
        <el-tooltip :content="isDark ? '切换亮色' : '切换暗色'" effect="dark" placement="bottom">
          <div class="right-menu-item hover-effect theme-toggle" @click="toggleTheme">
            <el-icon><Sunny v-if="isDark" /><Moon v-else /></el-icon>
          </div>
        </el-tooltip>
        <!-- U1 主题色机构自定义：调色盘弹层（本浏览器即时生效，管理员可同步机构） -->
        <el-popover placement="bottom" :width="260" trigger="click">
          <template #reference>
            <div class="right-menu-item hover-effect theme-toggle">
              <el-icon><Brush /></el-icon>
            </div>
          </template>
          <div class="theme-color-panel">
            <div class="theme-color-title">主题色</div>
            <div class="theme-color-presets">
              <span
                v-for="c in presetColors"
                :key="c.value"
                class="theme-color-dot"
                :class="{ active: themeColor === c.value }"
                :style="{ background: c.value }"
                :title="c.name"
                @click="pickThemeColor(c.value)"
              />
              <el-color-picker
                v-model="themeColor"
                size="small"
                :predefine="presetColors.map((c) => c.value)"
                @change="pickThemeColor"
              />
            </div>
            <el-checkbox v-if="isAdmin" v-model="syncOrg" class="theme-color-sync">同步到机构（全站用户生效）</el-checkbox>
            <div class="theme-color-tip">机构主题色存于系统参数 sys.ui.themeColor，登录时自动对齐</div>
          </div>
        </el-popover>
        <el-tooltip content="布局设置" effect="dark" placement="bottom">
          <div class="right-menu-item hover-effect setting-entry" @click="$emit('setLayout')">
            <el-icon><Setting /></el-icon>
          </div>
        </el-tooltip>
      </template>

      <el-dropdown class="avatar-container right-menu-item hover-effect" trigger="click">
        <div class="avatar-wrapper">
          <img :src="avatar" class="user-avatar" />
          <span class="user-name">{{ nickName }}</span>
          <el-icon class="el-icon--right"><CaretBottom /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <router-link to="/user/profile">
              <el-dropdown-item>个人中心</el-dropdown-item>
            </router-link>
            <el-dropdown-item @click="lockScreen" v-if="setting">
              <span>锁定屏幕</span>
            </el-dropdown-item>
            <el-dropdown-item divided @click="logout">
              <span>退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
// Vue3 迁移：slot="dropdown" → <template #dropdown>；el-icon-* 字体类 → @element-plus/icons-vue 组件；
// $router 仍可用（Options API 下 this.$router/$store 保留）。
import { mapState } from 'vuex'
import Breadcrumb from '@/components/Breadcrumb'
import Hamburger from '@/components/Hamburger'
import { Setting, CaretBottom, Sunny, Moon, Brush } from '@element-plus/icons-vue'
import { isDarkEnabled, toggleDarkMode } from '@/utils/theme'
import { applyThemeColor, getStoredThemeColor, saveThemeColorToConfig, DEFAULT_PRIMARY } from '@/utils/uiTheme'

export default {
  name: 'Navbar',
  components: { Breadcrumb, Hamburger, Setting, CaretBottom, Sunny, Moon, Brush },
  emits: ['setLayout'],
  data() {
    return {
      setting: true,
      isDark: isDarkEnabled(),
      themeColor: getStoredThemeColor(),
      syncOrg: false,
      // 预设色板：长江大学品牌蓝打头，其余为常见机构色
      presetColors: [
        { name: '长江蓝', value: DEFAULT_PRIMARY },
        { name: '政务红', value: '#c7000b' },
        { name: '森林绿', value: '#2e8b57' },
        { name: '沉稳紫', value: '#6a5acd' },
        { name: '暖橙', value: '#e6a23c' }
      ]
    }
  },
  computed: {
    ...mapState({
      sidebar: (state) => state.app.sidebar,
      device: (state) => state.app.device,
      avatar: (state) => state.user.avatar,
      nickName: (state) => state.user.nickName,
      roles: (state) => state.user.roles,
      routes: (state) => state.permission.routes
    }),
    topNav() {
      return this.$store.state.settings.navType === 3
    },
    isAdmin() {
      return (this.roles || []).includes('admin')
    }
  },
  methods: {
    toggleSideBar() {
      this.$store.dispatch('app/toggleSideBar')
    },
    toggleTheme() {
      this.isDark = toggleDarkMode()
    },
    pickThemeColor(color) {
      if (!color) return
      this.themeColor = color
      applyThemeColor(color)
      if (this.syncOrg && this.isAdmin) {
        saveThemeColorToConfig(color)
          .then(() => this.$modal.msgSuccess('已同步机构主题色，全站用户登录后生效'))
          .catch(() => this.$modal.msgError('同步失败，请检查参数权限（system:config:edit）'))
      }
    },
    lockScreen() {
      const currentPath = this.$route.fullPath
      this.$store.dispatch('lock/lockScreen', currentPath)
      this.$router.push('/lock')
    },
    logout() {
      this.$confirm('确定注销并退出系统吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          this.$store.dispatch('LogOut').then(() => {
            location.href = '/index'
          })
        })
        .catch(() => {})
    }
  }
}
</script>

<style lang="scss">
/* U1 主题色调色盘面板：el-popover 内容 teleport 到 body，scoped 不生效，此处全局限定类名 */
.theme-color-panel {
  .theme-color-title {
    font-size: 13px;
    color: var(--dt-text-regular);
    margin-bottom: 8px;
  }
  .theme-color-presets {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
  }
  .theme-color-dot {
    width: 20px;
    height: 20px;
    border-radius: 50%;
    cursor: pointer;
    border: 2px solid transparent;
    &.active {
      border-color: var(--dt-text-primary);
    }
  }
  .theme-color-sync {
    margin-bottom: 4px;
  }
  .theme-color-tip {
    font-size: 12px;
    color: var(--dt-text-placeholder);
    line-height: 1.4;
  }
}
</style>

<style lang="scss" scoped>
.navbar {
  height: 50px;
  overflow: hidden;
  position: relative;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

  .hamburger-container {
    line-height: 46px;
    height: 100%;
    float: left;
    cursor: pointer;
    transition: background 0.3s;
    &:hover {
      background: rgba(0, 0, 0, 0.025);
    }
  }

  .breadcrumb-container {
    float: left;
  }

  .right-menu {
    float: right;
    height: 100%;
    line-height: 50px;
    display: flex;
    align-items: center;

    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: inline-block;
      padding: 0 8px;
      height: 100%;
      font-size: 18px;
      color: #5a5e66;
      vertical-align: text-bottom;

      &.hover-effect {
        cursor: pointer;
        transition: background 0.3s;
        &:hover {
          background: rgba(0, 0, 0, 0.025);
        }
      }
    }

    .setting-entry {
      display: flex;
      align-items: center;
    }

    .avatar-container {
      margin-right: 8px;

      .avatar-wrapper {
        margin-top: 5px;
        position: relative;
        display: flex;
        align-items: center;

        .user-avatar {
          cursor: pointer;
          width: 40px;
          height: 40px;
          border-radius: 10px;
          margin-right: 8px;
        }

        .user-name {
          cursor: pointer;
          display: inline-block;
          vertical-align: text-bottom;
          position: relative;
          left: 0px;
          top: -2px;
          color: #808695;
          font-size: 14px;
        }
      }
    }
  }
}
</style>
