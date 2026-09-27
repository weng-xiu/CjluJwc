<!--
  Vue3 迁移：替代 Vue2 版 components/IconSelect。
  - 原 requireIcons.js 使用 webpack 的 require.context 读取 svg 目录，Vite 下不存在，
    改用 import.meta.glob('./svg/*.svg') 的 key 列表推导图标名。
  - 图标渲染仍复用全局 SvgIcon 组件（<use xlink:href="#icon-xxx"> 雪碧图，见 assets/icons/index.js）。
  - el-input 的 slot="suffix" → <template #suffix>；el-icon-search 字体类 → <el-icon><Search/></el-icon>。
-->
<template>
  <div class="icon-body">
    <el-input v-model="name" class="icon-search" clearable placeholder="请输入图标名称" @clear="filterIcons" @input="filterIcons">
      <template #suffix>
        <el-icon class="el-input__icon"><Search /></el-icon>
      </template>
    </el-input>
    <div class="icon-list">
      <div class="list-container">
        <div v-for="(item, index) in iconList" class="icon-item-wrapper" :key="index" @click="selectedIcon(item)">
          <div :class="['icon-item', { active: activeIcon === item }]">
            <svg-icon :icon-class="item" class-name="icon" style="height: 25px;width: 16px;" />
            <span>{{ item }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { Search } from '@element-plus/icons-vue'

// 通过 import.meta.glob 收集 svg 目录下的全部图标名（不 eager，仅取 key）
const svgModules = import.meta.glob('../../assets/icons/svg/*.svg')
const allIcons = Object.keys(svgModules).map(p => p.replace(/^.*\/(.+)\.svg$/, '$1'))

export default {
  name: 'IconSelect',
  components: { Search },
  props: {
    activeIcon: { type: String, default: '' }
  },
  data() {
    return {
      name: '',
      iconList: [...allIcons]
    }
  },
  methods: {
    filterIcons() {
      this.iconList = [...allIcons]
      if (this.name) {
        this.iconList = this.iconList.filter(item => item.includes(this.name))
      }
    },
    selectedIcon(name) {
      this.$emit('selected', name)
      document.body.click()
    },
    reset() {
      this.name = ''
      this.iconList = [...allIcons]
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
.icon-body {
  width: 100%;
  padding: 10px;
  .icon-search {
    position: relative;
    margin-bottom: 5px;
  }
  .icon-list {
    height: 200px;
    overflow: auto;
    .list-container {
      display: flex;
      flex-wrap: wrap;
      .icon-item-wrapper {
        width: calc(100% / 3);
        height: 25px;
        line-height: 25px;
        cursor: pointer;
        display: flex;
        .icon-item {
          display: flex;
          max-width: 100%;
          height: 100%;
          padding: 0 5px;
          &:hover {
            background: #ececec;
            border-radius: 5px;
          }
          .icon {
            flex-shrink: 0;
          }
          span {
            display: inline-block;
            vertical-align: -0.15em;
            fill: currentColor;
            padding-left: 2px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }
        .icon-item.active {
          background: #ececec;
          border-radius: 5px;
        }
      }
    }
  }
}
</style>
