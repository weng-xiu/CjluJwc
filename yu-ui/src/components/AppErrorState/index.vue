<template>
  <!--
    U3 统一错误态（Vue2 主栈版，element-ui 2.x 无 el-result 故自建）：
    请求失败/异常占位，可带重试按钮（emit retry），默认插槽追加自定义操作。
  -->
  <div class="app-error-state" role="alert">
    <i class="el-icon-warning-outline app-error-icon" aria-hidden="true"></i>
    <p class="app-error-title">{{ title }}</p>
    <p class="app-error-message">{{ message }}</p>
    <div class="app-error-actions">
      <el-button v-if="showRetry" type="primary" size="small" icon="el-icon-refresh" @click="$emit('retry')">重新加载</el-button>
      <slot />
    </div>
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
  }
}
</script>

<style lang="scss" scoped>
.app-error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  padding: 24px 16px;
  text-align: center;
}
.app-error-icon {
  font-size: 56px;
  line-height: 1;
  color: var(--el-color-danger);
}
.app-error-title {
  margin: 12px 0 0;
  color: var(--dt-text-primary);
  font-size: 16px;
  font-weight: 600;
}
.app-error-message {
  margin: 6px 0 0;
  max-width: 560px;
  color: var(--dt-text-secondary);
  font-size: 13px;
  line-height: 1.6;
  word-break: break-all;
}
.app-error-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
}
</style>
