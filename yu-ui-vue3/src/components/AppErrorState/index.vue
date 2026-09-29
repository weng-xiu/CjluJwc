<template>
  <!-- U3 统一错误态：请求失败/异常占位，可带重试按钮（emit retry） -->
  <div class="app-error-state">
    <el-result icon="error" :title="title" :sub-title="message">
      <template #extra>
        <el-button v-if="showRetry" type="primary" @click="$emit('retry')">重新加载</el-button>
        <slot />
      </template>
    </el-result>
  </div>
</template>

<script>
export default {
  name: 'AppErrorState',
  props: {
    /** 错误标题 */
    title: { type: String, default: '加载失败' },
    /** 错误详情（如后端返回信息） */
    message: { type: String, default: '数据请求异常，请稍后重试' },
    /** 是否显示重试按钮 */
    showRetry: { type: Boolean, default: true }
  },
  emits: ['retry']
}
</script>

<style lang="scss" scoped>
.app-error-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
}
</style>
