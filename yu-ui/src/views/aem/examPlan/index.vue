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
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="examPlanList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="考试名称" align="center" prop="examName" :show-overflow-tooltip="true" />
      <el-table-column label="学期ID" align="center" prop="semesterId" />
      <el-table-column label="考试类型" align="center" prop="examType"><template slot-scope="scope"><dict-tag :options="dict.type.aem_exam_type" :value="scope.row.examType"/></template></el-table-column>
      <el-table-column label="课程ID" align="center" prop="courseId" />
      <el-table-column label="考试日期" align="center" prop="examDate" width="120"><template slot-scope="scope"><span>{{ parseTime(scope.row.examDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="开始时间" align="center" prop="startTime" />
      <el-table-column label="结束时间" align="center" prop="endTime" />
      <el-table-column label="考试时长(分)" align="center" prop="duration" />
      <el-table-column label="考生人数" align="center" prop="totalStudents" />
      <el-table-column label="安排状态" align="center" prop="planStatus"><template slot-scope="scope"><dict-tag :options="dict.type.aem_plan_status" :value="scope.row.planStatus"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
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
        <el-form-item label="开始时间" prop="startTime"><el-input v-model="form.startTime" placeholder="请输入开始时间(如14:30)" /></el-form-item>
        <el-form-item label="结束时间" prop="endTime"><el-input v-model="form.endTime" placeholder="请输入结束时间(如16:30)" /></el-form-item>
        <el-form-item label="考试时长(分)" prop="duration"><el-input-number v-model="form.duration" placeholder="请输入考试时长" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="考生人数" prop="totalStudents"><el-input-number v-model="form.totalStudents" placeholder="请输入考生人数" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="安排状态" prop="planStatus"><el-select v-model="form.planStatus" placeholder="请选择安排状态" style="width:100%"><el-option v-for="dict in dict.type.aem_plan_status" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listExamPlan, getExamPlan, delExamPlan, addExamPlan, updateExamPlan } from "@/api/aem/examPlan"
export default {
  name: "ExamPlan", dicts: ['aem_exam_type', 'aem_plan_status'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, examPlanList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, examName: null, examType: null, planStatus: null },
    form: {}, rules: { examName: [{ required: true, message: "考试名称不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listExamPlan(this.queryParams).then(response => { this.examPlanList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { examId: null, examName: null, semesterId: null, courseId: null, examType: "0", examDate: null, startTime: null, endTime: null, duration: null, totalStudents: null, planStatus: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.examId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加考试安排" },
    handleUpdate(row) { this.reset(); const examId = row.examId || this.ids; getExamPlan(examId).then(response => { this.form = response.data; this.open = true; this.title = "修改考试安排" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.examId != null) { updateExamPlan(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addExamPlan(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const examIds = row.examId || this.ids; this.$modal.confirm('是否确认删除考试安排编号为"' + examIds + '"的数据项？').then(function() { return delExamPlan(examIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('aem/examPlan/export', { ...this.queryParams }, `examPlan_${new Date().getTime()}.xlsx`) }
  }
}
</script>
