<template>
  <div class="mobile-borrow">
    <!-- 发起申请 -->
    <div class="apply-block">
      <div class="block-title">
        <span>借用申请</span>
        <el-button type="primary" size="mini" plain @click="applyOpen = !applyOpen">{{ applyOpen ? '收起' : '发起申请' }}</el-button>
      </div>
      <div v-show="applyOpen" class="apply-form">
        <div class="fi">
          <label>教室</label>
          <el-select v-model="form.classroomId" filterable placeholder="请选择教室" size="small" style="width:100%" @change="preCheck">
            <el-option v-for="c in classrooms" :key="c.classroomId" :value="c.classroomId"
                       :label="c.classroomName + (c.buildingName ? ' · ' + c.buildingName : '') + '（' + (c.capacity || 0) + '人）'" />
          </el-select>
        </div>
        <div class="fi">
          <label>借用日期</label>
          <el-date-picker v-model="form.borrowDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" size="small" style="width:100%" @change="preCheck" />
        </div>
        <div class="fi fi-row">
          <div class="fi-half">
            <label>开始时间</label>
            <el-input v-model="form.startTime" placeholder="如 08:00" size="small" @blur="preCheck" />
          </div>
          <div class="fi-half">
            <label>结束时间</label>
            <el-input v-model="form.endTime" placeholder="如 10:00" size="small" @blur="preCheck" />
          </div>
        </div>
        <div class="fi fi-row">
          <div class="fi-half">
            <label>借用人次</label>
            <el-input-number v-model="form.attendeeCount" :min="0" size="small" controls-position="right" style="width:100%" />
          </div>
          <div class="fi-half">
            <label>联系电话</label>
            <el-input v-model="form.contactPhone" placeholder="选填" size="small" />
          </div>
        </div>
        <div class="fi">
          <label>借用用途</label>
          <el-input v-model="form.purpose" type="textarea" :rows="2" maxlength="200" placeholder="请说明借用用途" size="small" />
        </div>
        <div v-if="conflictTips.length" class="conflict-box">
          <div class="conflict-title"><i class="el-icon-warning-outline"></i> 检测到冲突：</div>
          <div v-for="(t, i) in conflictTips" :key="i" class="conflict-item">{{ t }}</div>
        </div>
        <el-button type="primary" size="small" class="submit-btn" :loading="submitting" @click="submitApply">提交申请（冲突校验+进入审批）</el-button>
      </div>
    </div>

    <!-- 我的申请 -->
    <div class="block-title"><span>我的申请</span></div>
    <div v-if="loading" class="loading-state"><i class="el-icon-loading"></i> 加载中...</div>
    <div v-else-if="list.length === 0" class="empty-state"><i class="el-icon-office-building"></i><p>暂无借用申请</p></div>
    <div v-for="row in list" :key="row.borrowId" class="rec-card">
      <div class="rec-top">
        <span class="rec-title">{{ row.classroomName || ('教室#' + row.classroomId) }}<span v-if="row.buildingName" class="rec-building">{{ row.buildingName }}</span></span>
        <span class="rec-status" :class="'st-' + row.approveStatus">{{ approveText[row.approveStatus] || '未知' }}</span>
      </div>
      <div class="rec-when"><i class="el-icon-date"></i> {{ fmtDate(row.borrowDate) }} {{ row.startTime || '-' }}~{{ row.endTime || '-' }} · {{ row.attendeeCount || 0 }}人</div>
      <div class="rec-purpose">{{ row.purpose }}</div>
      <div v-if="row.deptApproveBy || row.aaApproveBy" class="rec-trace">
        <div v-if="row.deptApproveBy">院系：{{ row.deptApproveBy }} {{ row.deptOpinion || '（无意见）' }}</div>
        <div v-if="row.aaApproveBy">教务处：{{ row.aaApproveBy }} {{ row.aaOpinion || '（无意见）' }}</div>
      </div>
      <div class="rec-actions">
        <el-button v-if="row.procInstId" size="mini" type="text" @click="openTrace(row)">流程进度</el-button>
        <el-button v-if="row.approveStatus === '0'" size="mini" type="text" style="color:#f56c6c" @click="cancelRow(row)">撤销</el-button>
      </div>
    </div>
    <div v-if="!loading && list.length < total" class="load-more" @click="loadMore">点击加载更多</div>

    <!-- 流程进度弹层 -->
    <div v-if="traceRow" class="mask" @click.self="traceRow = null">
      <div class="dialog">
        <div class="dialog-title">借用审批进度</div>
        <div v-if="traceLoading" class="loading-state"><i class="el-icon-loading"></i> 查询中...</div>
        <template v-else-if="traceData">
          <div class="steps">
            <div v-for="(s, i) in ['提交申请', '院系审批', '教务处审批', '办结']" :key="i" class="step-item" :class="{ done: i < traceActive, current: i === traceActive }">
              <span class="step-dot"></span><span class="step-label">{{ s }}</span>
            </div>
          </div>
          <div v-for="(t, i) in (traceData.tasks || [])" :key="'tk' + i" class="tl-item">
            <div class="tl-dot" :class="{ done: t.endTime }"></div>
            <div class="tl-body">
              <div class="tl-name">{{ t.taskName }}<span v-if="t.assignee" class="tl-assignee">（{{ t.assignee }}）</span></div>
              <div class="tl-time">{{ fmt(t.endTime || t.startTime) }}</div>
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
import { listMyBorrow, listBorrowClassrooms, applyBorrow, cancelBorrow, borrowTrace, checkBorrowConflict } from '@/api/portal/borrow'

