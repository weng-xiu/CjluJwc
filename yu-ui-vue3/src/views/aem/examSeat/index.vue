<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="考试ID" prop="examId"><el-input v-model="queryParams.examId" placeholder="请输入考试ID" clearable @keyup.enter="handleQuery"/></el-form-item>
      <el-form-item label="教室ID" prop="classroomId"><el-input v-model="queryParams.classroomId" placeholder="请输入教室ID" clearable @keyup.enter="handleQuery"/></el-form-item>
      <el-form-item label="学生ID" prop="studentId"><el-input v-model="queryParams.studentId" placeholder="请输入学生ID" clearable @keyup.enter="handleQuery"/></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button><el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['aem:examSeat:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['aem:examSeat:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['aem:examSeat:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['aem:examSeat:export']">导出</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="Upload" size="small" @click="handleImport" v-hasPermi="['aem:examSeat:import']">导入</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="examSeatList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="考试ID" align="center" prop="examId" />
      <el-table-column label="教室ID" align="center" prop="classroomId" />
      <el-table-column label="学生ID" align="center" prop="studentId" />
      <el-table-column label="座位号" align="center" prop="seatNumber" />
      <el-table-column label="行号" align="center" prop="rowNumber" />
      <el-table-column label="列号" align="center" prop="colNumber" />
      <el-table-column label="状态" align="center" prop="status"><template #default="scope"><dict-tag :options="dict.type.aem_seat_status" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button size="small" link icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['aem:examSeat:edit']">修改</el-button>
          <el-button size="small" link icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['aem:examSeat:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="考试ID" prop="examId"><el-input v-model="form.examId" placeholder="请输入考试ID" /></el-form-item>
        <el-form-item label="教室ID" prop="classroomId"><el-input v-model="form.classroomId" placeholder="请输入教室ID" /></el-form-item>
        <el-form-item label="学生ID" prop="studentId"><el-input v-model="form.studentId" placeholder="请输入学生ID" /></el-form-item>
        <el-form-item label="座位号" prop="seatNumber"><el-input-number v-model="form.seatNumber" placeholder="请输入座位号" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="行号" prop="rowNumber"><el-input-number v-model="form.rowNumber" placeholder="请输入行号" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="列号" prop="colNumber"><el-input-number v-model="form.colNumber" placeholder="请输入列号" :min="1" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
      </template>
    </el-dialog>

    <import-excel-dialog
      ref="importDialog"
      title="考场座位导入"
      tip="请按模板填写考试ID、教室ID、学生ID、座位号等信息。"
      :import-api="importExamSeat"
      template-url="aem/examSeat/importTemplate"
      template-name="examSeat_template"
      @success="getList"/>
  </div>
</template>
<script>
import { listExamSeat, getExamSeat, delExamSeat, addExamSeat, updateExamSeat, importExamSeat } from "@/api/aem/examSeat"
import ImportExcelDialog from "../components/ImportExcelDialog"
export default {
  name: "ExamSeat",
  components: { ImportExcelDialog },
  dicts: ['aem_seat_status'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, examSeatList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, examId: null, classroomId: null, studentId: null },
    form: {}, rules: {} }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listExamSeat(this.queryParams).then(response => { this.examSeatList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { seatId: null, examId: null, classroomId: null, studentId: null, seatNumber: null, rowNumber: null, colNumber: null }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.seatId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加座位编排" },
    handleUpdate(row) { this.reset(); const seatId = row.seatId || this.ids; getExamSeat(seatId).then(response => { this.form = response.data; this.open = true; this.title = "修改座位编排" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.seatId != null) { updateExamSeat(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addExamSeat(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const seatIds = row.seatId || this.ids; this.$modal.confirm('是否确认删除座位编排编号为"' + seatIds + '"的数据项？').then(function() { return delExamSeat(seatIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('aem/examSeat/export', { ...this.queryParams }, `examSeat_${new Date().getTime()}.xlsx`) },
    handleImport() { this.$refs.importDialog.open() }
  }
}
</script>
