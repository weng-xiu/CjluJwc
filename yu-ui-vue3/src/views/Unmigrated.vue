<template>
  <div class="unmigrated">
    <el-result icon="info" title="页面迁移中" :sub-title="subTitle">
      <template #extra>
        <el-tag type="warning" size="large">Vue2 → Vue3 迁移占位</el-tag>
        <div class="path">{{ currentPath }}</div>
        <div class="tip">
          该业务模块尚未完成 Vue3 迁移，暂以占位页展示以避免菜单点击报错。<br />
          原功能仍可在 Vue2 管理端（yu-ui）中使用。
        </div>
      </template>
    </el-result>
  </div>
</template>

<script>
// 未迁移业务页统一占位：permission store 的 loadView 找不到对应 .vue 时回退到此组件，
// 保证侧栏菜单可点击且不白屏/不构建失败。
export default {
  name: 'Unmigrated',
  computed: {
    currentPath() {
      return this.$route.path
    },
    subTitle() {
      const title = this.$route.meta && this.$route.meta.title
      return title ? `「${title}」模块` : '当前模块'
    }
  }
}
</script>

<style scoped>
.unmigrated {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}
.path {
  margin: 12px 0;
  font-family: Menlo, Consolas, monospace;
  color: #909399;
}
.tip {
  color: #606266;
  font-size: 13px;
  line-height: 1.8;
}
</style>
