<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header"><el-icon><Refresh /></el-icon> 调停课申请</div>
      </template>
      <div style="margin-bottom:16px"><el-button type="primary" icon="Plus" @click="openApply">新建申请</el-button></div>
      <el-table v-loading="loading" :data="adjustmentList" border stripe>
        <el-table-column label="课程名称" prop="courseName" min-width="130" show-overflow-tooltip />
        <el-table-column label="申请类型" width="90" align="center">
          <template #default="scope"><el-tag :type="typeColor(scope.row.adjustType)" size="small">{{ typeText(scope.row.adjustType) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="原课表安排" min-width="150" align="center">
          <template #default="scope">{{ scheduleText(scope.row.originalWeekDay, scope.row.originalStartPeriod, scope.row.originalEndPeriod) }}<span v-if="scope.row.originalClassroomName"> · {{ scope.row.originalClassroomName }}</span></template>
        </el-table-column>
        <el-table-column label="调整后安排" min-width="150" align="center">
          <template #default="scope">{{ adjustTypeText(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="申请原因" prop="reason" min-width="140" show-overflow-tooltip />
        <el-table-column label="申请时间" width="150" align="center">
          <template #default="scope">{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</template>
        </el-table-column>
        <el-table-column label="审批状态" width="90" align="center">
          <template #default="scope"><el-tag :type="statusColor(scope.row.approveStatus)" size="small">{{ statusText(scope.row.approveStatus) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="审批结果" min-width="130" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.approveStatus !== '0' && scope.row.approveStatus !== '3'">{{ scope.row.approveBy }} · {{ parseTime(scope.row.approveTime, '{y}-{m}-{d} {h}:{i}') }}</span>
            <span v-else style="color:#909399">{{ scope.row.approveTime ? '已撤销 ' + parseTime(scope.row.approveTime, '{y}-{m}-{d}') : '待教务处审批' }}</span>
            <div v-if="scope.row.approveComment" style="color:#606266;font-size:12px">{{ scope.row.approveComment }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="scope">
            <el-button v-if="scope.row.approveStatus === '0'" size="small" link type="danger" icon="Delete" @click="handleCancel(scope.row)">撤销</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      <el-empty v-if="!loading && !adjustmentList.length" description="暂无调停课申请" />
    </el-card>
    <!-- 新建申请（Vue3 迁移：:visible.sync → v-model，slot="footer" → #footer，el-radio label → value） -->
    <el-dialog title="调停课申请" v-model="dialogVisible" width="560px" :close-on-click-modal="false">
      <el-form ref="applyForm" :model="applyForm" label-width="110px" :rules="rules">
        <el-form-item label="原排课" prop="scheduleId">
          <el-select v-model="applyForm.scheduleId" placeholder="请选择本人任课的排课" filterable style="width:100%" @change="onScheduleChange">
            <el-option v-for="s in mySchedules" :key="s.scheduleId" :label="scheduleOption(s)" :value="s.scheduleId" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请类型" prop="adjustType">
          <el-radio-group v-model="applyForm.adjustType">
            <el-radio value="1">调课</el-radio><el-radio value="2">停课</el-radio><el-radio value="3">补课</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="applyForm.adjustType !== '2'">
          <el-form-item label="新星期" prop="newWeekDay">
            <el-select v-model="applyForm.newWeekDay" placeholder="请选择星期" style="width:100%">
              <el-option v-for="d in weekOptions" :key="d.value" :label="d.label" :value="d.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="新旧节次" prop="newStartPeriod">
            <el-input-number v-model="applyForm.newStartPeriod" :min="1" :max="12" controls-position="right" style="width:47%" />
            <span style="margin:0 3%">至</span>
            <el-input-number v-model="applyForm.newEndPeriod" :min="1" :max="12" controls-position="right" style="width:47%" />
          </el-form-item>
          <div style="margin:0 0 14px 110px;color:#909399;font-size:12px">教室默认沿用原教室，如需更换请在申请原因中说明，由教务处审批时调整</div>
        </template>
        <el-form-item label="申请日期" prop="newDate" v-if="applyForm.adjustType === '3'">
          <el-date-picker v-model="applyForm.newDate" type="date" value-format="YYYY-MM-DD" placeholder="选择补课日期" style="width:100%" />
        </el-form-item>
        <el-form-item label="申请原因" prop="reason">
          <el-input v-model="applyForm.reason" type="textarea" rows="3" maxlength="500" show-word-limit placeholder="请详细说明调停课原因" />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="applying" @click="submitApply">提交申请</el-button></template>
    </el-dialog>
  </div>
</template>
<script>
import { listAdjustments, applyAdjustment, listMySchedules, cancelAdjustment } from '@/api/portal/adjustment'
export default {
  name: 'TeacherAdjustment',
  data() {
    return {
      loading: false, total: 0, adjustmentList: [], queryParams: { pageNum: 1, pageSize: 10 },
      dialogVisible: false, applying: false, mySchedules: [],
      weekOptions: [ { value: 1, label: '周一' }, { value: 2, label: '周二' }, { value: 3, label: '周三' }, { value: 4, label: '周四' }, { value: 5, label: '周五' }, { value: 6, label: '周六' }, { value: 7, label: '周日' } ],
      emptyForm: { scheduleId: '', adjustType: '1', newWeekDay: null, newStartPeriod: null, newEndPeriod: null, newDate: '', reason: '' },
      applyForm: { scheduleId: '', adjustType: '1', newWeekDay: null, newStartPeriod: null, newEndPeriod: null, newDate: '', reason: '' },
      rules: {
        scheduleId: [{ required: true, message: '请选择排课', trigger: 'change' }],
        adjustType: [{ required: true, message: '请选择类型', trigger: 'change' }],
        newWeekDay: [{ required: true, message: '请选择新星期', trigger: 'change' }],
        newStartPeriod: [{ required: true, validator: this.validatePeriod, trigger: 'change' }],
        reason: [{ required: true, message: '请输入原因', trigger: 'blur' }]
      }
    }
  },
  created() { this.fetchSchedules(); this.getList() },
  methods: {
    validatePeriod(rule, value, callback) {
      const f = this.applyForm
      if (f.adjustType !== '2' && (f.newStartPeriod == null || f.newEndPeriod == null || f.newEndPeriod < f.newStartPeriod)) {
        callback(new Error('请填写正确的节次区间'))
      } else { callback() }
    },
    fetchSchedules() { listMySchedules({ pageNum: 1, pageSize: 200 }).then(r => { this.mySchedules = r.rows || [] }) },
    getList() { this.loading = true; listAdjustments(this.queryParams).then(r => { this.adjustmentList = r.rows || []; this.total = r.total || 0 }).finally(() => { this.loading = false }) },
    openApply() { this.applyForm = JSON.parse(JSON.stringify(this.emptyForm)); this.dialogVisible = true },
    onScheduleChange(id) {
      const s = this.mySchedules.find(x => x.scheduleId === id)
      if (s) { this.applyForm.newWeekDay = s.weekDay; this.applyForm.newStartPeriod = s.startPeriod; this.applyForm.newEndPeriod = s.endPeriod }
    },
    submitApply() {
      this.$refs.applyForm.validate(valid => {
        if (!valid) return
        const f = this.applyForm
        const payload = { scheduleId: f.scheduleId, adjustType: f.adjustType, reason: f.reason }
        if (f.adjustType === '3' && f.newDate) { payload.newDate = f.newDate }
        if (f.adjustType !== '2') {
          payload.newWeekDay = f.newWeekDay
          payload.newStartPeriod = f.newStartPeriod
          payload.newEndPeriod = f.newEndPeriod
        }
        this.applying = true
        applyAdjustment(payload).then(() => { this.$message.success('申请已提交，待教务处审批，结果将通过站内消息通知'); this.dialogVisible = false; this.fetchSchedules(); this.getList() })
          .finally(() => { this.applying = false })
      })
    },
    handleCancel(row) {
      this.$confirm('确认撤销该调停课申请吗？', '提示', { type: 'warning' }).then(() => {
        cancelAdjustment(row.adjustId).then(() => { this.$message.success('已撤销'); this.getList() })
      }).catch(() => {})
    },
    scheduleOption(s) {
      return (s.courseName || ('排课#' + s.scheduleId)) + ' · ' + this.scheduleText(s.weekDay, s.startPeriod, s.endPeriod) + (s.classroomName ? ' · ' + s.classroomName : '')
    },
    scheduleText(weekDay, start, end) {
      if (weekDay == null) return '-'
      const w = this.weekOptions.find(d => d.value === weekDay)
      return (w ? w.label : '周' + weekDay) + ' ' + (start || '?') + '-' + (end || '?') + '节'
    },
    adjustTypeText(row) {
      if (row.adjustType === '2') return '停上本次课'
      if (row.newWeekDay == null) return row.adjustType === '3' && row.newDate ? '补课 ' + this.parseTime(row.newDate, '{y}-{m}-{d}') : '-'
      const parts = []
      parts.push(this.scheduleText(row.newWeekDay, row.newStartPeriod, row.newEndPeriod))
      if (row.newClassName) parts.push(row.newClassName)
      if (row.adjustType === '3' && row.newDate) parts.push(this.parseTime(row.newDate, '{y}-{m}-{d}'))
      return parts.join(' · ')
    },
    typeText(t) { return { '1': '调课', '2': '停课', '3': '补课' }[t] || t },
    typeColor(t) { return { '1': 'primary', '2': 'danger', '3': 'success' }[t] || 'info' },
    statusText(s) { return { '0': '待审批', '1': '已通过', '2': '已驳回', '3': '已撤销' }[s] || s },
    statusColor(s) { return { '0': 'warning', '1': 'success', '2': 'danger', '3': 'info' }[s] || 'info' }
  }
}
</script>
<style scoped>.page-container { max-width: 1200px; margin: 0 auto; }.card-header { font-size: 16px; font-weight: 600; }</style>
