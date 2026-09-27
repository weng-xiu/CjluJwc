<template>
  <div class="sidebar-logo-container" :class="{ collapse: collapse }" :style="{ backgroundColor: bgColor }">
    <transition name="sidebarLogoFade">
      <router-link v-if="collapse" key="collapse" class="sidebar-logo-link" to="/">
        <img v-if="logo" :src="logo" class="sidebar-logo" />
        <h1 v-else class="sidebar-title" :style="{ color: titleColor }">{{ title }}</h1>
      </router-link>
      <router-link v-else key="expand" class="sidebar-logo-link" to="/">
        <img v-if="logo" :src="logo" class="sidebar-logo" />
        <h1 class="sidebar-title" :style="{ color: titleColor }">{{ title }}</h1>
      </router-link>
    </transition>
  </div>
</template>

<script>
// Vue3 迁移：移除对 variables.scss 的 JS 导入（:export 在 Vite 下不可用），
// 配色改为按 sideTheme 内联硬编码；标题取 import.meta.env.VITE_APP_TITLE。
import logoImg from '@/assets/logo/logo.png'

export default {
  name: 'Logo',
  props: {
    collapse: { type: Boolean, default: false }
  },
  data() {
    return {
      title: import.meta.env.VITE_APP_TITLE || '长江大学教务管理系统',
      logo: logoImg
    }
  },
  computed: {
    sideTheme() {
      return this.$store.state.settings.sideTheme
    },
    isDark() {
      return this.sideTheme === 'theme-dark'
    },
    bgColor() {
      return this.isDark ? '#1a1f2e' : '#ffffff'
    },
    titleColor() {
      return this.isDark ? '#ffffff' : '#001529'
    }
  }
}
</script>

<style lang="scss" scoped>
.sidebarLogoFade-enter-active {
  transition: opacity 1.5s;
}
.sidebarLogoFade-enter-from,
.sidebarLogoFade-leave-to {
  opacity: 0;
}
.sidebar-logo-container {
  position: relative;
  width: 100%;
  height: 50px;
  line-height: 50px;
  background: #2b2f3a;
  text-align: center;
  overflow: hidden;

  & .sidebar-logo-link {
    height: 100%;
    width: 100%;

    & .sidebar-logo {
      width: 32px;
      height: 32px;
      vertical-align: middle;
      margin-right: 12px;
    }

    & .sidebar-title {
      display: inline-block;
      margin: 0;
      color: #fff;
      font-weight: 600;
      line-height: 50px;
      font-size: 14px;
      font-family: Avenir, Helvetica Neue, Arial, Helvetica, sans-serif;
      vertical-align: middle;
    }
  }

  &.collapse {
    .sidebar-logo {
      margin-right: 0px;
    }
  }
}
</style>
