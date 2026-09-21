<template>
  <div class="page-container">
    <el-card>
      <div slot="header" class="card-header">
        <span><i class="el-icon-office-building"></i> 教室借用申请</span>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="openApply">新建申请</el-button>
      </div>

      <el-table v-loading="loading" :data="borrowList" border stripe>
        <el-table-column label="教室" min-width="150" show-overflow-tooltip>
          <template slot-scope="scope">
            {{ scope.row.classroomName || ('教室#' + scope.row.classroomId) }}
            <el-tag v-if="scope.row.buildingName" size="mini" type="info">{{ scope.row.buildingName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="借用日期" width="110" align="center">
          <template slot-scope="scope">{{ parseTime(scope.row.borrowDate, '{y}-{m}-{d}') }}</template>
        </el-table-column>
        <el-table-column label="时段" width="120" align="center">
          <template slot-scope="scope">{{ scope.row.startTime || '-' }}~{{ scope.row.endTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="用途" prop="purpose" min-width="140" show-overflow-tooltip />
        <el-table-column label="审批状态" width="110" align="center">
          <template slot-scope="scope">
            <el-tag :type="statusTag(scope.row.approveStatus)" size="small">{{ statusText(scope.row.approveStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批留痕" min-width="160">
          <template slot-scope="scope">
            <div v-if="scope.row.deptApproveBy" class="trace-line">院系：{{ scope.row.deptApproveBy }} {{ scope.row.deptOpinion || '（无意见）' }}</div>
            <div v-if="scope.row.aaApproveBy" class="trace-line">教务处：{{ scope.row.aaApproveBy }} {{ scope.row.aaOpinion || '（无意见）' }}</div>
            <span v-if="!scope.row.deptApproveBy && !scope.row.aaApproveBy" class="trace-line">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center">
          <template slot-scope="scope">
            <el-button v-if="canCancel(scope.row)" type="text" size="mini" @click="handleCancel(scope.row)">撤销</el-button>
            <el-button v-if="scope.row.procInstId" type="text" size="mini" @click="handleTrace(scope.row)">进度</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination background layout="total, prev, pager, next" :total="total"
                     :page-size="queryParams.pageSize" :current-page.sync="queryParams.pageNum"
                     @current-change="getList" style="margin-top:12px;text-align:right" />
    </el-card>

    <!-- 新建申请 -->
    <el-dialog title="教室借用申请" :visible.sync="applyOpen" width="560px" append-to-body>
      <el-form ref="applyForm" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="教室" prop="classroomId">
          <el-select v-model="form.classroomId" filterable placeholder="请选择教室" style="width:100%" @change="preCheck">
            <el-option v-for="c in classrooms" :key="c.classroomId" :value="c.classroomId"
                       :label="c.classroomName + (c.buildingName ? ' · ' + c.buildingName : '') + '（' + (c.capacity || 0) + '人）'" />
          </el-select>
        </el-form-item>
        <el-form-item label="借用日期" prop="borrowDate">
          <el-date-picker v-model="form.borrowDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width:100%" @change="preCheck" />
        </el-form-item>
        <el-form-item label="时段">
          <el-col :span="11"><el-input v-model="form.startTime" placeholder="开始 如08:00" @blur="preCheck" /></el-col>
          <el-col :span="2" style="text-align:center">~</el-col>
          <el-col :span="11"><el-input v-model="form.endTime" placeholder="结束 如10:00" @blur="preCheck" /></el-col>
        </el-form-item>
        <el-form-item label="借用人次" prop="attendeeCount">
          <el-input-number v-model="form.attendeeCount" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="借用用途" prop="purpose">
          <el-input v-model="form.purpose" type="textarea" :rows="2" placeholder="请说明借用用途" />
        </el-form-item>
        <el-alert v-if="conflictTips.length" type="warning" show-icon :closable="false" title="检测到借用冲突"
                  :description="conflictTips.join('；')" />
      </el-form>
      <div slot="footer">
        <el-button @click="applyOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitApply">提交申请</el-button>
      </div>
    </el-dialog>

    <!-- 审批进度 -->
    <el-dialog title="审批进度" :visible.sync="traceOpen" width="560px" append-to-body>
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
    </el-dialog>
  </div>
</template>

<script>
import { listMyBorrow, listBorrowClassrooms, applyBorrow, cancelBorrow, borrowTrace, checkBorrowConflict } from '@/api/portal/borrow'

export default {
  name: 'ClassroomBorrow',
  data() {
    return {
      loading: false,
      total: 0,
      borrowList: [],
      queryParams: { pageNum: 1, pageSize: 10 },
      applyOpen: false,
      submitting: false,
      classrooms: [],
      form: {},
      conflictTips: [],
      traceOpen: false,
      traceTasks: [],
      traceRow: {},
      rules: {
        classroomId: [{ required: true, message: '请选择教室', trigger: 'change' }],
        borrowDate: [{ required: true, message: '请选择借用日期', trigger: 'change' }],
        purpose: [{ required: true, message: '请填写借用用途', trigger: 'blur' }]
      }
    }
  },
  computed: {
    traceActive() {
      const s = this.traceRow.approveStatus
      if (s === '1') return 3
      if (s === '2' || s === '3' || s === '4') return 4
      return 2
    },
    traceFinalTitle() {
      const s = this.traceRow.approveStatus
      if (s === '2') return '已通过'
      if (s === '3') return '已驳回'
      if (s === '4') return '已撤销'
      return '办结'
    }
  },
  created() {
    this.getList()
    listBorrowClassrooms({ pageNum: 1, pageSize: 500 }).then(r => { this.classrooms = r.rows || [] })
  },
  methods: {
    parseTime(v, fmt) {
      if (!v) return '—'
      const d = new Date(v)
      if (isNaN(d.getTime())) return v
      const p = n => String(n).padStart(2, '0')
      const map = { y: d.getFullYear(), m: p(d.getMonth() + 1), d: p(d.getDate()), h: p(d.getHours()), i: p(d.getMinutes()) }
      return (fmt || '{y}-{m}-{d}').replace(/\{([ymdhi])\}/g, (_, k) => map[k])
    },
    getList() {
      this.loading = true
      listMyBorrow(this.queryParams).then(r => {
        this.borrowList = r.rows || []
        this.total = r.total || 0
      }).finally(() => { this.loading = false })
    },
    statusText(v) {
      return { '0': '待院系审核', '1': '待教务处审核', '2': '已通过', '3': '已驳回', '4': '已撤销' }[v] || v
    },
    statusTag(v) {
      return { '0': 'warning', '1': 'warning', '2': 'success', '3': 'danger', '4': 'info' }[v] || 'info'
    },
    canCancel(row) {
      return !!row.procInstId && (row.approveStatus === '0' || row.approveStatus === '1')
    },
    openApply() {
      this.form = { classroomId: null, borrowDate: null, startTime: null, endTime: null, attendeeCount: 0, contactPhone: null, purpose: null }
      this.conflictTips = []
      this.applyOpen = true
    },
    preCheck() {
      this.conflictTips = []
      if (!this.form.classroomId || !this.form.borrowDate) return
      checkBorrowConflict({
        classroomId: this.form.classroomId,
        borrowDate: this.form.borrowDate,
        startTime: this.form.startTime,
        endTime: this.form.endTime
      }).then(r => { this.conflictTips = r.data || [] })
    },
    submitApply() {
      this.$refs.applyForm.validate(valid => {
        if (!valid) return
        this.submitting = true
        applyBorrow(this.form).then(() => {
          this.$message.success('申请已提交，等待院系审批')
          this.applyOpen = false
          this.getList()
        }).finally(() => { this.submitting = false })
      })
    },
    handleCancel(row) {
      this.$confirm('确认撤销该借用申请？', '提示', { type: 'warning' }).then(() => {
        return cancelBorrow(row.borrowId)
      }).then(() => {
        this.$message.success('已撤销')
        this.getList()
      }).catch(() => {})
    },
    handleTrace(row) {
      this.traceRow = row
      borrowTrace(row.borrowId).then(r => {
        const d = r.data || {}
        this.traceTasks = d.tasks || []
        this.traceOpen = true
      })
    }
  }
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.trace-line { font-size: 12px; color: #606266; line-height: 1.6; }
</style>
