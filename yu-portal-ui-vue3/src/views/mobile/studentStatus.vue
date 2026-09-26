<template>
  <div class="mobile-status">
    <!-- 学籍信息卡 -->
    <div class="stu-card" v-if="student">
      <div class="stu-name">{{ student.studentName || '--' }}
        <span class="stu-no">{{ student.studentId || '' }}</span>
      </div>
      <div class="stu-meta">{{ student.deptName }} · {{ student.majorName }} · {{ student.className }}</div>
      <div class="stu-meta">入学 {{ student.enrollmentYear || '--' }} · {{ statusText(student.studentStatus) }}</div>
    </div>
    <div class="stu-card empty" v-else>
      <div class="stu-name">未找到您的学籍信息</div>
      <div class="stu-meta">请联系教务管理员核实账号与学籍绑定</div>
    </div>

    <!-- 发起申请 -->
    <div class="apply-block">
      <div class="block-title">
        <span>异动申请</span>
        <el-button type="primary" size="small" plain @click="toggleApply">{{ applyOpen ? '收起' : '发起申请' }}</el-button>
      </div>
      <div v-show="applyOpen" class="apply-form">
        <div class="fi">
          <label>异动类型</label>
          <el-select v-model="form.changeType" placeholder="请选择" size="small" style="width:100%">
            <el-option label="休学" value="0" />
            <el-option label="复学" value="1" />
            <el-option label="退学" value="3" />
          </el-select>
        </div>
        <div class="fi">
          <label>异动日期</label>
          <el-date-picker v-model="form.changeDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" size="small" style="width:100%" />
        </div>
        <div class="fi">
          <label>申请原因</label>
          <el-input v-model="form.reason" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请填写申请原因" size="small" />
        </div>
        <el-button type="primary" size="small" class="submit-btn" :loading="submitting" @click="submitApply">提交申请（进入审批流程）</el-button>
      </div>
    </div>

    <!-- 我的申请记录 -->
    <div class="block-title record-title"><span>我的申请记录</span></div>
    <div v-if="loading" class="loading-state"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</div>
    <div v-else-if="list.length === 0" class="empty-state"><el-icon><FolderOpened /></el-icon><p>暂无异动申请</p></div>
    <div v-for="row in list" :key="row.changeId" class="rec-card">
      <div class="rec-top">
        <span class="rec-type">{{ changeTypeText(row.changeType) }}</span>
        <span class="rec-status" :class="'st-' + row.approveStatus">{{ approveText[row.approveStatus] || '未知' }}</span>
      </div>
      <div class="rec-reason">{{ row.reason }}</div>
      <div class="rec-meta">申请 {{ fmtDate(row.createTime) }}<template v-if="row.approveOpinion"> · 审批意见：{{ row.approveOpinion }}</template></div>
      <div class="rec-actions">
        <el-button v-if="row.approveStatus !== '3'" size="small" link type="primary" @click="openTrace(row)">审批进度</el-button>
        <el-button v-if="row.approveStatus === '0'" size="small" link type="danger" @click="cancelRow(row)">撤销</el-button>
      </div>
    </div>
    <div v-if="!loading && list.length < total" class="load-more" @click="loadMore">点击加载更多</div>

    <!-- 进度弹层 -->
    <div v-if="traceRow" class="mask" @click.self="traceRow = null">
      <div class="dialog">
        <div class="dialog-title">{{ changeTypeText(traceRow.changeType) }} · 审批进度</div>
        <div v-if="traceLoading" class="loading-state"><el-icon class="is-loading"><Loading /></el-icon> 查询中...</div>
        <template v-else-if="traceData">
          <div class="steps">
            <div v-for="(s, i) in traceSteps" :key="i" class="step-item" :class="{ done: i < traceActive, current: i === traceActive }">
              <span class="step-dot"></span><span class="step-label">{{ s }}</span>
            </div>
          </div>
          <div class="tl">
            <div v-for="(t, i) in (traceData.tasks || [])" :key="'t' + i" class="tl-item">
              <div class="tl-dot" :class="{ done: t.endTime }"></div>
              <div class="tl-body">
                <div class="tl-name">{{ t.taskName }}<span v-if="t.assignee" class="tl-assignee">（{{ t.assignee }}）</span></div>
                <div class="tl-time">{{ fmt(t.endTime || t.startTime) }}</div>
                <template v-for="(c, ci) in (t.comments || [])">
                  <div v-if="c.fullMessage || c.message" :key="ci" class="tl-comment">{{ c.fullMessage || c.message }}</div>
                </template>
              </div>
            </div>
          </div>
        </template>
        <div v-else class="trace-fallback">{{ traceFallback }}</div>
        <div class="dialog-actions"><el-button type="primary" size="small" @click="traceRow = null">关闭</el-button></div>
      </div>
    </div>
  </div>
