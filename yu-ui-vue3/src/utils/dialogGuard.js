/**
 * 表单防误关 mixin（U3 交互体验收口：未保存修改二次确认）—— yu-ui-vue3 版。
 *
 * 与 Vue2 (yu-ui) 版逐方法同构，复用 Options API Mixin 结构：
 * Element Plus 的 el-dialog 同样支持 :before-close，$modal.confirm 走 ElMessageBox 返回 Promise。
 * 注意 RuoYi-Vue3 Options API 已知坑：data 字段名不可与方法名相同——本 mixin 的
 * data 字段 dialogSnapshot / dialogSnapshots 与各方法名（captureDialogSnapshot 等）无冲突。
 *
 * 用法（opt-in，逐页接入）：
 *   1. import dialogGuard from '@/utils/dialogGuard'，组件 mixins: [dialogGuard]；
 *   2. el-dialog 绑定 :before-close="guardedBeforeClose"（覆盖 X / 遮罩 / ESC）；
 *   3. 「取消」按钮 @click="guardedCancel(cancel)"；
 *   4. handleAdd / handleUpdate 数据回填、对话框打开后调用 captureDialogSnapshot()；
 *   5. 除 form 外的独立子表用组件级 guardWatch: [...] 纳入脏检查。
 *
 * E7 第二版（一页多可编辑对话框）：页面除主编辑框外还有「审批 / 审核意见 / 生成参数」
 * 之类第二个可编辑对话框时，用命名槽位单独采集，与主槽互不干扰：
 *   6. 组件声明 guardSlots: { approve: ['approveForm', 'approveRow'] }
 *      （键=槽位名，值=参与脏比较的 data 字段名数组；裸标量同样支持，
 *       如 audit: ['auditOpinion']）；
 *   7. 该对话框回填后调用 captureSlotSnapshot('approve')；
 *   8. el-dialog 绑定 :before-close="done => guardedSlotBeforeClose('approve', done)"，
 *      「取消」按钮 @click="guardedSlotCancel('approve', cancelApprove)"。
 *
 * 为什么不能把第二对话框塞进 guardWatch：主槽固定读 this.form，第二对话框绑的是
 * 另一份 data（approveForm / auditOpinion…）。混进同一个快照后，"打开审批框"和
 * "改主表"共用一份基线，任一边变化都会让另一边误判脏；独立槽位把"快照来源"
 * 从隐式约定改成显式声明，也让"附加框开关不污染主框快照"变成结构保证而非断言。
 *
 * V6.4 同步修正：本栈此前是同步采集快照，而 Vue2 版早就改成 $nextTick 采集。
 * 同步采集会把富文本编辑器"收到 value 后重新规范化并回写 v-model"的中间态当成基线，
 * 表现为"一打开就判脏"。两栈对齐为 $nextTick 采集，行为与 Vue2 完全一致。
 *
 * 约定：未调用 captureDialogSnapshot() / captureSlotSnapshot() 时快照为空，
 * isDialogDirty() / isSlotDirty() 恒为 false，即对只读/详情类对话框零副作用，不会误报。
 */

import { t } from './i18n'

/** 脏数据二次确认的默认文案（槽位可传第 3 个参数覆盖，如「审批意见尚未提交…」） */
const DIRTY_PROMPT = '表单存在未保存的修改，确定要关闭吗？'

export default {
  data() {
    return {
      // 主对话框（this.form + guardWatch）的快照，undefined 表示未采集
      dialogSnapshot: undefined,
      // 命名槽位快照：槽位名 -> JSON 串；某键缺失/为 undefined 表示该槽未采集
      dialogSnapshots: {}
    }
  },
  methods: {
    /**
     * 采集脏检查基线：在对话框打开且表单数据回填完成后调用。
     * 延后一拍采集：富文本编辑器等在收到新 value 后会把内容重新规范化并回写 v-model，
     * 同步采集会把规范化前的值当作基线，导致“一打开就判脏”。
     */
    captureDialogSnapshot() {
      this.dialogSnapshot = undefined
      this.$nextTick(() => {
        this.dialogSnapshot = this._collectGuardState()
      })
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
      this._confirmIfDirty(this.isDialogDirty(), () => { this.dialogSnapshot = undefined }, done)
    },
    /** 供页面「取消」按钮复用：脏则二次确认后再执行原有关闭逻辑 rawCancel */
    guardedCancel(rawCancel) {
      this._confirmIfDirty(this.isDialogDirty(), () => { this.dialogSnapshot = undefined }, rawCancel)
    },

    /* ===== 命名槽位：第二及以后的可编辑对话框 ===== */

    /** 采集命名槽位基线；slotKey 必须已在组件 guardSlots 中声明 */
    captureSlotSnapshot(slotKey) {
      // 先做一次同步校验：未声明槽位属于接入错误，必须在打开对话框时炸出来，
      // 而不是悄悄退化成"这个框永远不设防"
      this._collectSlotState(slotKey)
      this._writeSlotSnapshot(slotKey, undefined)
      this.$nextTick(() => {
        this._writeSlotSnapshot(slotKey, this._collectSlotState(slotKey))
      })
    },
    /** 汇总某槽位参与脏比较的状态并序列化 */
    _collectSlotState(slotKey) {
      const slots = this.guardSlots || {}
      const sources = slots[slotKey]
      if (!Array.isArray(sources) || sources.length === 0) {
        // 静默返回空串等于声明"永不判脏"，比直接报错危险
        throw new Error(`dialogGuard：槽位 "${slotKey}" 未在组件 guardSlots 中声明字段数组`)
      }
      const state = {}
      sources.forEach(key => { state[key] = this[key] })
      return JSON.stringify(state)
    },
    /** 槽位相对快照是否发生变化；未采集时恒 false */
    isSlotDirty(slotKey) {
      const snapshot = this.dialogSnapshots[slotKey]
      if (snapshot === undefined) return false
      return this._collectSlotState(slotKey) !== snapshot
    },
    /** 写槽位快照：整体替换对象而非逐键赋值，Vue2 也能观测到新增键 */
    _writeSlotSnapshot(slotKey, value) {
      const next = Object.assign({}, this.dialogSnapshots)
      next[slotKey] = value
      this.dialogSnapshots = next
    },
    /** 主动丢弃某槽位基线（如提交成功后） */
    resetSlotSnapshot(slotKey) {
      this._writeSlotSnapshot(slotKey, undefined)
    },
    /** 槽位版 :before-close：模板写 done => guardedSlotBeforeClose('approve', done) */
    guardedSlotBeforeClose(slotKey, done, prompt) {
      this._confirmIfDirty(this.isSlotDirty(slotKey), () => this.resetSlotSnapshot(slotKey), done, prompt)
    },
    /** 槽位版「取消」按钮 */
    guardedSlotCancel(slotKey, rawCancel, prompt) {
      this._confirmIfDirty(this.isSlotDirty(slotKey), () => this.resetSlotSnapshot(slotKey), rawCancel, prompt)
    },

    /** 脏则二次确认后再跑 proceed，不脏直接跑；主槽与命名槽共用同一套确认语义 */
    _confirmIfDirty(dirty, clearSnapshot, proceed, prompt) {
      const run = () => {
        clearSnapshot()
        typeof proceed === 'function' && proceed()
      }
      if (!dirty) {
        run()
        return
      }
      this.$modal.confirm(prompt || t('dialog.dirty', DIRTY_PROMPT)).then(run).catch(() => {})
    }
  }
}
