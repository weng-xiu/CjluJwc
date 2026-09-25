<template>
  <div class="mobile-adjust">
    <!-- 顶部切换：我的申请 / 待我审批 -->
    <div class="seg-bar">
      <div class="seg-item" :class="{ active: tab === 'mine' }" @click="switchTab('mine')">我的申请</div>
      <div class="seg-item" :class="{ active: tab === 'audit' }" @click="switchTab('audit')">待我审批</div>
    </div>

    <!-- 我的申请 -->
    <template v-if="tab === 'mine'">
      <div class="apply-entry">
        <el-button type="primary" size="small" class="submit-btn" @click="openApply"><i class="el-icon-plus"></i> 发起调停课申请</el-button>
      </div>
      <div v-if="loading" class="loading-state"><i class="el-icon-loading"></i> 加载中...</div>
      <div v-else-if="list.length === 0" class="empty-state"><i class="el-icon-s-operation"></i><p>暂无调停课申请</p></div>
      <div v-for="row in list" :key="row.adjustId" class="rec-card">
        <div class="rec-top">
          <span class="rec-title">{{ row.courseName || ('排课#' + row.scheduleId) }}</span>
          <span class="rec-status" :class="'st-' + row.approveStatus">{{ approveText[row.approveStatus] || '未知' }}</span>
        </div>
        <div class="rec-line"><span class="tag-type">{{ adjustTypeText(row.adjustType) }}</span>原：{{ weekText(row.originalWeekDay) }} 第{{ row.originalStartPeriod }}-{{ row.originalEndPeriod }}节 {{ row.originalClassroomName || '' }}</div>
        <div v-if="row.newWeekDay" class="rec-line new">新：{{ weekText(row.newWeekDay) }} 第{{ row.newStartPeriod }}-{{ row.newEndPeriod }}节<template v-if="row.newDate"> · {{ fmtDate(row.newDate) }}</template><template v-if="row.newClassName"> · {{ row.newClassName }}</template></div>
        <div class="rec-reason">{{ row.reason }}</div>
        <div v-if="row.approveBy" class="rec-audit">{{ row.approveBy }} {{ fmt(row.approveTime) }}<template v-if="row.approveComment"> · {{ row.approveComment }}</template></div>
        <div class="rec-actions">
          <el-button v-if="row.approveStatus === '0'" size="mini" type="text" style="color:#f56c6c" @click="cancelRow(row)">撤销</el-button>
        </div>
      </div>
      <div v-if="!loading && list.length < total" class="load-more" @click="loadMore">点击加载更多</div>
    </template>

    <!-- 待我审批 -->
    <template v-else>
      <div v-if="auditDenied" class="empty-state"><i class="el-icon-lock"></i><p>{{ auditDenied }}</p></div>
      <div v-else-if="auditLoading" class="loading-state"><i class="el-icon-loading"></i> 加载中...</div>
      <div v-else-if="auditList.length === 0" class="empty-state"><i class="el-icon-circle-check"></i><p>暂无待审申请</p></div>
      <div v-for="row in auditList" :key="'a' + row.adjustId" class="rec-card">
        <div class="rec-top">
          <span class="rec-title">{{ row.courseName || ('排课#' + row.scheduleId) }}</span>
          <span class="rec-status st-0">待审</span>
        </div>
        <div class="rec-line">申请人：{{ row.applicant || row.createBy }}</div>
        <div class="rec-line"><span class="tag-type">{{ adjustTypeText(row.adjustType) }}</span>原：{{ weekText(row.originalWeekDay) }} 第{{ row.originalStartPeriod }}-{{ row.originalEndPeriod }}节 {{ row.originalClassroomName || '' }}</div>
        <div v-if="row.newWeekDay" class="rec-line new">新：{{ weekText(row.newWeekDay) }} 第{{ row.newStartPeriod }}-{{ row.newEndPeriod }}节<template v-if="row.newDate"> · {{ fmtDate(row.newDate) }}</template></div>
        <div class="rec-reason">{{ row.reason }}</div>
        <div class="audit-btns">
          <el-input v-model="row._comment" size="mini" placeholder="审批意见（选填）" class="audit-comment" />
          <div class="audit-ops">
            <el-button size="mini" type="danger" plain @click="doReject(row)">驳回</el-button>
            <el-button size="mini" type="primary" @click="doApprove(row)">通过</el-button>
          </div>
        </div>
      </div>
      <div v-if="!auditLoading && auditList.length < auditTotal" class="load-more" @click="loadMoreAudit">点击加载更多</div>
    </template>

    <!-- 申请弹层 -->
    <div v-if="applyOpen" class="mask" @click.self="applyOpen = false">
      <div class="dialog">
        <div class="dialog-title">调停课申请</div>
        <div class="fi">
          <label>选择排课（本人任课）</label>
          <el-select v-model="form.scheduleId" filterable placeholder="请选择" size="small" style="width:100%">
            <el-option v-for="s in schedules" :key="s.scheduleId" :value="s.scheduleId" :label="scheduleLabel(s)" />
          </el-select>
        </div>
        <div class="fi">
          <label>类型</label>
          <el-radio-group v-model="form.adjustType" size="mini">
            <el-radio-button label="1">调课</el-radio-button>
            <el-radio-button label="2">停课</el-radio-button>
            <el-radio-button label="3">补课</el-radio-button>
          </el-radio-group>
        </div>
        <template v-if="form.adjustType !== '2'">
          <div class="fi fi-row">
            <div class="fi-half">
              <label>新星期</label>
              <el-select v-model="form.newWeekDay" placeholder="周几" size="small" style="width:100%">
                <el-option v-for="w in 7" :key="w" :value="w" :label="weekText(w)" />
              </el-select>
            </div>
            <div class="fi-half">
              <label>节次</label>
              <div class="period-row">
                <el-input-number v-model="form.newStartPeriod" :min="1" :max="12" size="small" controls-position="right" />
                <span>—</span>
                <el-input-number v-model="form.newEndPeriod" :min="1" :max="12" size="small" controls-position="right" />
              </div>
            </div>
          </div>
          <div v-if="form.adjustType === '3'" class="fi">
            <label>补课日期</label>
            <el-date-picker v-model="form.newDate" type="date" value-format="yyyy-MM-dd" placeholder="选填" size="small" style="width:100%" />
          </div>
        </template>
        <div class="fi">
          <label>原因</label>
          <el-input v-model="form.reason" type="textarea" :rows="2" maxlength="500" placeholder="请填写原因" size="small" />
        </div>
        <div class="dialog-actions">
          <el-button size="small" @click="applyOpen = false">取消</el-button>
          <el-button type="primary" size="small" :loading="submitting" @click="submitApply">提交</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { listAdjustments, applyAdjustment, listMySchedules, cancelAdjustment, listPendingAdjustments, approveAdjustment, rejectAdjustment } from '@/api/portal/adjustment'

