<template>
  <!--
    U3 批量操作反馈（Vue2 主栈版）：进度层 + 结果汇总，千条导入/审核不卡 UI。
    用法：<app-batch-progress :visible.sync="batchProgressOpen" :running="batchRunning" ... />
  -->
  <el-dialog
    :title="title"
    :visible="dialogVisible"
    width="460px"
    :close-on-click-modal="false"
    :show-close="!running"
    append-to-body
    @closed="$emit('closed')"
  >
    <div v-if="running">
      <el-progress v-if="!indeterminate" :percentage="percentage" :stroke-width="14" />
      <div v-else class="batch-indeterminate" role="progressbar" aria-valuetext="处理中">
        <span class="bar"></span>
      </div>
      <p class="tip">{{ tipText }}</p>
    </div>
    <div v-else class="batch-result">
      <i :class="resultIconClass" aria-hidden="true"></i>
      <p class="result-title">{{ resultTitle }}</p>
      <div class="summary">
        <span>总数 {{ total }}</span>
        <el-tag type="success" size="small">成功 {{ success }}</el-tag>
        <el-tag v-if="failed > 0" type="danger" size="small">失败 {{ failed }}</el-tag>
      </div>
      <slot name="detail" :failed="failed" />
    </div>
    <div slot="footer" class="dialog-footer">
      <el-button v-if="running" type="warning" plain @click="runInBackground">后台运行</el-button>
      <el-button v-else type="primary" @click="close">关 闭</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'AppBatchProgress',
  props: {
    /** 弹层显隐（Vue2 侧走 :visible.sync） */
    visible: { type: Boolean, default: false },
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
  computed: {
    /** el-dialog 的显隐双向绑定：子组件只 emit，父组件持真值（避免直改 prop 告警） */
    dialogVisible: {
      get() {
        return this.visible
      },
      set(v) {
        this.$emit('update:visible', v)
      }
    },
    percentage() {
      if (!this.total) return 0
      return Math.min(100, Math.round((this.done / this.total) * 100))
    },
    resultIconClass() {
      return this.failed > 0
        ? 'el-icon-warning-outline batch-icon is-warning'
        : 'el-icon-circle-check batch-icon is-success'
    },
    resultTitle() {
      return this.failed > 0 ? '处理完成（存在失败项）' : '处理完成'
    }
  },
  methods: {
    close() {
      this.$emit('update:visible', false)
    },
    /** 「后台运行」：通知父级后自行收起进度层（请求继续，页面不卡在弹层上） */
    runInBackground() {
      this.$emit('cancel')
      this.close()
    }
  }
}
</script>

<style lang="scss" scoped>
.tip {
  margin-top: 12px;
  color: var(--dt-text-secondary);
  font-size: 13px;
  text-align: center;
}
.batch-indeterminate {
  position: relative;
  height: 14px;
  overflow: hidden;
  border-radius: 100px;
  background: var(--dt-fill-light);
}
.batch-indeterminate .bar {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  width: 40%;
  border-radius: 100px;
  background: var(--el-color-primary);
  animation: batch-slide 1.2s ease-in-out infinite;
}
@keyframes batch-slide {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(250%);
  }
}
@media (prefers-reduced-motion: reduce) {
  .batch-indeterminate .bar {
    animation: none;
    width: 100%;
    opacity: 0.6;
  }
}
.batch-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0 4px;
}
.batch-icon {
  font-size: 44px;
  line-height: 1;
}
.batch-icon.is-success {
  color: var(--el-color-success);
}
.batch-icon.is-warning {
  color: var(--el-color-warning);
}
.result-title {
  margin: 10px 0 0;
  color: var(--dt-text-primary);
  font-size: 15px;
  font-weight: 600;
}
.summary {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 10px;
  color: var(--dt-text-regular);
  font-size: 13px;
}
</style>
