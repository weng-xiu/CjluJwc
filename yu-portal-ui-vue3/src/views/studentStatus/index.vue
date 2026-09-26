<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header"><el-icon><Postcard /></el-icon> 学籍服务</div>
      </template>
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="学籍信息" name="info">
          <el-descriptions v-if="studentInfo && studentInfo.studentId" :column="2" border style="margin-top:16px">
            <el-descriptions-item label="学号">{{ studentInfo.studentNo }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ studentInfo.studentName }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ genderText(studentInfo.gender) }}</el-descriptions-item>
            <el-descriptions-item label="院系">{{ studentInfo.deptName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="专业">{{ studentInfo.majorName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="班级">{{ studentInfo.className || '-' }}</el-descriptions-item>
            <el-descriptions-item label="入学年份">{{ studentInfo.enrollmentYear }}</el-descriptions-item>
            <el-descriptions-item label="学历层次">{{ studentInfo.educationLevel || '-' }}</el-descriptions-item>
            <el-descriptions-item label="学籍状态"><el-tag :type="statusColor(studentInfo.studentStatus)" size="small">{{ statusText(studentInfo.studentStatus) }}</el-tag></el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="暂无学籍信息" />
        </el-tab-pane>
        <el-tab-pane label="异动申请" name="apply">
          <el-form ref="applyForm" :model="applyForm" label-width="100px" style="max-width:500px;margin-top:16px">
            <el-form-item label="异动类型" required>
              <el-select v-model="applyForm.changeType" placeholder="请选择" style="width:100%">
                <el-option label="休学" value="0" />
                <el-option label="复学" value="1" />
                <el-option label="退学" value="3" />
              </el-select>
            </el-form-item>
            <el-form-item label="生效日期">
              <!-- Vue3/element-plus 迁移：日期格式 token 由 yyyy-MM-dd 改为 dayjs 的 YYYY-MM-DD -->
              <el-date-picker v-model="applyForm.changeDate" type="date" value-format="YYYY-MM-DD" placeholder="默认为提交日期" style="width:100%" />
            </el-form-item>
            <el-form-item label="申请原因" required>
              <el-input v-model="applyForm.reason" type="textarea" rows="4" maxlength="500" show-word-limit placeholder="请详细说明申请原因" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="applying" @click="submitApply">提交申请</el-button>
              <span style="margin-left:12px;color:#909399;font-size:12px">提交后进入院系、教务处两级审批，可在"申请记录"中查看进度</span>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="申请记录" name="records">
          <el-table :data="changeList" border stripe v-loading="recordLoading">
            <el-table-column label="异动类型" width="100" align="center">
              <template #default="scope">{{ changeTypeText(scope.row.changeType) }}</template>
            </el-table-column>
            <el-table-column label="申请原因" prop="reason" min-width="180" show-overflow-tooltip />
            <el-table-column label="申请时间" width="160" align="center">
              <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="审批状态" width="100" align="center">
              <template #default="scope"><el-tag :type="approveColor(scope.row.approveStatus)" size="small">{{ approveText(scope.row.approveStatus) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="审批意见" prop="approveOpinion" min-width="140" show-overflow-tooltip />
            <el-table-column label="操作" width="140" align="center">
              <template #default="scope">
                <el-button size="small" link type="primary" icon="View" @click="openTrace(scope.row)">进度</el-button>
                <el-button v-if="scope.row.approveStatus === '0'" size="small" link type="danger" icon="Delete" @click="handleCancel(scope.row)">撤销</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!recordLoading && !changeList.length" description="暂无异动申请记录" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 审批进度 -->
    <el-dialog title="审批进度" v-model="traceOpen" width="560px" append-to-body>
      <template v-if="traceData">
        <el-steps :active="traceActive" finish-status="success" align-center style="margin-bottom:18px">
          <el-step title="提交申请" />
          <el-step title="院系审批" />
          <el-step title="教务处审批" />
          <el-step :title="traceFinalTitle" />
        </el-steps>
        <el-timeline v-if="traceTasks.length">
          <el-timeline-item v-for="(t, i) in traceTasks" :key="i"
                            :timestamp="parseTime(t.endTime || t.startTime, '{y}-{m}-{d} {h}:{i}')"
                            :type="t.endTime ? 'success' : 'primary'" :hollow="!t.endTime">
            <b>{{ t.taskName }}</b><span v-if="t.assignee" style="color:#909399;margin-left:8px">{{ t.assignee }}</span>
            <div v-if="t.comments && t.comments.length" style="color:#606266">{{ t.comments.join(' / ') }}</div>
            <div v-if="!t.endTime" style="color:#E6A23C">进行中…</div>
          </el-timeline-item>
        </el-timeline>
      </template>
      <el-empty v-else :description="traceFallback" />
    </el-dialog>
  </div>
</template>
<script>
import { getStudentInfo, applyStatusChange, listStatusChanges, traceStatusChange, cancelStatusChange } from '@/api/portal/studentStatus'
export default {
  name: 'StudentStatus',
  data() {
    return {
      activeTab: 'info', studentInfo: {}, applying: false,
      applyForm: { changeType: '', changeDate: '', reason: '' },
      changeList: [], recordLoading: false,
      traceOpen: false, traceData: null, traceFallback: '该申请尚未进入审批流程'
    }
  },
  computed: {
    traceTasks() { return (this.traceData && this.traceData.tasks) || [] },
    traceActive() {
      if (!this.traceData) return 0
      if (this.traceData.endTime) return 4
      return 1 + this.traceTasks.filter(t => t.endTime).length
    },
    traceFinalTitle() {
      const s = this.traceData && this.traceData.deleteReason ? this.traceData.deleteReason : ''
      return s.indexOf('撤销') >= 0 ? '已撤销' : '审批结束'
    }
  },
  created() { this.fetchInfo() },
  methods: {
    fetchInfo() { getStudentInfo().then(r => { this.studentInfo = r.data || {} }) },
    handleTabClick(tab) { if (tab.paneName === 'records') this.fetchRecords() },
    submitApply() {
      if (!this.applyForm.changeType || !this.applyForm.reason) { this.$message.warning('请填写完整信息'); return }
      this.applying = true
      applyStatusChange(this.applyForm).then(() => {
        this.$message.success('申请已提交，已进入审批流程')
        this.applyForm = { changeType: '', changeDate: '', reason: '' }
        this.activeTab = 'records'; this.fetchRecords()
      }).finally(() => { this.applying = false })
    },
    fetchRecords() { this.recordLoading = true; listStatusChanges({ pageNum: 1, pageSize: 20 }).then(r => { this.changeList = r.rows || [] }).finally(() => { this.recordLoading = false }) },
    openTrace(row) {
      this.traceData = null; this.traceFallback = '该申请尚未进入审批流程'
      traceStatusChange(row.changeId).then(r => {
        if (r.data) { this.traceData = r.data } else { this.traceFallback = '未进入线上审批流程（历史申请），当前状态：' + this.approveText(row.approveStatus) }
        this.traceOpen = true
      })
    },
    handleCancel(row) {
      this.$confirm('确认撤销该异动申请吗？撤销后审批流程将终止。', '提示', { type: 'warning' }).then(() => {
        cancelStatusChange(row.changeId).then(() => { this.$message.success('已撤销'); this.fetchRecords() })
      }).catch(() => {})
    },
    genderText(g) { return { '0': '男', '1': '女' }[g] || '-' },
    statusText(s) { return { '0': '在读', '1': '休学', '2': '退学', '3': '毕业', '4': '转出', '5': '保留学籍' }[s] || '-' },
    statusColor(s) { return { '0': 'success', '1': 'warning', '2': 'danger', '3': 'info', '4': 'info', '5': 'warning' }[s] || 'info' },
    changeTypeText(t) { return { '0': '休学', '1': '复学', '2': '转学', '3': '退学', '4': '保留学籍' }[t] || t },
    approveText(s) { return { '0': '审核中', '1': '已通过', '2': '已驳回', '3': '已撤销' }[s] || s },
    approveColor(s) { return { '0': 'warning', '1': 'success', '2': 'danger', '3': 'info' }[s] || 'info' }
  }
}
</script>
<style scoped>.page-container { max-width: 1200px; margin: 0 auto; }.card-header { font-size: 16px; font-weight: 600; }</style>