export default {
  name: 'MobileAdjustment',
  data() {
    return {
      tab: 'mine',
      loading: false,
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 20,
      approveText: { '0': '待审批', '1': '已通过', '2': '已驳回', '3': '已撤销' },
      // 审批
      auditLoading: false,
      auditList: [],
      auditTotal: 0,
      auditPageNum: 1,
      auditDenied: '',
      // 申请表单
      applyOpen: false,
      submitting: false,
      schedules: [],
      form: { scheduleId: null, adjustType: '1', newWeekDay: null, newStartPeriod: 1, newEndPeriod: 2, newDate: '', reason: '' }
    }
  },
  methods: {
    switchTab(t) {
      this.tab = t
      if (t === 'audit' && this.auditList.length === 0 && !this.auditDenied) this.fetchAudit()
      if (t === 'mine' && this.list.length === 0) this.fetch()
    },
    /* 我的申请 */
    fetch() {
      this.loading = true
      listAdjustments({ pageNum: this.pageNum, pageSize: this.pageSize }).then(r => {
        const rows = r.rows || []
        this.list = this.pageNum === 1 ? rows : this.list.concat(rows)
        this.total = r.total || 0
      }).finally(() => { this.loading = false })
    },
    reload() { this.pageNum = 1; this.fetch() },
    loadMore() { this.pageNum++; this.fetch() },
    cancelRow(row) {
      this.$confirm('确定撤销该申请？', '提示', { type: 'warning' }).then(() => {
        cancelAdjustment(row.adjustId).then(() => { this.toast('已撤销'); this.reload() })
      }).catch(() => {})
    },
    /* 待我审批 */
    fetchAudit() {
      this.auditLoading = true
      listPendingAdjustments({ pageNum: this.auditPageNum, pageSize: this.pageSize }).then(r => {
        const rows = (r.rows || []).map(x => Object.assign({}, x, { _comment: '' }))
        this.auditList = this.auditPageNum === 1 ? rows : this.auditList.concat(rows)
        this.auditTotal = r.total || 0
      }).catch(e => {
        this.auditDenied = (e && e.message) || '您不是调停课审批人，无法执行审批'
      }).finally(() => { this.auditLoading = false })
    },
    loadMoreAudit() { this.auditPageNum++; this.fetchAudit() },
    doApprove(row) {
      approveAdjustment(row.adjustId, { approveComment: row._comment || null }).then(() => {
        this.toast('已通过，结果将通知申请人'); this.auditRefresh()
      }).catch(() => {})
    },
    doReject(row) {
      if (!row._comment) { this.toast('驳回请填写意见', 'warning'); return }
      rejectAdjustment(row.adjustId, { approveComment: row._comment }).then(() => {
        this.toast('已驳回'); this.auditRefresh()
      }).catch(() => {})
    },
    auditRefresh() { this.auditPageNum = 1; this.auditList = []; this.fetchAudit() },
    /* 申请弹层 */
    openApply() {
      this.applyOpen = true
      if (this.schedules.length === 0) {
        listMySchedules({ pageNum: 1, pageSize: 300 }).then(r => { this.schedules = r.rows || [] }).catch(() => {})
      }
    },
    submitApply() {
      const f = this.form
      if (!f.scheduleId || !f.adjustType || !f.reason) { this.toast('请选择排课、类型并填写原因', 'warning'); return }
      if (f.adjustType !== '2' && (!f.newWeekDay || !f.newStartPeriod || !f.newEndPeriod)) { this.toast('请填写新的星期与节次', 'warning'); return }
      if (f.adjustType !== '2' && f.newEndPeriod < f.newStartPeriod) { this.toast('结束节次不能小于开始节次', 'warning'); return }
      const payload = { scheduleId: f.scheduleId, adjustType: f.adjustType, reason: f.reason }
      if (f.adjustType !== '2') {
        payload.newWeekDay = f.newWeekDay
        payload.newStartPeriod = f.newStartPeriod
        payload.newEndPeriod = f.newEndPeriod
        if (f.adjustType === '3' && f.newDate) payload.newDate = f.newDate
      }
      this.submitting = true
      applyAdjustment(payload).then(() => {
        this.toast('已提交，待教务处审批，结果将通过站内消息通知')
        this.applyOpen = false
        this.tab = 'mine'
        this.reload()
      }).finally(() => { this.submitting = false })
    },
    scheduleLabel(s) {
      return (s.courseName || ('课程#' + s.offeringId)) + ' · ' + this.weekText(s.weekDay) + ' 第' + s.startPeriod + '-' + s.endPeriod + '节' + (s.classroomName ? ' · ' + s.classroomName : '')
    },
    weekText(w) { return w ? '周' + ['一', '二', '三', '四', '五', '六', '日'][w - 1] : '--' },
    adjustTypeText(t) { return { '1': '调课', '2': '停课', '3': '补课' }[t] || '调停课' },
    fmt(v) { return v && this.parseTime ? this.parseTime(v, '{y}-{m}-{d} {h}:{i}') : (v || '--') },
    fmtDate(v) { return v && this.parseTime ? this.parseTime(v, '{y}-{m}-{d}') : (v || '--') },
    toast(m, type) {
      this.$message ? this.$message({ message: m, type: type || 'success' }) : window.alert(m)
    }
  }
}
</script>