</template>

<script>
import { getStudentInfo, applyStatusChange, listStatusChanges, traceStatusChange, cancelStatusChange } from '@/api/portal/studentStatus'

export default {
  name: 'MobileStudentStatus',
  data() {
    return {
      student: null,
      applyOpen: false,
      submitting: false,
      form: { changeType: '', changeDate: '', reason: '' },
      loading: false,
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 20,
      approveText: { '0': '审核中', '1': '已通过', '2': '已驳回', '3': '已撤销' },
      traceRow: null,
      traceLoading: false,
      traceData: null,
      traceFallback: ''
    }
  },
  computed: {
    traceSteps() { return ['提交申请', '院系审批', '教务处审批', '办结'] },
    traceActive() {
      if (!this.traceData) return 0
      if (this.traceData.endTime) return 4
      const done = (this.traceData.tasks || []).filter(t => t.endTime).length
      return Math.min(1 + done, 3)
    }
  },
  mounted() {
    getStudentInfo().then(r => { this.student = r.data }).catch(() => {})
    this.fetch()
  },
  methods: {
    toggleApply() { this.applyOpen = !this.applyOpen },
    fetch() {
      this.loading = true
      listStatusChanges({ pageNum: this.pageNum, pageSize: this.pageSize }).then(r => {
        const rows = r.rows || []
        this.list = this.pageNum === 1 ? rows : this.list.concat(rows)
        this.total = r.total || 0
      }).finally(() => { this.loading = false })
    },
    reload() { this.pageNum = 1; this.fetch() },
    loadMore() { this.pageNum++; this.fetch() },
    submitApply() {
      if (!this.form.changeType || !this.form.reason) { this.toast('请填写异动类型与原因', 'warning'); return }
      this.submitting = true
      applyStatusChange(this.form).then(() => {
        this.toast('申请已提交，已进入审批流程')
        this.applyOpen = false
        this.form = { changeType: '', changeDate: '', reason: '' }
        this.reload()
      }).finally(() => { this.submitting = false })
    },
    cancelRow(row) {
      this.$confirm('撤销后申请将终止审批流程，确定撤销？', '提示', { type: 'warning' }).then(() => {
        cancelStatusChange(row.changeId).then(() => { this.toast('已撤销'); this.reload() })
      }).catch(() => {})
    },
    openTrace(row) {
      this.traceRow = row
      this.traceLoading = true
      this.traceData = null
      this.traceFallback = '该申请尚未进入审批流程或流程信息不可用（当前状态：' + (this.approveText[row.approveStatus] || '') + '）'
      traceStatusChange(row.changeId).then(r => { this.traceData = r.data }).finally(() => { this.traceLoading = false })
    },
    changeTypeText(t) {
      const map = { '0': '休学', '1': '复学', '2': '转学', '3': '退学', '4': '保留学籍' }
      return map[t] || '异动'
    },
    statusText(s) {
      const map = { '0': '在读', '1': '休学', '2': '退学', '3': '毕业', '4': '保留学籍' }
      return map[s] || ('状态' + (s || '--'))
    },
    fmt(v) { return v && this.parseTime ? this.parseTime(v, '{y}-{m}-{d} {h}:{i}') : (v || '--') },
    fmtDate(v) { return v && this.parseTime ? this.parseTime(v, '{y}-{m}-{d}') : (v || '--') },
    toast(m, type) {
      this.$message ? this.$message({ message: m, type: type || 'success' }) : window.alert(m)
    }
  }
}
</script>

