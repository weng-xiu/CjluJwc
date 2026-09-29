<template>
  <!-- U3 统一骨架屏：列表/详情首屏加载占位，行数/标题可配，默认插槽放真实内容 -->
  <el-skeleton :loading="loading" animated :count="count">
    <template #template>
      <el-skeleton-item v-if="title" variant="h3" style="margin-bottom: 16px" />
      <div v-for="n in rows" :key="n" class="sk-row">
        <el-skeleton-item variant="rect" style="width: 100%; height: 100%" />
      </div>
    </template>
    <template #default>
      <slot />
    </template>
  </el-skeleton>
</template>

<script>
export default {
  name: 'AppSkeleton',
  props: {
    /** 是否处于加载态 */
    loading: { type: Boolean, default: false },
    /** 骨架行数 */
    rows: { type: Number, default: 6 },
    /** 重复组数 */
    count: { type: Number, default: 1 },
    /** 是否显示标题占位 */
    title: { type: Boolean, default: false }
  }
}
</script>

<style lang="scss" scoped>
.sk-row {
  height: 40px;
  margin-bottom: 10px;
}
</style>