<style scoped>
.mobile-adjust { padding: 12px; }
.seg-bar { display: flex; background: #fff; border-radius: 8px; padding: 4px; margin-bottom: 10px; }
.seg-item { flex: 1; text-align: center; line-height: 32px; font-size: 14px; color: #606266; border-radius: 6px; cursor: pointer; }
.seg-item.active { background: linear-gradient(135deg, #003366, #007ab8); color: #fff; }
.apply-entry { margin-bottom: 10px; }
.submit-btn { width: 100%; }
.loading-state, .empty-state { text-align: center; color: #c0c4cc; padding: 30px 0; font-size: 13px; }
.empty-state i { font-size: 36px; display: block; margin-bottom: 6px; }
.rec-card { background: #fff; border-radius: 8px; padding: 12px; margin-bottom: 10px; }
.rec-top { display: flex; justify-content: space-between; align-items: center; }
.rec-title { font-size: 15px; font-weight: 600; color: #303133; }
.rec-status { font-size: 12px; padding: 2px 8px; border-radius: 10px; flex-shrink: 0; }
.st-0 { background: #fdf6ec; color: #e6a23c; }
.st-1 { background: #f0f9ec; color: #67c23a; }
.st-2 { background: #fef0f0; color: #f56c6c; }
.st-3 { background: #f4f4f5; color: #909399; }
.rec-line { font-size: 12px; color: #606266; margin-top: 6px; }
.rec-line.new { color: #007ab8; }
.tag-type { display: inline-block; background: #eef5fb; color: #007ab8; border-radius: 4px; font-size: 11px; padding: 1px 6px; margin-right: 6px; }
.rec-reason { font-size: 13px; color: #606266; margin-top: 6px; }
.rec-audit { font-size: 12px; color: #909399; margin-top: 4px; }
.rec-actions { border-top: 1px solid #f5f7fa; margin-top: 6px; text-align: right; }
.audit-btns { border-top: 1px solid #f5f7fa; margin-top: 8px; padding-top: 8px; }
.audit-comment { margin-bottom: 6px; }
.audit-ops { display: flex; justify-content: flex-end; gap: 8px; }
.load-more { text-align: center; font-size: 13px; color: #007ab8; padding: 10px 0 20px; cursor: pointer; }
.mask { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.45); z-index: 999; display: flex; align-items: center; justify-content: center; }
.dialog { width: 90%; max-height: 80vh; overflow-y: auto; background: #fff; border-radius: 10px; padding: 16px; }
.dialog-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; }
.fi { margin-bottom: 10px; }
.fi label { display: block; font-size: 13px; color: #606266; margin-bottom: 4px; }
.fi-row { display: flex; gap: 10px; }
.fi-half { flex: 1; }
.period-row { display: flex; align-items: center; gap: 4px; }
.dialog-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px; }
</style>