<style scoped>
.mobile-status { padding: 12px; }
.stu-card { background: linear-gradient(135deg, #003366, #007ab8); color: #fff; border-radius: 10px; padding: 14px 16px; margin-bottom: 12px; }
.stu-card.empty { background: #e4e7ed; color: #909399; }
.stu-name { font-size: 17px; font-weight: 600; }
.stu-no { font-size: 12px; opacity: 0.85; margin-left: 8px; font-weight: 400; }
.stu-meta { font-size: 12px; opacity: 0.9; margin-top: 4px; }
.block-title { display: flex; align-items: center; justify-content: space-between; font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 8px; }
.apply-block { background: #fff; border-radius: 10px; padding: 12px 14px; margin-bottom: 12px; }
.apply-form { border-top: 1px solid #f0f2f5; padding-top: 10px; }
.fi { margin-bottom: 10px; }
.fi label { display: block; font-size: 13px; color: #606266; margin-bottom: 4px; }
.submit-btn { width: 100%; }
.record-title { margin-top: 4px; }
.loading-state, .empty-state { text-align: center; color: #c0c4cc; padding: 30px 0; font-size: 13px; }
.empty-state .el-icon { font-size: 36px; display: block; margin-bottom: 6px; }
.rec-card { background: #fff; border-radius: 8px; padding: 12px; margin-bottom: 10px; }
.rec-top { display: flex; justify-content: space-between; align-items: center; }
.rec-type { font-size: 15px; font-weight: 600; color: #303133; }
.rec-status { font-size: 12px; padding: 2px 8px; border-radius: 10px; }
.st-0 { background: #fdf6ec; color: #e6a23c; }
.st-1 { background: #f0f9ec; color: #67c23a; }
.st-2 { background: #fef0f0; color: #f56c6c; }
.st-3 { background: #f4f4f5; color: #909399; }
.rec-reason { font-size: 13px; color: #606266; margin: 6px 0; }
.rec-meta { font-size: 12px; color: #909399; }
.rec-actions { border-top: 1px solid #f5f7fa; margin-top: 6px; text-align: right; }
.load-more { text-align: center; font-size: 13px; color: #007ab8; padding: 10px 0 20px; cursor: pointer; }
.mask { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.45); z-index: 999; display: flex; align-items: center; justify-content: center; }
.dialog { width: 88%; max-height: 74vh; overflow-y: auto; background: #fff; border-radius: 10px; padding: 16px; }
.dialog-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; }
.steps { display: flex; margin-bottom: 14px; }
.step-item { flex: 1; text-align: center; font-size: 11px; color: #c0c4cc; position: relative; }
.step-dot { display: block; width: 12px; height: 12px; border-radius: 50%; background: #dcdfe6; margin: 0 auto 4px; }
.step-item.done .step-dot, .step-item.current .step-dot { background: #007ab8; }
.step-item.done .step-label, .step-item.current .step-label { color: #007ab8; }
.tl-item { display: flex; padding: 6px 0; }
.tl-dot { width: 10px; height: 10px; border-radius: 50%; background: #dcdfe6; margin: 4px 10px 0 2px; flex-shrink: 0; }
.tl-dot.done { background: #67c23a; }
.tl-body { flex: 1; }
.tl-name { font-size: 13px; color: #303133; }
.tl-assignee { color: #007ab8; }
.tl-time { font-size: 11px; color: #909399; }
.tl-comment { font-size: 12px; color: #606266; background: #f5f7fa; border-radius: 4px; padding: 4px 8px; margin-top: 4px; }
.trace-fallback { font-size: 13px; color: #909399; padding: 16px 0; text-align: center; }
.dialog-actions { text-align: right; margin-top: 12px; }
</style>
