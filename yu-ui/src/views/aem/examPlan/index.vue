<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="考试名称" prop="examName"><el-input v-model="queryParams.examName" placeholder="请输入考试名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="考试类型" prop="examType"><el-select v-model="queryParams.examType" placeholder="请选择考试类型" clearable><el-option v-for="dict in dict.type.aem_exam_type" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item label="安排状态" prop="planStatus"><el-select v-model="queryParams.planStatus" placeholder="请选择安排状态" clearable><el-option v-for="dict in dict.type.aem_plan_status" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aem:examPlan:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['aem:examPlan:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aem:examPlan:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['aem:examPlan:export']">导出</el-button></el-col>
      <el-col :span="1.5"><el-tooltip content="点击左侧箭头展开可维护座位编排与监考安排" placement="top"><span class="tips-text">点击行首箭头维护明细</span></el-tooltip></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table ref="examPlanTable" v-loading="loading" :data="examPlanList" @selection-change="handleSelectionChange" :row-key="getRowKey" @expand-change="handleExpandChange">
      <el-table-column type="expand">
        <template slot-scope="props">
          <el-tabs v-model="activeTab[props.row.examId]" type="card" class="detail-tabs">
            <el-tab-pane label="座位编排" name="seats">
              <master-detail-panel
                :master-id="props.row.examId"
                foreign-key="examId"
                title="座位编排"
                row-key="seatId"
                :loader="loadSeats"
                :add-api="addExamSeat"
                :update-api="updateExamSeat"
                :delete-api="delExamSeat"
                :perms="{ add: ['aem:examSeat:add'], edit: ['aem:examSeat:edit'], remove: ['aem:examSeat:remove'] }"
                :columns="seatColumns"
              />
            </el-tab-pane>
            <el-tab-pane label="监考安排" name="invigilations">
              <master-detail-panel
                :master-id="props.row.examId"
                foreign-key="examId"
                title="监考安排"
                row-key="invigilationId"
                :loader="loadInvigilations"
                :add-api="addInvigilation"
                :update-api="updateInvigilation"
                :delete-api="delInvigilation"
                :perms="{ add: ['aem:invigilation:add'], edit: ['aem:invigilation:edit'], remove: ['aem:invigilation:remove'] }"
                :columns="invigilationColumns"
              />
            </el-tab-pane>
          </el-tabs>
        </template>
      </el-table-column>
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="考试名称" align="center" prop="examName" :show-overflow-tooltip="true" />
      <el-table-column label="学期ID" align="center" prop="semesterId" width="80" />
      <el-table-column label="考试类型" align="center" prop="examType" width="100"><template slot-scope="scope"><dict-tag :options="dict.type.aem_exam_type" :value="scope.row.examType"/></template></el-table-column>
      <el-table-column label="课程ID" align="center" prop="courseId" width="80" />
      <el-table-column label="考试日期" align="center" prop="examDate" width="120"><template slot-scope="scope"><span>{{ parseTime(scope.row.examDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="开始时间" align="center" prop="startTime" width="90" />
      <el-table-column label="结束时间" align="center" prop="endTime" width="90" />
      <el-table-column label="考试时长(分)" align="center" prop="duration" width="100" />
      <el-table-column label="考生人数" align="center" prop="totalStudents" width="80" />
      <el-table-column label="安排状态" align="center" prop="planStatus" width="90"><template slot-scope="scope"><dict-tag :options="dict.type.aem_plan_status" :value="scope.row.planStatus"/></template></el-table-column>
      <el-table-column label="操作" align="center" width="280" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="toggleExpand(scope.row)">明细</el-button>
          <el-button size="mini" type="text" icon="el-icon-s-grid" @click="handleAutoSeat(scope.row)" v-hasPermi="['aem:examSeat:add']">排座</el-button>
          <el-button size="mini" type="text" icon="el-icon-user" @click="handleAutoDispatch(scope.row)" v-hasPermi="['aem:invigilation:add']">派监考</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aem:examPlan:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aem:examPlan:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="700px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="考试名称" prop="examName"><el-input v-model="form.examName" placeholder="请输入考试名称" /></el-form-item>
        <el-form-item label="学期ID" prop="semesterId"><el-input v-model="form.semesterId" placeholder="请输入学期ID" /></el-form-item>
        <el-form-item label="课程ID" prop="courseId"><el-input v-model="form.courseId" placeholder="请输入课程ID" /></el-form-item>
        <el-form-item label="考试类型" prop="examType"><el-select v-model="form.examType" placeholder="请选择考试类型" style="width:100%"><el-option v-for="dict in dict.type.aem_exam_type" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
        <el-form-item label="考试日期" prop="examDate"><el-date-picker clearable v-model="form.examDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择考试日期" style="width:100%" /></el-form-item>
        <el-form-item label="开始时间" prop="startTime"><el-time-picker v-model="form.startTime" value-format="HH:mm" format="HH:mm" placeholder="如14:30" style="width:100%" /></el-form-item>
        <el-form-item label="结束时间" prop="endTime"><el-time-picker v-model="form.endTime" value-format="HH:mm" format="HH:mm" placeholder="如16:30" style="width:100%" /></el-form-item>
        <el-form-item label="考试时长(分)" prop="duration"><el-input-number v-model="form.duration" placeholder="请输入考试时长" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="考生人数" prop="totalStudents"><el-input-number v-model="form.totalStudents" placeholder="请输入考生人数" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="安排状态" prop="planStatus"><el-select v-model="form.planStatus" placeholder="请选择安排状态" style="width:100%"><el-option v-for="dict in dict.type.aem_plan_status" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listExamPlan, getExamPlan, delExamPlan, addExamPlan, updateExamPlan, getExamPlanDetail, autoArrangeSeat, autoDispatch } from "@/api/aem/examPlan"
import { listExamSeat, addExamSeat, updateExamSeat, delExamSeat } from "@/api/aem/examSeat"
import { listInvigilation, addInvigilation, updateInvigilation, delInvigilation } from "@/api/aem/invigilation"
import MasterDetailPanel from "../components/MasterDetailPanel"
export default {
  name: "ExamPlan",
  components: { MasterDetailPanel },
  dicts: ['aem_exam_type', 'aem_plan_status', 'aem_duty_type'],
  data() {
    return {
      loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, examPlanList: [], title: "", open: false,
      activeTab: {},
      queryParams: { pageNum: 1, pageSize: 10, examName: null, examType: null, planStatus: null },
      form: {},
      rules: { examName: [{ required: true, message: "考试名称不能为空", trigger: "blur" }] },
      seatColumns: [
        { prop: 'classroomId', label: '教室ID', width: 90, type: 'number', required: true },
        { prop: 'studentId', label: '学生ID', width: 110, type: 'number', required: true },
        { prop: 'seatNumber', label: '座位号', width: 90, type: 'number', min: 0 },
        { prop: 'rowNumber', label: '行号', width: 80, type: 'number', min: 0 },
        { prop: 'colNumber', label: '列号', width: 80, type: 'number', min: 0 },
        { prop: 'status', label: '状态', width: 90, type: 'select', options: [{ label: '正常', value: '0' }, { label: '缺考', value: '1' }], defaultValue: '0' }
      ],
      invigilationColumns: [
        { prop: 'classroomId', label: '教室ID', width: 90, type: 'number', required: true },
        { prop: 'teacherId', label: '教师ID', width: 110, type: 'number', required: true },
        { prop: 'examDate', label: '考试日期', type: 'date' },
        { prop: 'startTime', label: '开始时间', width: 100, type: 'time' },
        { prop: 'endTime', label: '结束时间', width: 100, type: 'time' },
        { prop: 'dutyType', label: '职责', width: 110, type: 'select', options: [{ label: '主监考', value: '0' }, { label: '副监考', value: '1' }, { label: '巡考', value: '2' }], defaultValue: '0' }
      ]
    }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listExamPlan(this.queryParams).then(response => { this.examPlanList = response.rows; this.total = response.total; this.loading = false }) },
    getRowKey(row) { return row.examId },
    // 展开行时懒加载明细并默认显示第一个标签
    handleExpandChange(row, expandedRows) {
      const expanded = expandedRows.some(r => r.examId === row.examId)
      if (expanded && !this.activeTab[row.examId]) {
        this.$set(this.activeTab, row.examId, 'seats')
      }
    },
    toggleExpand(row) {
      this.$refs.examPlanTable && this.$refs.examPlanTable.toggleRowExpansion(row)
    },
    loadSeats(examId) {
      return listExamSeat({ examId, pageNum: 1, pageSize: 1000 }).then(res => res.rows)
    },
    loadInvigilations(examId) {
      return listInvigilation({ examId, pageNum: 1, pageSize: 1000 }).then(res => res.rows)
    },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { examId: null, examName: null, semesterId: null, courseId: null, examType: "0", examDate: null, startTime: null, endTime: null, duration: null, totalStudents: null, planStatus: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.examId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加考试安排" },
    handleUpdate(row) { this.reset(); const examId = row.examId || this.ids; getExamPlan(examId).then(response => { this.form = response.data; this.open = true; this.title = "修改考试安排" }) },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) return
        // 级联校验：发布状态时检测是否已编排座位或监考
        if (this.form.planStatus === '2') {
          getExamPlanDetail(this.form.examId).then(res => {
            const d = res.data || {}
            if ((!d.seats || d.seats.length === 0) && (!d.invigilations || d.invigilations.length === 0)) {
              this.$modal.msgWarning("发布前请先展开该考试并完成座位编排或监考安排")
              return
            }
            this.doSubmit()
          })
        } else {
          this.doSubmit()
        }
      })
    },
    doSubmit() {
      if (this.form.examId != null) {
        updateExamPlan(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() })
      } else {
        addExamPlan(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() })
      }
    },
    handleDelete(row) { const examIds = row.examId || this.ids; this.$modal.confirm('是否确认删除考试安排编号为"' + examIds + '"的数据项？其下座位与监考安排将一并删除。').then(function() { return delExamPlan(examIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleAutoSeat(row) {
      this.$prompt('请输入教室ID以自动编排座位', '自动排座', { confirmButtonText: '确定', cancelButtonText: '取消', inputPattern: /^\d+$/, inputErrorMessage: '教室ID必须为数字' })
        .then(({ value }) => {
          return autoArrangeSeat(row.examId, value)
        }).then(res => {
          this.$modal.msgSuccess((res.data && res.data.message) || res.msg || '排座完成')
          this.getList()
        }).catch(() => {})
    },
    handleAutoDispatch(row) {
      this.$modal.confirm('确认为考试「' + row.examName + '」自动派发监考教师？将清除原监考记录。').then(() => {
        return autoDispatch(row.examId)
      }).then(res => {
        this.$modal.msgSuccess((res.data && res.data.message) || res.msg || '派监考完成')
        this.getList()
      }).catch(() => {})
    },
    handleExport() { this.download('aem/examPlan/export', { ...this.queryParams }, `examPlan_${new Date().getTime()}.xlsx`) }
  }
}
</script>
<style scoped>
.tips-text { color: #909399; font-size: 12px; line-height: 28px; }
.detail-tabs { margin: 0; }
</style>
