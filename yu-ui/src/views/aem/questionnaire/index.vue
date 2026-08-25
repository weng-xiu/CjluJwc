<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="问卷标题" prop="title"><el-input v-model="queryParams.title" placeholder="请输入问卷标题" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="评教状态" prop="evalStatus"><el-select v-model="queryParams.evalStatus" placeholder="请选择评教状态" clearable><el-option v-for="dict in dict.type.aem_eval_status" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aem:questionnaire:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['aem:questionnaire:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aem:questionnaire:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['aem:questionnaire:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table ref="questionnaireTable" v-loading="loading" :data="questionnaireList" @selection-change="handleSelectionChange" :row-key="getRowKey" @expand-change="handleExpandChange">
      <el-table-column type="expand">
        <template slot-scope="props">
          <master-detail-panel
            :master-id="props.row.questionnaireId"
            foreign-key="questionnaireId"
            title="评教题目"
            row-key="questionId"
            :loader="loadQuestions"
            :add-api="addQuestion"
            :update-api="updateQuestion"
            :delete-api="delQuestion"
            :perms="{ add: ['aem:question:add'], edit: ['aem:question:edit'], remove: ['aem:question:remove'] }"
            :columns="questionColumns"
            @change="handleDetailChange"
          />
        </template>
      </el-table-column>
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="问卷标题" align="center" prop="title" :show-overflow-tooltip="true" />
      <el-table-column label="学期" align="center" prop="semesterId">
        <template slot-scope="scope">
          <span>{{ getSemesterName(scope.row.semesterId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="题目数量" align="center" prop="questionCount" />
      <el-table-column label="满分" align="center" prop="fullScore" />
      <el-table-column label="开始时间" align="center" prop="startTime" width="160"><template slot-scope="scope"><span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span></template></el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="160"><template slot-scope="scope"><span>{{ parseTime(scope.row.endTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span></template></el-table-column>
      <el-table-column label="评教状态" align="center" prop="evalStatus"><template slot-scope="scope"><dict-tag :options="dict.type.aem_eval_status" :value="scope.row.evalStatus"/></template></el-table-column>
      <el-table-column label="是否匿名" align="center" prop="isAnonymous"><template slot-scope="scope"><dict-tag :options="dict.type.aem_is_anonymous" :value="scope.row.isAnonymous"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="toggleExpand(scope.row)">明细</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aem:questionnaire:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aem:questionnaire:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="650px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="学年">
          <el-select v-model="formYearId" placeholder="请选择学年" clearable @change="handleFormYearChange" style="width:100%">
            <el-option v-for="y in yearList" :key="y.yearId" :label="y.yearName" :value="y.yearId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="学期" prop="semesterId">
          <el-select v-model="form.semesterId" placeholder="请先选择学年" clearable :disabled="!formYearId" style="width:100%">
            <el-option v-for="s in formSemesterList" :key="s.semesterId" :label="s.semesterName" :value="s.semesterId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="问卷标题" prop="title"><el-input v-model="form.title" placeholder="请输入问卷标题" /></el-form-item>
        <el-form-item label="问卷说明" prop="description"><el-input v-model="form.description" type="textarea" placeholder="请输入问卷说明" /></el-form-item>
        <el-form-item label="题目数量" prop="questionCount"><el-input-number v-model="form.questionCount" placeholder="请输入题目数量" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="满分" prop="fullScore"><el-input-number v-model="form.fullScore" placeholder="请输入满分" :min="0" :precision="1" style="width:100%" /></el-form-item>
        <el-form-item label="开始时间" prop="startTime"><el-date-picker clearable v-model="form.startTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择开始时间" style="width:100%" /></el-form-item>
        <el-form-item label="结束时间" prop="endTime"><el-date-picker clearable v-model="form.endTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择结束时间" style="width:100%" /></el-form-item>
        <el-form-item label="评教状态" prop="evalStatus"><el-select v-model="form.evalStatus" placeholder="请选择评教状态" style="width:100%"><el-option v-for="dict in dict.type.aem_eval_status" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
        <el-form-item label="是否匿名" prop="isAnonymous"><el-radio-group v-model="form.isAnonymous"><el-radio v-for="dict in dict.type.aem_is_anonymous" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listQuestionnaire, getQuestionnaire, delQuestionnaire, addQuestionnaire, updateQuestionnaire } from "@/api/aem/questionnaire"
import { listQuestion, addQuestion, updateQuestion, delQuestion } from "@/api/aem/evaluationQuestion"
import { listYear } from "@/api/brm/year"
import { listSemester, getSemester } from "@/api/brm/semester"
import MasterDetailPanel from "../components/MasterDetailPanel"
export default {
  name: "Questionnaire",
  components: { MasterDetailPanel },
  dicts: ['aem_eval_status', 'aem_is_anonymous', 'aem_question_type'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, questionnaireList: [], title: "", open: false, semesterNameMap: {},
    queryParams: { pageNum: 1, pageSize: 10, title: null, evalStatus: null },
    form: {}, rules: { title: [{ required: true, message: "问卷标题不能为空", trigger: "blur" }] },
    yearList: [], formYearId: null, formSemesterList: [],
    questionColumns: [
      { prop: 'questionType', label: '题目类型', width: 110, type: 'dict', dict: 'aem_question_type', required: true },
      { prop: 'questionContent', label: '题目内容', required: true },
      { prop: 'sortOrder', label: '排序', width: 80, type: 'number', min: 0 },
      { prop: 'maxScore', label: '最高分', width: 100, type: 'number', min: 0, precision: 1 },
      { prop: 'optionsJson', label: '选项JSON', type: 'textarea', editable: false }
    ]
  }},
  created() { this.loadSemesterNameMap(); this.loadYears(); this.getList() },
  methods: {
    loadYears() {
      listYear({ pageNum: 1, pageSize: 100 }).then(r => { this.yearList = r.rows })
    },
    handleFormYearChange(yearId) {
      this.formSemesterList = []
      this.form.semesterId = null
      if (yearId) {
        listSemester({ academicYearId: yearId, pageNum: 1, pageSize: 50 }).then(r => { this.formSemesterList = r.rows })
      }
    },
    loadSemesterNameMap() {
      listSemester({ pageNum: 1, pageSize: 200 }).then(response => {
        const map = {}
        response.rows.forEach(s => { map[s.semesterId] = s.semesterName })
        this.semesterNameMap = map
      })
    },
    getSemesterName(semesterId) {
      return this.semesterNameMap[semesterId] || semesterId
    },
    getList() { this.loading = true; listQuestionnaire(this.queryParams).then(response => { this.questionnaireList = response.rows; this.total = response.total; this.loading = false }) },
    getRowKey(row) { return row.questionnaireId },
    handleExpandChange() { /* 由 MasterDetailPanel 内部 watch masterId 自动加载 */ },
    toggleExpand(row) { this.$refs.questionnaireTable && this.$refs.questionnaireTable.toggleRowExpansion(row) },
    loadQuestions(questionnaireId) {
      return listQuestion({ questionnaireId, pageNum: 1, pageSize: 1000 }).then(res => res.rows)
    },
    // 子表变更后刷新主表列表（题目数量回写）
    handleDetailChange() { this.getList() },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { questionnaireId: null, semesterId: null, title: null, description: null, questionCount: null, fullScore: null, startTime: null, endTime: null, evalStatus: "0", isAnonymous: "0" }; this.formYearId = null; this.formSemesterList = []; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.questionnaireId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加评教问卷" },
    handleUpdate(row) {
      this.reset()
      const questionnaireId = row.questionnaireId || this.ids
      getQuestionnaire(questionnaireId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改评教问卷"
        if (this.form.semesterId) {
          getSemester(this.form.semesterId).then(res => {
            if (res.data && res.data.academicYearId) {
              this.formYearId = res.data.academicYearId
              listSemester({ academicYearId: this.formYearId, pageNum: 1, pageSize: 50 }).then(r => { this.formSemesterList = r.rows })
            }
          })
        }
      })
    },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.questionnaireId != null) { updateQuestionnaire(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addQuestionnaire(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const questionnaireIds = row.questionnaireId || this.ids; this.$modal.confirm('是否确认删除评教问卷编号为"' + questionnaireIds + '"的数据项？').then(function() { return delQuestionnaire(questionnaireIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('aem/questionnaire/export', { ...this.queryParams }, `questionnaire_${new Date().getTime()}.xlsx`) }
  }
}
</script>
