<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学生ID" prop="studentId"><el-input v-model="queryParams.studentId" placeholder="请输入学生ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="课程ID" prop="courseId"><el-input v-model="queryParams.courseId" placeholder="请输入课程ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="考试类型" prop="examType"><el-select v-model="queryParams.examType" placeholder="请选择考试类型" clearable><el-option v-for="dict in dict.type.aem_grade_exam_type" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item label="是否通过" prop="isPass"><el-select v-model="queryParams.isPass" placeholder="请选择是否通过" clearable><el-option v-for="dict in dict.type.aem_is_pass" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aem:gradeRecord:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['aem:gradeRecord:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aem:gradeRecord:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['aem:gradeRecord:export']">导出</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="el-icon-upload2" size="mini" @click="handleImport" v-hasPermi="['aem:gradeRecord:import']">导入</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table ref="gradeRecordTable" v-loading="loading" :data="gradeRecordList" @selection-change="handleSelectionChange" :row-key="getRowKey" @expand-change="handleExpandChange">
      <el-table-column type="expand">
        <template slot-scope="props">
          <master-detail-panel
            :master-id="props.row.gradeId"
            foreign-key="gradeId"
            title="成绩复核"
            row-key="reviewId"
            :default-values="{ studentId: props.row.studentId, courseId: props.row.courseId }"
            :loader="loadReviews"
            :add-api="addGradeReview"
            :update-api="updateGradeReview"
            :delete-api="delGradeReview"
            :perms="{ add: ['aem:gradeReview:add'], edit: ['aem:gradeReview:edit'], remove: ['aem:gradeReview:remove'] }"
            :columns="reviewColumns"
          />
        </template>
      </el-table-column>
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学生ID" align="center" prop="studentId" />
      <el-table-column label="课程ID" align="center" prop="courseId" />
      <el-table-column label="学期ID" align="center" prop="semesterId" />
      <el-table-column label="考试类型" align="center" prop="examType"><template slot-scope="scope"><dict-tag :options="dict.type.aem_grade_exam_type" :value="scope.row.examType"/></template></el-table-column>
      <el-table-column label="平时成绩" align="center" prop="regularScore" />
      <el-table-column label="考试成绩" align="center" prop="examScore" />
      <el-table-column label="总成绩" align="center" prop="totalScore" />
      <el-table-column label="绩点" align="center" prop="gradePoint" />
      <el-table-column label="等级" align="center" prop="gradeLevel"><template slot-scope="scope"><dict-tag :options="dict.type.aem_grade_level" :value="scope.row.gradeLevel"/></template></el-table-column>
      <el-table-column label="是否通过" align="center" prop="isPass"><template slot-scope="scope"><dict-tag :options="dict.type.aem_is_pass" :value="scope.row.isPass"/></template></el-table-column>
      <el-table-column label="是否已复核" align="center" prop="isReviewed"><template slot-scope="scope"><dict-tag :options="dict.type.aem_is_reviewed" :value="scope.row.isReviewed"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="toggleExpand(scope.row)">明细</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aem:gradeRecord:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aem:gradeRecord:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="650px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="学生ID" prop="studentId"><el-input v-model="form.studentId" placeholder="请输入学生ID" /></el-form-item>
        <el-form-item label="课程ID" prop="courseId"><el-input v-model="form.courseId" placeholder="请输入课程ID" /></el-form-item>
        <el-form-item label="学期ID" prop="semesterId"><el-input v-model="form.semesterId" placeholder="请输入学期ID" /></el-form-item>
        <el-form-item label="考试类型" prop="examType"><el-select v-model="form.examType" placeholder="请选择考试类型" style="width:100%"><el-option v-for="dict in dict.type.aem_grade_exam_type" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
        <el-form-item label="平时成绩" prop="regularScore"><el-input-number v-model="form.regularScore" placeholder="请输入平时成绩" :min="0" :max="100" :precision="1" style="width:100%" @change="autoCalcTotal" /></el-form-item>
        <el-form-item label="考试成绩" prop="examScore"><el-input-number v-model="form.examScore" placeholder="请输入考试成绩" :min="0" :max="100" :precision="1" style="width:100%" @change="autoCalcTotal" /></el-form-item>
        <el-form-item label="总成绩" prop="totalScore"><el-input-number v-model="form.totalScore" placeholder="请输入总成绩" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item>
        <el-form-item label="绩点" prop="gradePoint"><el-input-number v-model="form.gradePoint" placeholder="请输入绩点" :min="0" :max="5" :precision="1" style="width:100%" /></el-form-item>
        <el-form-item label="等级" prop="gradeLevel"><el-select v-model="form.gradeLevel" placeholder="请选择等级" style="width:100%"><el-option v-for="dict in dict.type.aem_grade_level" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
        <el-form-item label="是否通过" prop="isPass"><el-radio-group v-model="form.isPass"><el-radio v-for="dict in dict.type.aem_is_pass" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio></el-radio-group></el-form-item>
        <el-form-item label="是否已复核" prop="isReviewed"><el-radio-group v-model="form.isReviewed"><el-radio v-for="dict in dict.type.aem_is_reviewed" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>

    <!-- 成绩导入对话框 -->
    <import-excel-dialog
      ref="importDialog"
      title="成绩导入"
      tip="仅允许导入 xls/xlsx 文件，总成绩留空时将按平时30%+考试70%自动计算。"
      :import-api="importGrade"
      template-url="aem/gradeRecord/importTemplate"
      template-name="gradeRecord_template"
      :extra-data="{}"
      @success="getList"/>
  </div>
