<template>
  <!--
    U3 统一空态（Vue2 主栈版）：element-ui 2.15 已内建 el-empty，此处仅做薄封装，
    与 Vue3 版保持同一 props 契约（仅 description / imageSize，无 image 属性），便于双栈同口径迁移。
  -->
  <div class="app-empty" role="status">
    <el-empty :description="description" :image-size="imageSize">
      <slot />
    </el-empty>
  </div>
</template>

<script>
export default {
  name: 'AppEmpty',
  props: {
    /** 空态文案（默认按场景给出引导） */
    description: { type: String, default: '暂无数据' },
    /** 插图尺寸 */
    imageSize: { type: Number, default: 120 }
  }
}
</script>

<style lang="scss" scoped>
.app-empty {
  padding: 8px 16px;
  /* 令牌化：暗色下不出现白底块（el-empty 自身无底色，仅需文案色对齐） */
  ::v-deep .el-empty__description p {
    color: var(--dt-text-secondary);
  }
}
</style>
