<template>
  <div class="navbar">
    <hamburger id="hamburger-container" :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />
    <breadcrumb id="breadcrumb-container" class="breadcrumb-container" v-if="!topNav" />

    <div class="right-menu">
      <template v-if="device !== 'mobile'">
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
import { Setting, CaretBottom } from '@element-plus/icons-vue'

export default {
  name: 'Navbar',
  components: { Breadcrumb, Hamburger, Setting, CaretBottom },
  emits: ['setLayout'],
  data() {
    return {
      setting: true
    }
  },
  computed: {
    ...mapState({
      sidebar: (state) => state.app.sidebar,
      device: (state) => state.app.device,
      avatar: (state) => state.user.avatar,
      nickName: (state) => state.user.nickName,
      routes: (state) => state.permission.routes
    }),
    topNav() {
      return this.$store.state.settings.navType === 3
    }
  },
  methods: {
    toggleSideBar() {
      this.$store.dispatch('app/toggleSideBar')
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