</template>
<script>
import { listGradeRecord, getGradeRecord, delGradeRecord, addGradeRecord, updateGradeRecord, importGrade } from "@/api/aem/gradeRecord"
import { listGradeReview, addGradeReview, updateGradeReview, delGradeReview } from "@/api/aem/gradeReview"
import MasterDetailPanel from "../components/MasterDetailPanel"
import ImportExcelDialog from "../components/ImportExcelDialog"
export default {
  name: "GradeRecord",
  components: { MasterDetailPanel, ImportExcelDialog },
  dicts: ['aem_grade_exam_type', 'aem_grade_level', 'aem_is_pass', 'aem_is_reviewed', 'aem_review_type', 'aem_approve_status'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, gradeRecordList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, studentId: null, courseId: null, examType: null, isPass: null },
    form: {}, rules: {},
    reviewColumns: [
      { prop: 'originalScore', label: '原成绩', width: 100, type: 'number', min: 0, max: 100, precision: 1, required: true },
      { prop: 'newScore', label: '新成绩', width: 100, type: 'number', min: 0, max: 100, precision: 1, required: true },
      { prop: 'reviewType', label: '复核类型', width: 110, type: 'dict', dict: 'aem_review_type', defaultValue: '1' },
      { prop: 'reviewReason', label: '复核原因', required: true },
      { prop: 'approveStatus', label: '审批状态', width: 100, type: 'dict', dict: 'aem_approve_status', defaultValue: '0' },
      { prop: 'approveOpinion', label: '审批意见', editable: false }
    ]
  }},
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listGradeRecord(this.queryParams).then(response => { this.gradeRecordList = response.rows; this.total = response.total; this.loading = false }) },
    getRowKey(row) { return row.gradeId },
    handleExpandChange() { /* 子表自加载 */ },
    toggleExpand(row) { this.$refs.gradeRecordTable && this.$refs.gradeRecordTable.toggleRowExpansion(row) },
    loadReviews(gradeId) {
      return listGradeReview({ gradeId, pageNum: 1, pageSize: 1000 }).then(res => res.rows)
    },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { gradeId: null, studentId: null, courseId: null, semesterId: null, examType: "0", regularScore: null, examScore: null, totalScore: null, gradePoint: null, gradeLevel: null, isPass: "1", isReviewed: "0" }; this.resetForm("form") },
    // 平时/考试成绩变化时自动按30/70计算总成绩
    autoCalcTotal() {
      const r = this.form.regularScore == null ? 0 : Number(this.form.regularScore)
      const e = this.form.examScore == null ? 0 : Number(this.form.examScore)
      if (this.form.regularScore != null || this.form.examScore != null) {
        this.$set(this.form, 'totalScore', Math.round((r * 0.3 + e * 0.7) * 100) / 100)
      }
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.gradeId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加成绩记录" },
    handleUpdate(row) { this.reset(); const gradeId = row.gradeId || this.ids; getGradeRecord(gradeId).then(response => { this.form = response.data; this.open = true; this.title = "修改成绩记录" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.gradeId != null) { updateGradeRecord(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addGradeRecord(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const gradeIds = row.gradeId || this.ids; this.$modal.confirm('是否确认删除成绩记录编号为"' + gradeIds + '"的数据项？').then(function() { return delGradeRecord(gradeIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('aem/gradeRecord/export', { ...this.queryParams }, `gradeRecord_${new Date().getTime()}.xlsx`) },
    handleImport() { this.$refs.importDialog.open() }
  }
}
</script>
