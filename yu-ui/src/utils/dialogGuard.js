/**
 * 表单防误关 mixin（U3 交互体验收口：未保存修改二次确认）。
 *
 * 场景：对话框表单编辑到一半，用户点击关闭按钮 / 遮罩 / ESC / 「取消」，
 * 原若依逻辑会直接丢弃输入。本 mixin 在对话框打开时对表单做快照，
 * 关闭前比对是否发生变化，脏则弹二次确认，避免误操作丢失未保存内容。
 *
 * 用法（opt-in，逐页接入，不改动未接入页面）：
 *   1. import dialogGuard from '@/utils/dialogGuard'，组件 mixins: [dialogGuard]；
 *   2. el-dialog 上绑定 :before-close="guardedBeforeClose"（覆盖 X / 遮罩 / ESC）；
 *   3. 「取消」按钮改为 @click="guardedCancel(cancel)"（cancel 为页面原有关闭方法）；
 *   4. handleAdd / handleUpdate 在数据回填完成、对话框打开后调用 captureDialogSnapshot()；
 *   5. 若表单除 form 外还有独立子表（如岗位/资格明细），组件声明
 *      guardWatch: ['positionList', 'qualificationList'] 纳入脏检查。
 *
 * 约定：未调用 captureDialogSnapshot() 时快照为空，isDialogDirty() 恒为 false，
 * 即对只读/详情类对话框零副作用，不会误报。
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
