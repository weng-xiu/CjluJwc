<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="调整类型" prop="adjustType">
        <el-select v-model="queryParams.adjustType" placeholder="请选择调整类型" clearable>
          <el-option v-for="dict in dict.type.tpm_adjust_type" :key="dict.value" :label="dict.label" :value="dict.value"/>
        </el-select>
      </el-form-item>
      <el-form-item label="审批状态" prop="approveStatus">
        <el-select v-model="queryParams.approveStatus" placeholder="请选择审批状态" clearable>
          <el-option v-for="dict in dict.type.tpm_approve_status" :key="dict.value" :label="dict.label" :value="dict.value"/>
        </el-select>
      </el-form-item>
      <el-form-item label="课程名称" prop="courseName">
        <el-input v-model="queryParams.courseName" placeholder="请输入课程名称" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['tpm:adjust:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['tpm:adjust:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['tpm:adjust:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['tpm:adjust:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="adjustList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="调课类型" align="center" prop="adjustType" width="90">
        <template slot-scope="scope"><dict-tag :options="dict.type.tpm_adjust_type" :value="scope.row.adjustType"/></template>
      </el-table-column>
      <el-table-column label="课程名称" align="center" prop="courseName" min-width="120" show-overflow-tooltip />
      <el-table-column label="原教室" align="center" prop="originalClassroomName" width="120" show-overflow-tooltip />
      <el-table-column label="原时间" align="center" width="140">
        <template slot-scope="scope">
          <span>{{ formatTime(scope.row.originalWeekDay, scope.row.originalStartPeriod, scope.row.originalEndPeriod) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="新教室" align="center" prop="newClassName" width="120" show-overflow-tooltip />
      <el-table-column label="新时间" align="center" width="140">
        <template slot-scope="scope">
          <span>{{ formatTime(scope.row.newWeekDay, scope.row.newStartPeriod, scope.row.newEndPeriod) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="申请人" align="center" prop="applicant" width="100" />
      <el-table-column label="审批状态" align="center" prop="approveStatus" width="90">
        <template slot-scope="scope"><dict-tag :options="dict.type.tpm_approve_status" :value="scope.row.approveStatus"/></template>
      </el-table-column>
      <el-table-column label="审批人" align="center" prop="approveBy" width="100" />
      <el-table-column label="审批时间" align="center" prop="approveTime" width="160">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.approveTime) }}</span></template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220">
        <template slot-scope="scope">
          <el-button v-if="scope.row.approveStatus === '0'" size="mini" type="text" icon="el-icon-check" @click="handleApprove(scope.row)" v-hasPermi="['tpm:adjust:edit']">通过</el-button>
          <el-button v-if="scope.row.approveStatus === '0'" size="mini" type="text" icon="el-icon-close" @click="handleReject(scope.row)" v-hasPermi="['tpm:adjust:edit']">驳回</el-button>
          <el-button v-if="scope.row.approveStatus === '0'" size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['tpm:adjust:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['tpm:adjust:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="680px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="排课" prop="scheduleId">
          <el-select v-model="form.scheduleId" placeholder="请选择排课（课程名 教室 周次节次）" filterable clearable style="width:100%">
            <el-option
              v-for="item in scheduleOptions"
              :key="item.scheduleId"
              :label="scheduleLabel(item)"
              :value="item.scheduleId" />
          </el-select>
        </el-form-item>
        <el-form-item label="调课类型" prop="adjustType">
          <el-select v-model="form.adjustType" placeholder="请选择调课类型" style="width:100%">
            <el-option v-for="dict in dict.type.tpm_adjust_type" :key="dict.value" :label="dict.label" :value="dict.value"/>
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="原日期" prop="originalDate">
              <el-date-picker clearable v-model="form.originalDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择原日期" style="width:100%"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="新日期" prop="newDate">
              <el-date-picker clearable v-model="form.newDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择新日期" style="width:100%"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="新教室" prop="newClassroomId">
          <el-select v-model="form.newClassroomId" placeholder="请选择新教室" filterable clearable style="width:100%">
            <el-option v-for="item in classroomOptions" :key="item.classroomId" :label="item.classroomName" :value="item.classroomId" />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="新星期" prop="newWeekDay">
              <el-select v-model="form.newWeekDay" placeholder="请选择" clearable style="width:100%">
                <el-option v-for="d in weekDayOptions" :key="d.value" :label="d.label" :value="d.value"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="开始节次" prop="newStartPeriod">
              <el-input-number v-model="form.newStartPeriod" :min="1" :max="12" controls-position="right" style="width:100%"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结束节次" prop="newEndPeriod">
              <el-input-number v-model="form.newEndPeriod" :min="1" :max="12" controls-position="right" style="width:100%"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="申请原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请输入申请原因" />
        </el-form-item>
        <el-form-item label="申请人" prop="applicant">
          <el-input v-model="form.applicant" placeholder="请输入申请人" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import { listAdjust, getAdjust, delAdjust, addAdjust, updateAdjust, approveAdjust, rejectAdjust } from "@/api/tpm/adjust"
import { listSchedule } from "@/api/tpm/schedule"
import { listClassroom } from "@/api/brm/classroom"

export default {
  name: "Adjust",
  dicts: ['tpm_adjust_type', 'tpm_approve_status', 'sys_normal_disable'],
  data() {
    return {
      loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0,
      adjustList: [], title: "", open: false,
      scheduleOptions: [], classroomOptions: [],
      weekDayOptions: [
        { value: 1, label: '周一' }, { value: 2, label: '周二' }, { value: 3, label: '周三' },
        { value: 4, label: '周四' }, { value: 5, label: '周五' }, { value: 6, label: '周六' }, { value: 7, label: '周日' }
      ],
      queryParams: { pageNum: 1, pageSize: 10, adjustType: null, approveStatus: null, courseName: null },
      form: {},
      rules: {
        scheduleId: [{ required: true, message: "排课不能为空", trigger: "change" }],
        adjustType: [{ required: true, message: "调课类型不能为空", trigger: "change" }],
        reason: [{ required: true, message: "申请原因不能为空", trigger: "blur" }]
      }
    }
  },
  created() { this.getList(); this.loadOptions() },
  methods: {
    getList() {
      this.loading = true
      listAdjust(this.queryParams).then(response => {
        this.adjustList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    loadOptions() {
      listSchedule({ pageNum: 1, pageSize: 200 }).then(res => { this.scheduleOptions = res.rows || [] })
      listClassroom({ pageNum: 1, pageSize: 1000 }).then(res => { this.classroomOptions = res.rows || [] })
    },
    scheduleLabel(item) {
      if (!item) return ''
      const day = this.weekDayLabel(item.weekDay)
      const period = (item.startPeriod && item.endPeriod) ? `第${item.startPeriod}-${item.endPeriod}节` : ''
      return `${item.courseName || ''} ${item.classroomName || ''} ${day}${period}`
    },
    weekDayLabel(day) {
      const labels = { 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日' }
      return labels[day] || ''
    },
    formatTime(weekDay, startPeriod, endPeriod) {
      if (!weekDay && !startPeriod && !endPeriod) return '-'
      const day = this.weekDayLabel(weekDay)
      const period = (startPeriod && endPeriod) ? `第${startPeriod}-${endPeriod}节` : ''
      return `${day}${period}` || '-'
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = {
        adjustId: null, scheduleId: null, adjustType: null,
        originalDate: null, newDate: null, newClassroomId: null,
        newWeekDay: null, newStartPeriod: null, newEndPeriod: null,
        reason: null, applicant: null, approveStatus: null, status: "0"
      }
      this.resetForm("form")
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.adjustId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() { this.reset(); this.open = true; this.title = "添加调停课申请" },
    handleUpdate(row) {
      this.reset()
      const adjustId = row.adjustId || this.ids
      getAdjust(adjustId).then(response => { this.form = response.data; this.open = true; this.title = "修改调停课申请" })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.adjustId != null) {
            updateAdjust(this.form).then(() => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() })
          } else {
            addAdjust(this.form).then(() => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() })
          }
        }
      })
    },
    handleDelete(row) {
      const adjustIds = row.adjustId || this.ids
      this.$modal.confirm('是否确认删除调停课申请编号为"' + adjustIds + '"的数据项？').then(function () { return delAdjust(adjustIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    },
    handleExport() { this.download('tpm/adjust/export', { ...this.queryParams }, `adjust_${new Date().getTime()}.xlsx`) },
    handleApprove(row) {
      this.$prompt('请输入审批意见', '审批通过', {
        confirmButtonText: '确定', cancelButtonText: '取消', inputType: 'textarea', inputValidator: () => true
      }).then(({ value }) => {
        approveAdjust(row.adjustId, { approveComment: value }).then(() => {
          this.$modal.msgSuccess("审批通过成功")
          this.getList()
        })
      }).catch(() => {})
    },
    handleReject(row) {
      this.$prompt('请输入驳回原因', '审批驳回', {
        confirmButtonText: '确定', cancelButtonText: '取消', inputType: 'textarea', inputValidator: () => true
      }).then(({ value }) => {
        rejectAdjust(row.adjustId, { approveComment: value }).then(() => {
          this.$modal.msgSuccess("已驳回")
          this.getList()
        })
      }).catch(() => {})
    }
  }
}
</script>
