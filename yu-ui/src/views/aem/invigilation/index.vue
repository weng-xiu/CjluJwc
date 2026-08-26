<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="考试ID" prop="examId"><el-input v-model="queryParams.examId" placeholder="请输入考试ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="教师ID" prop="teacherId"><el-input v-model="queryParams.teacherId" placeholder="请输入教师ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="职责" prop="dutyType"><el-select v-model="queryParams.dutyType" placeholder="请选择职责" clearable><el-option v-for="dict in dict.type.aem_duty_type" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aem:invigilation:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['aem:invigilation:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aem:invigilation:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['aem:invigilation:export']">导出</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="el-icon-upload2" size="mini" @click="handleImport" v-hasPermi="['aem:invigilation:import']">导入</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="invigilationList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="考试ID" align="center" prop="examId" />
      <el-table-column label="教室ID" align="center" prop="classroomId" />
      <el-table-column label="教师ID" align="center" prop="teacherId" />
      <el-table-column label="考试日期" align="center" prop="examDate" width="180"><template slot-scope="scope"><span>{{ parseTime(scope.row.examDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="开始时间" align="center" prop="startTime" />
      <el-table-column label="结束时间" align="center" prop="endTime" />
      <el-table-column label="职责" align="center" prop="dutyType"><template slot-scope="scope"><dict-tag :options="dict.type.aem_duty_type" :value="scope.row.dutyType"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aem:invigilation:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aem:invigilation:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="考试ID" prop="examId"><el-input v-model="form.examId" placeholder="请输入考试ID" /></el-form-item>
        <el-form-item label="教室ID" prop="classroomId"><el-input v-model="form.classroomId" placeholder="请输入教室ID" /></el-form-item>
        <el-form-item label="教师ID" prop="teacherId"><el-input v-model="form.teacherId" placeholder="请输入教师ID" /></el-form-item>
        <el-form-item label="考试日期" prop="examDate"><el-date-picker clearable v-model="form.examDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择考试日期" /></el-form-item>
        <el-form-item label="开始时间" prop="startTime"><el-input v-model="form.startTime" placeholder="请输入开始时间(如14:30)" /></el-form-item>
        <el-form-item label="结束时间" prop="endTime"><el-input v-model="form.endTime" placeholder="请输入结束时间(如16:30)" /></el-form-item>
        <el-form-item label="职责" prop="dutyType"><el-select v-model="form.dutyType" placeholder="请选择职责"><el-option v-for="dict in dict.type.aem_duty_type" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>

    <import-excel-dialog
      ref="importDialog"
      title="监考安排导入"
      tip="请按模板填写考试ID、教室ID、教师ID、考试日期、开始/结束时间。"
      :import-api="importInvigilation"
      template-url="aem/invigilation/importTemplate"
      template-name="invigilation_template"
      @success="getList"/>
  </div>
</template>
<script>
import { listInvigilation, getInvigilation, delInvigilation, addInvigilation, updateInvigilation, importInvigilation } from "@/api/aem/invigilation"
import ImportExcelDialog from "../components/ImportExcelDialog"
export default {
  name: "Invigilation",
  components: { ImportExcelDialog },
  dicts: ['aem_duty_type'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, invigilationList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, examId: null, teacherId: null, dutyType: null },
    form: {}, rules: {} }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listInvigilation(this.queryParams).then(response => { this.invigilationList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { invigilationId: null, examId: null, classroomId: null, teacherId: null, examDate: null, startTime: null, endTime: null, dutyType: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.invigilationId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加监考分配" },
    handleUpdate(row) { this.reset(); const invigilationId = row.invigilationId || this.ids; getInvigilation(invigilationId).then(response => { this.form = response.data; this.open = true; this.title = "修改监考分配" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.invigilationId != null) { updateInvigilation(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addInvigilation(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const invigilationIds = row.invigilationId || this.ids; this.$modal.confirm('是否确认删除监考分配编号为"' + invigilationIds + '"的数据项？').then(function() { return delInvigilation(invigilationIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('aem/invigilation/export', { ...this.queryParams }, `invigilation_${new Date().getTime()}.xlsx`) },
    handleImport() { this.$refs.importDialog.open() }
  }
}
</script>
