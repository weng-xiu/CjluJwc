<template>
  <div v-if="!item.hidden">
    <template
      v-if="hasOneShowingChild(item.children, item) && (!onlyOneChild.children || onlyOneChild.noShowingChildren) && !item.alwaysShow"
    >
      <app-link v-if="onlyOneChild.meta" :to="resolvePath(onlyOneChild.path, onlyOneChild.query)">
        <el-menu-item :index="resolvePath(onlyOneChild.path)" :class="{ 'submenu-title-noDropdown': !isNest }">
          <!-- '#' 为数据库菜单图标占位符，需过滤避免生成无效的 #icon-# 引用 -->
          <el-icon v-if="validIcon(onlyOneChild.meta.icon) || validIcon(item.meta && item.meta.icon)">
            <svg-icon :icon-class="validIcon(onlyOneChild.meta.icon) || validIcon(item.meta && item.meta.icon)" />
          </el-icon>
          <template #title>
            <span class="menu-title" :title="onlyOneChild.meta.title">{{ onlyOneChild.meta.title }}</span>
          </template>
        </el-menu-item>
      </app-link>
    </template>

    <el-sub-menu v-else ref="subMenu" :index="resolvePath(item.path)" teleported>
      <template v-if="item.meta" #title>
        <el-icon v-if="validIcon(item.meta.icon)">
          <svg-icon :icon-class="validIcon(item.meta.icon)" />
        </el-icon>
        <span class="menu-title" :title="item.meta.title">{{ item.meta.title }}</span>
      </template>
      <sidebar-item
        v-for="(child, index) in item.children"
        :key="child.path + index"
        :is-nest="true"
        :item="child"
        :base-path="resolvePath(child.path)"
        class="nest-menu"
      />
    </el-sub-menu>
  </div>
</template>

<script>
// Vue3 迁移：el-submenu → el-sub-menu；slot="title" → <template #title>；
// 移除 node 'path' 模块依赖（Vite 浏览器环境无 path），改用浏览器版 resolve 实现；
// 移除 Item 函数式渲染组件与 FixiOSBug（element-plus 已无该 IE/iOS bug）。
import { isExternal } from '@/utils/validate'
import AppLink from './Link'

// 浏览器版 path.resolve（仅处理以 / 开头或相对拼接的常规菜单路径）
function resolvePath(...paths) {
  const stack = []
  paths.forEach((p) => {
    if (!p) return
    if (p.startsWith('/')) {
      stack.length = 0
      p.split('/').forEach((seg) => {
        if (seg === '' || seg === '.') return
        if (seg === '..') stack.pop()
        else stack.push(seg)
      })
    } else {
      p.split('/').forEach((seg) => {
        if (seg === '' || seg === '.') return
        if (seg === '..') stack.pop()
        else stack.push(seg)
      })
    }
  })
  return '/' + stack.join('/')
}

export default {
  name: 'SidebarItem',
  components: { AppLink },
  props: {
    item: { type: Object, required: true },
    isNest: { type: Boolean, default: false },
    basePath: { type: String, default: '' }
  },
  data() {
    this.onlyOneChild = null
    return {}
  },
  methods: {
    // 过滤空值与 '#' 占位符，返回可用于 svg-icon 的图标名（无有效图标时返回 ''）
    validIcon(icon) {
      return icon && icon !== '#' ? icon : ''
    },
    hasOneShowingChild(children = [], parent) {
      if (!children) {
        children = []
      }
      const showingChildren = children.filter((item) => {
        if (item.hidden) {
          return false
        }
        this.onlyOneChild = item
        return true
      })
      if (showingChildren.length === 1) {
        return true
      }
      if (showingChildren.length === 0) {
        this.onlyOneChild = { ...parent, path: '', noShowingChildren: true }
        return true
      }
      return false
    },
    resolvePath(routePath, routeQuery) {
      if (isExternal(routePath)) {
        return routePath
      }
      if (isExternal(this.basePath)) {
        return this.basePath
      }
      if (routeQuery) {
        const query = JSON.parse(routeQuery)
        return { path: resolvePath(this.basePath, routePath), query }
      }
      return resolvePath(this.basePath, routePath)
    }
  }
}
</script>
