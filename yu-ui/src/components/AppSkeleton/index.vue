<template>
  <!--
    U3 统一骨架屏（Vue2 主栈版，element-ui 2.x 无 el-skeleton 故自建）：
    列表/详情首屏加载占位，行数/标题可配，非加载态直接渲染默认插槽。
  -->
  <div v-if="loading" class="app-skeleton" role="status" aria-busy="true" :aria-label="label">
    <div v-for="group in count" :key="'g' + group" class="sk-group">
      <div v-if="title" class="sk-block sk-title"></div>
      <div v-for="n in rows" :key="n" class="sk-block sk-row"></div>
    </div>
  </div>
  <!-- Vue2 不允许 <slot> 作为根元素，非加载态用透传容器承载默认插槽 -->
  <div v-else class="app-skeleton-content"><slot /></div>
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
    title: { type: Boolean, default: false },
    /** 读屏播报文案 */
    label: { type: String, default: '内容加载中' }
  }
}
</script>

<style lang="scss" scoped>
.sk-group + .sk-group {
  margin-top: 24px;
}
.sk-block {
  border-radius: 4px;
  /* 令牌化：亮色取浅填充 + 弱边框，暗色由 tokens.scss 重映射，无白块残留 */
  background: linear-gradient(
    90deg,
    var(--dt-fill-light) 25%,
    var(--dt-border-color-weak) 37%,
    var(--dt-fill-light) 63%
  );
  background-size: 400% 100%;
  animation: sk-shimmer 1.4s ease infinite;
}
.sk-title {
  height: 22px;
  width: 38%;
  margin-bottom: 16px;
}
.sk-row {
  height: 40px;
  margin-bottom: 10px;
}
@keyframes sk-shimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}
/* 减少动态效果偏好下由 a11y.scss 统一降级；此处再兜底一次避免闪烁干扰后仍移动 */
@media (prefers-reduced-motion: reduce) {
  .sk-block {
    animation: none;
  }
}
</style>