export default {
  name: 'MobileBorrow',
  data() {
    return {
      applyOpen: false,
      submitting: false,
      classrooms: [],
      conflictTips: [],
      form: { classroomId: null, borrowDate: '', startTime: '', endTime: '', attendeeCount: 0, contactPhone: '', purpose: '' },
      loading: false,
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 20,
      approveText: { '0': '审核中', '1': '已通过', '2': '已驳回', '3': '已撤销', '4': '冲突退回' },
      traceRow: null,
      traceLoading: false,
      traceData: null,
      traceFallback: ''
    }
  },
  computed: {
    traceActive() {
      if (!this.traceData) return 0
      if (this.traceData.endTime) return 4
      const done = (this.traceData.tasks || []).filter(t => t.endTime).length
      return Math.min(1 + done, 3)
    }
  },
  mounted() {
    listBorrowClassrooms({ pageNum: 1, pageSize: 500 }).then(r => { this.classrooms = r.rows || [] }).catch(() => {})
    this.fetch()
  },
  methods: {
    fetch() {
      this.loading = true
      listMyBorrow({ pageNum: this.pageNum, pageSize: this.pageSize }).then(r => {
        const rows = r.rows || []
        this.list = this.pageNum === 1 ? rows : this.list.concat(rows)
        this.total = r.total || 0
      }).finally(() => { this.loading = false })
    },
    reload() { this.pageNum = 1; this.fetch() },
    loadMore() { this.pageNum++; this.fetch() },
    preCheck() {
      this.conflictTips = []
      if (!this.form.classroomId || !this.form.borrowDate) return
      checkBorrowConflict({
        classroomId: this.form.classroomId,
        borrowDate: this.form.borrowDate,
        startTime: this.form.startTime,
        endTime: this.form.endTime
      }).then(r => { this.conflictTips = r.data || [] }).catch(() => {})
    },
    submitApply() {
      if (!this.form.classroomId || !this.form.borrowDate || !this.form.purpose) {
        this.toast('请填写教室、日期与用途', 'warning'); return
      }
      this.submitting = true
      applyBorrow(this.form).then(() => {
        this.toast('申请已提交，等待院系审批')
        this.applyOpen = false
        this.form = { classroomId: null, borrowDate: '', startTime: '', endTime: '', attendeeCount: 0, contactPhone: '', purpose: '' }
        this.reload()
      }).finally(() => { this.submitting = false })
    },
    cancelRow(row) {
      this.$confirm('撤销后申请将终止审批流程，确定撤销？', '提示', { type: 'warning' }).then(() => {
        cancelBorrow(row.borrowId).then(() => { this.toast('已撤销'); this.reload() })
      }).catch(() => {})
    },
    openTrace(row) {
      this.traceRow = row
      this.traceLoading = true
      this.traceData = null
      this.traceFallback = '流程信息暂不可用（当前状态：' + (this.approveText[row.approveStatus] || '') + '）'
      borrowTrace(row.borrowId).then(r => { this.traceData = r.data }).finally(() => { this.traceLoading = false })
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
.mobile-borrow { padding: 12px; }
.block-title { display: flex; align-items: center; justify-content: space-between; font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 8px; }
.apply-block { background: #fff; border-radius: 10px; padding: 12px 14px; margin-bottom: 12px; }
.apply-form { border-top: 1px solid #f0f2f5; padding-top: 10px; }
.fi { margin-bottom: 10px; }
.fi label { display: block; font-size: 13px; color: #606266; margin-bottom: 4px; }
.fi-row { display: flex; gap: 10px; }
.fi-half { flex: 1; }
.submit-btn { width: 100%; }
.conflict-box { background: #fdf6ec; border-radius: 6px; padding: 8px 10px; margin-bottom: 10px; font-size: 12px; color: #e6a23c; }
.conflict-title { font-weight: 600; margin-bottom: 2px; }
.conflict-item { line-height: 1.6; }
.loading-state, .empty-state { text-align: center; color: #c0c4cc; padding: 30px 0; font-size: 13px; }
.empty-state i { font-size: 36px; display: block; margin-bottom: 6px; }
.rec-card { background: #fff; border-radius: 8px; padding: 12px; margin-bottom: 10px; }
.rec-top { display: flex; justify-content: space-between; align-items: center; }
.rec-title { font-size: 15px; font-weight: 600; color: #303133; }
.rec-building { font-size: 11px; color: #909399; font-weight: 400; margin-left: 6px; }
.rec-status { font-size: 12px; padding: 2px 8px; border-radius: 10px; flex-shrink: 0; }
.st-0 { background: #fdf6ec; color: #e6a23c; }
.st-1 { background: #f0f9ec; color: #67c23a; }
.st-2, .st-4 { background: #fef0f0; color: #f56c6c; }
.st-3 { background: #f4f4f5; color: #909399; }
.rec-when { font-size: 12px; color: #007ab8; margin: 6px 0 2px; }
.rec-purpose { font-size: 13px; color: #606266; margin: 2px 0; }
.rec-trace { font-size: 12px; color: #909399; background: #f8f9fb; border-radius: 4px; padding: 6px 8px; margin-top: 6px; }
.rec-actions { border-top: 1px solid #f5f7fa; margin-top: 6px; text-align: right; }
.load-more { text-align: center; font-size: 13px; color: #007ab8; padding: 10px 0 20px; cursor: pointer; }
.mask { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.45); z-index: 999; display: flex; align-items: center; justify-content: center; }
.dialog { width: 88%; max-height: 74vh; overflow-y: auto; background: #fff; border-radius: 10px; padding: 16px; }
.dialog-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; }
.steps { display: flex; margin-bottom: 14px; }
.step-item { flex: 1; text-align: center; font-size: 11px; color: #c0c4cc; }
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
.trace-fallback { font-size: 13px; color: #909399; padding: 16px 0; text-align: center; }
.dialog-actions { text-align: right; margin-top: 12px; }
</style>
