<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="申请人" prop="applicant">
        <el-input v-model="queryParams.applicant" placeholder="请输入申请人" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="审批状态" prop="approveStatus">
        <el-select v-model="queryParams.approveStatus" placeholder="请选择审批状态" clearable>
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['brm:borrow:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['brm:borrow:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="el-icon-date" size="mini" @click="handleOccupancy" v-hasPermi="['brm:borrow:list']">占用日历</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['brm:borrow:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="borrowList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="教室" align="center" min-width="150" show-overflow-tooltip>
        <template slot-scope="scope">
          <span>{{ scope.row.classroomName || ('教室#' + scope.row.classroomId) }}</span>
          <el-tag v-if="scope.row.buildingName" size="mini" type="info" style="margin-left:4px">{{ scope.row.buildingName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请人" align="center" prop="applicant" width="100" />
      <el-table-column label="申请部门" align="center" prop="applicantDept" width="140" show-overflow-tooltip />
      <el-table-column label="借用日期" align="center" width="110">
        <template slot-scope="scope">{{ parseTime(scope.row.borrowDate, '{y}-{m}-{d}') }}</template>
      </el-table-column>
      <el-table-column label="时段" align="center" width="120">
        <template slot-scope="scope">{{ scope.row.startTime || '-' }} ~ {{ scope.row.endTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="用途" align="center" prop="purpose" min-width="120" show-overflow-tooltip />
      <el-table-column label="审批状态" align="center" width="110">
        <template slot-scope="scope">
          <el-tag :type="statusTag(scope.row.approveStatus)" size="mini">{{ statusText(scope.row.approveStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button v-if="canSubmit(scope.row)" size="mini" type="text" icon="el-icon-s-promotion"
                     @click="handleSubmit(scope.row)" v-hasPermi="['brm:borrow:submit']">提交审批</el-button>
          <el-button v-if="scope.row.approveStatus === '0' && scope.row.procInstId" size="mini" type="text" icon="el-icon-s-check"
                     @click="openApprove(scope.row, 'dept')" v-hasPermi="['brm:borrow:approve']">院系审批</el-button>
          <el-button v-if="scope.row.approveStatus === '1'" size="mini" type="text" icon="el-icon-s-check"
                     @click="openApprove(scope.row, 'aa')" v-hasPermi="['brm:borrow:approve']">教务处审批</el-button>
          <el-button v-if="canCancel(scope.row)" size="mini" type="text" icon="el-icon-refresh-left"
                     @click="handleCancel(scope.row)" v-hasPermi="['brm:borrow:submit']">撤销</el-button>
          <el-button v-if="scope.row.procInstId" size="mini" type="text" icon="el-icon-time"
                     @click="handleTrace(scope.row)">追溯</el-button>
          <el-button v-if="canEdit(scope.row)" size="mini" type="text" icon="el-icon-edit"
                     @click="handleUpdate(scope.row)" v-hasPermi="['brm:borrow:edit']">修改</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>

    <!-- 新增/修改申请 -->
    <el-dialog :title="title" :visible.sync="open" width="620px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="教室" prop="classroomId">
          <el-select v-model="form.classroomId" filterable placeholder="请选择教室" style="width:100%" @change="preCheckConflict">
            <el-option v-for="c in classroomOptions" :key="c.classroomId" :value="c.classroomId"
                       :label="c.classroomName + (c.buildingName ? ' · ' + c.buildingName : '') + '（' + (c.capacity || 0) + '人）'" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请人" prop="applicant">
          <el-input v-model="form.applicant" placeholder="请输入申请人" />
        </el-form-item>
        <el-form-item label="申请部门" prop="applicantDept">
          <el-input v-model="form.applicantDept" placeholder="请输入申请部门" />
        </el-form-item>
        <el-form-item label="借用日期" prop="borrowDate">
          <el-date-picker clearable v-model="form.borrowDate" type="date" value-format="yyyy-MM-dd"
                          placeholder="请选择借用日期" style="width:100%" @change="preCheckConflict"/>
        </el-form-item>
        <el-form-item label="时段">
          <el-col :span="11">
            <el-form-item prop="startTime" label-width="0">
              <el-input v-model="form.startTime" placeholder="开始 如08:00" @blur="preCheckConflict"/>
            </el-form-item>
          </el-col>
          <el-col :span="2" style="text-align:center">~</el-col>
          <el-col :span="11">
            <el-form-item prop="endTime" label-width="0">
              <el-input v-model="form.endTime" placeholder="结束 如10:00" @blur="preCheckConflict"/>
            </el-form-item>
          </el-col>
        </el-form-item>
        <el-form-item label="借用人次" prop="attendeeCount">
          <el-input-number v-model="form.attendeeCount" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="借用用途" prop="purpose">
          <el-input v-model="form.purpose" type="textarea" placeholder="请输入借用用途" />
        </el-form-item>
        <el-alert v-if="conflictTips.length" type="warning" show-icon :closable="false" title="检测到借用冲突"
                  :description="conflictTips.join('；')" style="margin-top:6px" />
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 审批对话框 -->
    <el-dialog :title="approveTitle" :visible.sync="approveOpen" width="520px" append-to-body>
      <el-descriptions :column="1" border size="small" style="margin-bottom:12px">
        <el-descriptions-item label="教室">{{ approveRow.classroomName || approveRow.classroomId }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ approveRow.applicant }}</el-descriptions-item>
        <el-descriptions-item label="借用时段">
          {{ parseTime(approveRow.borrowDate, '{y}-{m}-{d}') }} {{ approveRow.startTime || '' }}~{{ approveRow.endTime || '' }}
        </el-descriptions-item>
        <el-descriptions-item label="用途">{{ approveRow.purpose }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="80px">
        <el-form-item label="结果">
          <el-radio-group v-model="approveForm.approved">
            <el-radio :label="true">通过</el-radio>
            <el-radio :label="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="意见">
          <el-input v-model="approveForm.opinion" type="textarea" :rows="3" placeholder="请输入审批意见（可选）" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="doApprove">确 定</el-button>
        <el-button @click="approveOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 全流程追溯 -->
    <el-dialog title="审批流程追溯" :visible.sync="traceOpen" width="640px" append-to-body>
      <el-timeline v-if="traceTasks.length">
        <el-timeline-item v-for="(t, i) in traceTasks" :key="i"
                          :timestamp="parseTime(t.endTime || t.startTime, '{y}-{m}-{d} {h}:{i}')"
                          :type="t.endTime ? 'success' : 'primary'" :hollow="!t.endTime">
          <div><b>{{ t.taskName }}</b><span v-if="t.assignee" style="color:#909399;margin-left:8px">办理人：{{ t.assignee }}</span></div>
          <div v-if="t.comments && t.comments.length" style="color:#606266">意见：{{ t.comments.join(' / ') }}</div>
          <div v-if="!t.endTime" style="color:#E6A23C">进行中…</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无流程记录" :image-size="60" />
    </el-dialog>

    <!-- 教室占用日历 -->
    <el-dialog title="教室占用日历" :visible.sync="occOpen" width="760px" append-to-body>
      <el-form :inline="true" size="small">
        <el-form-item label="教室">
          <el-select v-model="occQuery.classroomId" filterable placeholder="选择教室" style="width:240px" @change="loadOccupancy">
            <el-option v-for="c in classroomOptions" :key="c.classroomId" :value="c.classroomId"
                       :label="c.classroomName + (c.buildingName ? ' · ' + c.buildingName : '')" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期区间">
          <el-date-picker v-model="occRange" type="daterange" value-format="yyyy-MM-dd"
                          start-placeholder="开始" end-placeholder="结束" style="width:240px" @change="loadOccupancy"/>
        </el-form-item>
      </el-form>
      <el-row :gutter="16">
        <el-col :span="12">
          <div class="occ-title">每周固定排课占用</div>
          <el-table :data="occupancy.weeklySchedule || []" size="mini" max-height="260" border>
            <el-table-column label="星期" align="center" width="70">
              <template slot-scope="scope">周{{ weekText(scope.row.weekDay) }}</template>
            </el-table-column>
            <el-table-column label="节次" align="center" width="90">
              <template slot-scope="scope">{{ scope.row.startPeriod }}-{{ scope.row.endPeriod }}</template>
            </el-table-column>
            <el-table-column label="课程" prop="courseName" show-overflow-tooltip />
            <el-table-column label="教师" prop="teacherName" width="80" />
          </el-table>
        </el-col>
        <el-col :span="12">
          <div class="occ-title">区间内借用占用（待审/审批中/已通过）</div>
          <el-table :data="occupancy.borrows || []" size="mini" max-height="260" border>
            <el-table-column label="日期" align="center" width="95">
              <template slot-scope="scope">{{ parseTime(scope.row.borrowDate, '{m}-{d}') }}</template>
            </el-table-column>
            <el-table-column label="时段" align="center" width="110">
              <template slot-scope="scope">{{ scope.row.startTime || '-' }}~{{ scope.row.endTime || '-' }}</template>
            </el-table-column>
            <el-table-column label="申请人" prop="applicant" width="80" />
            <el-table-column label="用途" prop="purpose" show-overflow-tooltip />
            <el-table-column label="状态" align="center" width="90">
              <template slot-scope="scope">
                <el-tag :type="statusTag(scope.row.approveStatus)" size="mini">{{ statusText(scope.row.approveStatus) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script>
import { listBorrow, getBorrow, delBorrow, addBorrow, updateBorrow,
         submitBorrow, deptApproveBorrow, aaApproveBorrow, cancelBorrow,
         checkBorrowConflict, borrowOccupancy, borrowTrace } from "@/api/brm/borrow"
import { listClassroom } from "@/api/brm/classroom"

export default {
  name: "Borrow",
  data() {
    return {
      loading: true,
      ids: [],
      multiple: true,
      showSearch: true,
      total: 0,
      borrowList: [],
      title: "",
      open: false,
      classroomOptions: [],
      statusOptions: [
        { label: "待院系审核", value: "0" },
        { label: "待教务处审核", value: "1" },
        { label: "已通过", value: "2" },
        { label: "已驳回", value: "3" },
        { label: "已撤销", value: "4" }
      ],
      queryParams: { pageNum: 1, pageSize: 10, applicant: null, approveStatus: null },
      form: {},
      conflictTips: [],
      // 审批
      approveOpen: false,
      approveStage: "dept",
      approveRow: {},
      approveForm: { approved: true, opinion: null },
      // 追溯
      traceOpen: false,
      traceTasks: [],
      // 占用日历
      occOpen: false,
      occRange: [],
      occQuery: { classroomId: null },
      occupancy: {},
      rules: {
        classroomId: [{ required: true, message: "教室不能为空", trigger: "change" }],
        applicant: [{ required: true, message: "申请人不能为空", trigger: "blur" }],
        borrowDate: [{ required: true, message: "借用日期不能为空", trigger: "change" }]
      }
    }
  },
  computed: {
    approveTitle() {
      return this.approveStage === 'dept' ? "院系初审" : "教务处终审"
    }
  },
  created() {
    this.getList()
    listClassroom({ pageNum: 1, pageSize: 500, status: "0" }).then(r => {
      this.classroomOptions = r.rows || []
    })
  },
  methods: {
    getList() {
      this.loading = true
      listBorrow(this.queryParams).then(response => {
        this.borrowList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    statusText(v) {
      const s = this.statusOptions.find(x => x.value === v)
      return s ? s.label : (v == null ? "待提交" : v)
    },
    statusTag(v) {
      return { "0": "warning", "1": "warning", "2": "success", "3": "danger", "4": "info" }[v] || "info"
    },
    weekText(w) {
      return { 1: "一", 2: "二", 3: "三", 4: "四", 5: "五", 6: "六", 7: "日" }[w] || w
    },
    canSubmit(row) {
      return row.approveStatus === "0" && !row.procInstId
    },
    canCancel(row) {
      return !!row.procInstId && (row.approveStatus === "0" || row.approveStatus === "1")
    },
    canEdit(row) {
      return row.approveStatus === "0" && !row.procInstId
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        borrowId: null, classroomId: null, applicant: null, applicantDept: null,
        borrowDate: null, startTime: null, endTime: null, purpose: null,
        contactPhone: null, attendeeCount: 0, approveStatus: "0", status: "0"
      }
      this.conflictTips = []
      this.resetForm("form")
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.borrowId)
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "新增教室借用申请"
    },
    handleUpdate(row) {
      this.reset()
      getBorrow(row.borrowId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改教室借用申请"
      })
    },
    // 冲突预检（表单内提示，不阻断保存）
    preCheckConflict() {
      this.conflictTips = []
      if (!this.form.classroomId || !this.form.borrowDate) return
      checkBorrowConflict({
        classroomId: this.form.classroomId,
        borrowDate: this.form.borrowDate,
        startTime: this.form.startTime,
        endTime: this.form.endTime,
        excludeBorrowId: this.form.borrowId || undefined
      }).then(r => {
        this.conflictTips = r.data || []
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) return
        if (this.form.borrowId != null) {
          updateBorrow(this.form).then(() => {
            this.$modal.msgSuccess("修改成功")
            this.open = false
            this.getList()
          })
        } else {
          addBorrow(this.form).then(() => {
            this.$modal.msgSuccess("新增成功，请点击“提交审批”进入流程")
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleSubmit(row) {
      this.$modal.confirm('确认提交借用申请（编号' + row.borrowId + '）进入审批流程？').then(() => {
        return submitBorrow(row.borrowId)
      }).then(() => {
        this.$modal.msgSuccess("已提交，等待院系审批")
        this.getList()
      }).catch(() => {})
    },
    handleCancel(row) {
      this.$modal.confirm('确认撤销该借用申请？').then(() => {
        return cancelBorrow(row.borrowId)
      }).then(() => {
        this.$modal.msgSuccess("已撤销")
        this.getList()
      }).catch(() => {})
    },
    openApprove(row, stage) {
      this.approveRow = row
      this.approveStage = stage
      this.approveForm = { approved: true, opinion: null }
      this.approveOpen = true
    },
    doApprove() {
      const data = { approved: this.approveForm.approved, opinion: this.approveForm.opinion }
      const fn = this.approveStage === 'dept' ? deptApproveBorrow : aaApproveBorrow
      fn(this.approveRow.borrowId, data).then(() => {
        this.$modal.msgSuccess(this.approveForm.approved ? "已通过" : "已驳回")
        this.approveOpen = false
        this.getList()
      })
    },
    handleTrace(row) {
      borrowTrace(row.borrowId).then(r => {
        const d = r.data || {}
        this.traceTasks = d.tasks || []
        this.traceOpen = true
      })
    },
    handleOccupancy() {
      this.occOpen = true
      this.occupancy = {}
      const now = new Date()
      const fmt = d => d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
      const begin = new Date(now.getFullYear(), now.getMonth(), 1)
      const end = new Date(now.getFullYear(), now.getMonth() + 1, 0)
      this.occRange = [fmt(begin), fmt(end)]
      if (!this.occQuery.classroomId && this.classroomOptions.length) {
        this.occQuery.classroomId = this.classroomOptions[0].classroomId
      }
      this.loadOccupancy()
    },
    loadOccupancy() {
      if (!this.occQuery.classroomId) return
      borrowOccupancy({
        classroomId: this.occQuery.classroomId,
        beginDate: this.occRange && this.occRange[0],
        endDate: this.occRange && this.occRange[1]
      }).then(r => {
        this.occupancy = r.data || {}
      })
    },
    handleDelete(row) {
      const borrowIds = row.borrowId || this.ids
      this.$modal.confirm('是否确认删除借用编号为"' + borrowIds + '"的数据项？').then(function() {
        return delBorrow(borrowIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('brm/borrow/export', {
        ...this.queryParams
      }, `borrow_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style scoped>
.occ-title { font-weight: bold; margin: 6px 0; color: #303133; }
</style>
