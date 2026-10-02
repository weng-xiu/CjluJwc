/**
 * 表单防误关 mixin（U3 交互体验收口：未保存修改二次确认）—— yu-ui-vue3 同步版。
 *
 * 与 Vue2 (yu-ui) 版约定完全一致，复用 Options API  Mixin 结构：
 * Element Plus 的 el-dialog 同样支持 :before-close，$modal.confirm 走 ElMessageBox 返回 Promise。
 * 注意 RuoYi-Vue3 Options API 已知坑：data 字段名不可与方法名相同——本 mixin 的
 * data 字段 dialogSnapshot 与各方法名（captureDialogSnapshot 等）无冲突。
 *
 * 用法（opt-in，逐页接入）：
 *   1. import dialogGuard from '@/utils/dialogGuard'，组件 mixins: [dialogGuard]；
 *   2. el-dialog 绑定 :before-close="guardedBeforeClose"（覆盖 X / 遮罩 / ESC）；
 *   3. 「取消」按钮 @click="guardedCancel(cancel)"；
 *   4. handleAdd / handleUpdate 数据回填、对话框打开后调用 captureDialogSnapshot()；
 *   5. 除 form 外的独立子表用组件级 guardWatch: [...] 纳入脏检查。
 */
export default {
  data() {
    return {
      // 对话框打开时的表单快照（JSON 串），undefined 表示未采集
      dialogSnapshot: undefined
    }
  },
  methods: {
    /** 采集脏检查基线：在对话框打开且表单数据回填完成后调用 */
    captureDialogSnapshot() {
      this.dialogSnapshot = this._collectGuardState()
    },
    /** 汇总参与脏比较的状态并序列化（form 必含，guardWatch 指定的键附加） */
    _collectGuardState() {
      const state = { form: this.form }
      const watch = this.guardWatch || []
      watch.forEach(key => { state[key] = this[key] })
      return JSON.stringify(state)
    },
    /** 当前表单相对快照是否发生变化；未采集快照时恒 false */
    isDialogDirty() {
      if (this.dialogSnapshot === undefined) return false
      return this._collectGuardState() !== this.dialogSnapshot
    },
    /** el-dialog :before-close —— 关闭按钮/遮罩/ESC 的统一拦截 */
    guardedBeforeClose(done) {
      if (this.isDialogDirty()) {
        this.$modal.confirm('表单存在未保存的修改，确定要关闭吗？')
          .then(() => { this.dialogSnapshot = undefined; done() })
          .catch(() => {})
      } else {
        this.dialogSnapshot = undefined
        done()
      }
    },
    /** 供页面「取消」按钮复用：脏则二次确认后再执行原有关闭逻辑 rawCancel */
    guardedCancel(rawCancel) {
      if (this.isDialogDirty()) {
        this.$modal.confirm('表单存在未保存的修改，确定要关闭吗？')
          .then(() => { this.dialogSnapshot = undefined; typeof rawCancel === 'function' && rawCancel() })
          .catch(() => {})
      } else {
        this.dialogSnapshot = undefined
        typeof rawCancel === 'function' && rawCancel()
      }
    }
  }
}
