<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="问卷ID" prop="questionnaireId"><el-input v-model="queryParams.questionnaireId" placeholder="请输入问卷ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="问题类型" prop="questionType"><el-select v-model="queryParams.questionType" placeholder="请选择问题类型" clearable><el-option v-for="dict in dict.type.aem_question_type" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aem:question:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['aem:question:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aem:question:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['aem:question:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="questionList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="问卷ID" align="center" prop="questionnaireId" />
      <el-table-column label="问题类型" align="center" prop="questionType"><template slot-scope="scope"><dict-tag :options="dict.type.aem_question_type" :value="scope.row.questionType"/></template></el-table-column>
      <el-table-column label="问题内容" align="center" prop="questionContent" :show-overflow-tooltip="true" />
      <el-table-column label="排序号" align="center" prop="sortOrder" />
      <el-table-column label="最高评分" align="center" prop="maxScore" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aem:question:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aem:question:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="问卷ID" prop="questionnaireId"><el-input v-model="form.questionnaireId" placeholder="请输入问卷ID" /></el-form-item>
        <el-form-item label="问题类型" prop="questionType"><el-select v-model="form.questionType" placeholder="请选择问题类型" style="width:100%"><el-option v-for="dict in dict.type.aem_question_type" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
        <el-form-item label="问题内容" prop="questionContent"><el-input v-model="form.questionContent" type="textarea" placeholder="请输入问题内容" /></el-form-item>
        <el-form-item label="排序号" prop="sortOrder"><el-input-number v-model="form.sortOrder" placeholder="请输入排序号" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="最高评分" prop="maxScore"><el-input-number v-model="form.maxScore" placeholder="请输入最高评分" :min="0" :precision="1" style="width:100%" /></el-form-item>
        <el-form-item label="选项JSON" prop="optionsJson"><el-input v-model="form.optionsJson" type="textarea" placeholder='请输入选项JSON,如[{"label":"A","value":"A"}]' /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listQuestion, getQuestion, delQuestion, addQuestion, updateQuestion } from "@/api/aem/evaluationQuestion"
export default {
  name: "Question", dicts: ['aem_question_type'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, questionList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, questionnaireId: null, questionType: null },
    form: {}, rules: { questionContent: [{ required: true, message: "问题内容不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listQuestion(this.queryParams).then(response => { this.questionList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { questionId: null, questionnaireId: null, questionType: "0", questionContent: null, sortOrder: null, maxScore: null, optionsJson: null }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.questionId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加评教问题" },
    handleUpdate(row) { this.reset(); const questionId = row.questionId || this.ids; getQuestion(questionId).then(response => { this.form = response.data; this.open = true; this.title = "修改评教问题" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.questionId != null) { updateQuestion(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addQuestion(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const questionIds = row.questionId || this.ids; this.$modal.confirm('是否确认删除评教问题编号为"' + questionIds + '"的数据项？').then(function() { return delQuestion(questionIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('aem/question/export', { ...this.queryParams }, `question_${new Date().getTime()}.xlsx`) }
  }
}
</script>
