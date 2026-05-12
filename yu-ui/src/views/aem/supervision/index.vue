<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="督导姓名" prop="supervisor"><el-input v-model="queryParams.supervisor" placeholder="请输入督导姓名" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="教师ID" prop="teacherId"><el-input v-model="queryParams.teacherId" placeholder="请输入教师ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="记录类型" prop="recordType"><el-select v-model="queryParams.recordType" placeholder="请选择记录类型" clearable><el-option v-for="dict in dict.type.aem_record_type" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aem:supervision:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['aem:supervision:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aem:supervision:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['aem:supervision:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="supervisionList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="课程ID" align="center" prop="courseId" />
      <el-table-column label="教师ID" align="center" prop="teacherId" />
      <el-table-column label="督导姓名" align="center" prop="supervisor" />
      <el-table-column label="听课日期" align="center" prop="visitDate" width="120"><template slot-scope="scope"><span>{{ parseTime(scope.row.visitDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="听课节数" align="center" prop="classHours" />
      <el-table-column label="评价评分" align="center" prop="evaluationScore" />
      <el-table-column label="评价等级" align="center" prop="evaluationLevel"><template slot-scope="scope"><dict-tag :options="dict.type.aem_evaluation_level" :value="scope.row.evaluationLevel"/></template></el-table-column>
      <el-table-column label="记录类型" align="center" prop="recordType"><template slot-scope="scope"><dict-tag :options="dict.type.aem_record_type" :value="scope.row.recordType"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aem:supervision:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aem:supervision:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="650px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="课程ID" prop="courseId"><el-input v-model="form.courseId" placeholder="请输入课程ID" /></el-form-item>
        <el-form-item label="教师ID" prop="teacherId"><el-input v-model="form.teacherId" placeholder="请输入教师ID" /></el-form-item>
        <el-form-item label="督导姓名" prop="supervisor"><el-input v-model="form.supervisor" placeholder="请输入督导姓名" /></el-form-item>
        <el-form-item label="听课日期" prop="visitDate"><el-date-picker clearable v-model="form.visitDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择听课日期" style="width:100%" /></el-form-item>
        <el-form-item label="听课节数" prop="classHours"><el-input-number v-model="form.classHours" placeholder="请输入听课节数" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="教学内容" prop="teachingContent"><el-input v-model="form.teachingContent" type="textarea" placeholder="请输入教学内容" /></el-form-item>
        <el-form-item label="评价评分" prop="evaluationScore"><el-input-number v-model="form.evaluationScore" placeholder="请输入评价评分" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item>
        <el-form-item label="评价等级" prop="evaluationLevel"><el-select v-model="form.evaluationLevel" placeholder="请选择评价等级" style="width:100%"><el-option v-for="dict in dict.type.aem_evaluation_level" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
        <el-form-item label="改进建议" prop="suggestion"><el-input v-model="form.suggestion" type="textarea" placeholder="请输入改进建议" /></el-form-item>
        <el-form-item label="记录类型" prop="recordType"><el-select v-model="form.recordType" placeholder="请选择记录类型" style="width:100%"><el-option v-for="dict in dict.type.aem_record_type" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listSupervision, getSupervision, delSupervision, addSupervision, updateSupervision } from "@/api/aem/supervision"
export default {
  name: "Supervision", dicts: ['aem_evaluation_level', 'aem_record_type'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, supervisionList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, supervisor: null, teacherId: null, recordType: null },
    form: {}, rules: { supervisor: [{ required: true, message: "督导姓名不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listSupervision(this.queryParams).then(response => { this.supervisionList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { recordId: null, courseId: null, teacherId: null, supervisor: null, visitDate: null, classHours: null, teachingContent: null, evaluationScore: null, evaluationLevel: "0", suggestion: null, recordType: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.recordId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加督导听课记录" },
    handleUpdate(row) { this.reset(); const recordId = row.recordId || this.ids; getSupervision(recordId).then(response => { this.form = response.data; this.open = true; this.title = "修改督导听课记录" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.recordId != null) { updateSupervision(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addSupervision(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const recordIds = row.recordId || this.ids; this.$modal.confirm('是否确认删除督导听课记录编号为"' + recordIds + '"的数据项？').then(function() { return delSupervision(recordIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('aem/supervision/export', { ...this.queryParams }, `supervision_${new Date().getTime()}.xlsx`) }
  }
}
</script>
