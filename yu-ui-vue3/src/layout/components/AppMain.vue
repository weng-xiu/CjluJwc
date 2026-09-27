<template>
  <section class="app-main" :style="style">
    <router-view v-slot="{ Component, route }">
      <transition name="fade-transform" mode="out-in">
        <keep-alive :include="cachedViews">
          <component :is="Component" :key="route.path" />
        </keep-alive>
      </transition>
    </router-view>
  </section>
</template>

<script>
// Vue3 迁移：router-view 改为 v-slot 写法；transition 名称保持与 transition.scss 一致。
import { mapState } from 'vuex'

export default {
  name: 'AppMain',
  computed: {
    ...mapState({
      cachedViews: (state) => state.tagsView.cachedViews,
      needTagsView: (state) => state.settings.tagsView,
      fixedHeader: (state) => state.settings.fixedHeader
    }),
    style() {
      return { minHeight: this.needTagsView ? 'calc(100vh - 84px)' : 'calc(100vh - 50px)' }
    }
  }
}
</script>

<style lang="scss" scoped>
.app-main {
  position: relative;
  width: 100%;
  padding: 20px;
  box-sizing: border-box;
  overflow: hidden;
}
.fixed-header + .app-main {
  padding-top: 50px;
}
.hasTagsView .fixed-header + .app-main {
  padding-top: 84px;
}
</style>

<style lang="scss">
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-track {
  background-color: #f1f1f1;
}
::-webkit-scrollbar-thumb {
  background-color: #c0c0c0;
  border-radius: 3px;
}
</style>
