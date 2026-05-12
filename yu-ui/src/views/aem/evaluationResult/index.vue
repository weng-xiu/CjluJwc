<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="问卷ID" prop="questionnaireId"><el-input v-model="queryParams.questionnaireId" placeholder="请输入问卷ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="课程ID" prop="courseId"><el-input v-model="queryParams.courseId" placeholder="请输入课程ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="教师ID" prop="teacherId"><el-input v-model="queryParams.teacherId" placeholder="请输入教师ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aem:evaluationResult:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['aem:evaluationResult:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aem:evaluationResult:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['aem:evaluationResult:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="evaluationResultList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="问卷ID" align="center" prop="questionnaireId" />
      <el-table-column label="课程ID" align="center" prop="courseId" />
      <el-table-column label="教师ID" align="center" prop="teacherId" />
      <el-table-column label="学生ID" align="center" prop="studentId" />
      <el-table-column label="总评分" align="center" prop="totalScore" />
      <el-table-column label="评教时间" align="center" prop="evalDate" width="160"><template slot-scope="scope"><span>{{ parseTime(scope.row.evalDate, '{y}-{m}-{d} {h}:{i}:{s}') }}</span></template></el-table-column>
      <el-table-column label="评语建议" align="center" prop="comment" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aem:evaluationResult:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aem:evaluationResult:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="650px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="问卷ID" prop="questionnaireId"><el-input v-model="form.questionnaireId" placeholder="请输入问卷ID" /></el-form-item>
        <el-form-item label="课程ID" prop="courseId"><el-input v-model="form.courseId" placeholder="请输入课程ID" /></el-form-item>
        <el-form-item label="教师ID" prop="teacherId"><el-input v-model="form.teacherId" placeholder="请输入教师ID" /></el-form-item>
        <el-form-item label="学生ID" prop="studentId"><el-input v-model="form.studentId" placeholder="请输入学生ID" /></el-form-item>
        <el-form-item label="总评分" prop="totalScore"><el-input-number v-model="form.totalScore" placeholder="请输入总评分" :min="0" :precision="1" style="width:100%" /></el-form-item>
        <el-form-item label="评教时间" prop="evalDate"><el-date-picker clearable v-model="form.evalDate" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择评教时间" style="width:100%" /></el-form-item>
        <el-form-item label="评语建议" prop="comment"><el-input v-model="form.comment" type="textarea" placeholder="请输入评语建议" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listEvaluationResult, getEvaluationResult, delEvaluationResult, addEvaluationResult, updateEvaluationResult } from "@/api/aem/evaluationResult"
export default {
  name: "EvaluationResult", dicts: [],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, evaluationResultList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, questionnaireId: null, courseId: null, teacherId: null },
    form: {}, rules: {} }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listEvaluationResult(this.queryParams).then(response => { this.evaluationResultList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { resultId: null, questionnaireId: null, courseId: null, teacherId: null, studentId: null, totalScore: null, evalDate: null, comment: null }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.resultId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加评教结果" },
    handleUpdate(row) { this.reset(); const resultId = row.resultId || this.ids; getEvaluationResult(resultId).then(response => { this.form = response.data; this.open = true; this.title = "修改评教结果" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.resultId != null) { updateEvaluationResult(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addEvaluationResult(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const resultIds = row.resultId || this.ids; this.$modal.confirm('是否确认删除评教结果编号为"' + resultIds + '"的数据项？').then(function() { return delEvaluationResult(resultIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('aem/evaluationResult/export', { ...this.queryParams }, `evaluationResult_${new Date().getTime()}.xlsx`) }
  }
}
</script>
