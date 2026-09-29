<template>
  <!-- U3 批量操作反馈：进度条 + 结果汇总弹层，千条导入不卡 UI -->
  <el-dialog
    v-model="visible"
    :title="title"
    width="460px"
    :close-on-click-modal="false"
    :show-close="!running"
    append-to-body
    @closed="$emit('closed')"
  >
    <template v-if="running">
      <el-progress :percentage="percentage" :indeterminate="indeterminate" :stroke-width="14" />
      <p class="tip">{{ tipText }}</p>
    </template>
    <template v-else>
      <el-result
        :icon="resultIcon"
        :title="resultTitle"
      >
        <template #sub-title>
          <div class="summary">
            <span>总数 {{ total }}</span>
            <el-tag type="success" size="small">成功 {{ success }}</el-tag>
            <el-tag v-if="failed > 0" type="danger" size="small">失败 {{ failed }}</el-tag>
          </div>
        </template>
      </el-result>
      <slot name="detail" :failed="failed" />
    </template>

    <template #footer>
      <el-button v-if="running" type="warning" plain @click="$emit('cancel')">后台运行</el-button>
      <el-button v-else type="primary" @click="close">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script>
export default {
  name: 'AppBatchProgress',
  props: {
    modelValue: { type: Boolean, default: false },
    title: { type: String, default: '批量处理' },
    /** 是否进行中 */
    running: { type: Boolean, default: true },
    /** 无法量化进度时用不定模式 */
    indeterminate: { type: Boolean, default: false },
    total: { type: Number, default: 0 },
    done: { type: Number, default: 0 },
    success: { type: Number, default: 0 },
    failed: { type: Number, default: 0 },
    tipText: { type: String, default: '正在处理，请勿关闭窗口…' }
  },
  emits: ['update:modelValue', 'cancel', 'closed'],
  computed: {
    visible: {
      get() { return this.modelValue },
      set(v) { this.$emit('update:modelValue', v) }
    },
    percentage() {
      if (!this.total) return 0
      return Math.min(100, Math.round((this.done / this.total) * 100))
    },
    resultIcon() {
      return this.failed > 0 ? 'warning' : 'success'
    },
    resultTitle() {
      return this.failed > 0 ? '处理完成（存在失败项）' : '处理完成'
    }
  },
  methods: {
    close() { this.visible = false }
  }
}
</script>

<style lang="scss" scoped>
.tip {
  margin-top: 12px;
  color: var(--dt-text-secondary, #909399);
  font-size: 13px;
  text-align: center;
}
.summary {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}
</style>
