<template>
  <el-drawer v-model="showDrawer" title="布局设置" :size="drawerSize" class="setting-drawer">
    <div class="setting-drawer-block-checbox">
      <div class="handle-item">
        <span>标签页</span>
        <el-switch v-model="settings.tagsView" @change="handleTagsView" />
      </div>
      <div class="handle-item">
        <span>固定头部</span>
        <el-switch v-model="settings.fixedHeader" @change="fixHeader" />
      </div>
      <div class="handle-item">
        <span>侧边栏 Logo</span>
        <el-switch v-model="settings.sidebarLogo" @change="sidebarLogo" />
      </div>
      <div class="handle-item">
        <span>动态标题</span>
        <el-switch v-model="settings.dynamicTitle" @change="dynamicTitle" />
      </div>
      <div class="handle-item">
        <span>侧栏配色</span>
        <el-radio-group v-model="sideTheme" size="small" @change="handleTheme">
          <el-radio value="theme-dark">深色</el-radio>
          <el-radio value="theme-light">浅色</el-radio>
        </el-radio-group>
      </div>
    </div>
  </el-drawer>
</template>

<script>
// Vue3 迁移：el-dialog :visible.sync → el-drawer v-model；:label 值语义 → value；
// 精简原主题色板/导航模式等高级项，保留核心开关且与 store 双向同步。
import defaultSettings from '@/settings'
import { useDynamicTitle } from '@/utils/dynamicTitle'

export default {
  name: 'Settings',
  data() {
    return {
      showDrawer: false,
      drawerSize: document.body.clientWidth < 992 ? '100%' : '300px',
      sideTheme: this.$store.state.settings.sideTheme
    }
  },
  computed: {
    settings() {
      return this.$store.state.settings
    }
  },
  methods: {
    openSetting() {
      this.showDrawer = true
    },
    handleChange(settingKey, value) {
      this.$store.dispatch('settings/changeSetting', { key: settingKey, value })
    },
    handleTagsView(value) {
      this.handleChange('tagsView', value)
    },
    fixHeader(value) {
      this.handleChange('fixedHeader', value)
    },
    sidebarLogo(value) {
      this.handleChange('sidebarLogo', value)
    },
    dynamicTitle(value) {
      this.handleChange('dynamicTitle', value)
      useDynamicTitle()
    },
    handleTheme(value) {
      this.handleChange('sideTheme', value)
    }
  }
}
</script>

<style lang="scss" scoped>
.handle-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.65);
}
</style>
