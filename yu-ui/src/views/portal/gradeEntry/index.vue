<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="课程名称" prop="courseName"><el-input v-model="queryParams.courseName" placeholder="请输入课程名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="学生姓名" prop="studentName"><el-input v-model="queryParams.studentName" placeholder="请输入学生姓名" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['portal:gradeEntry:add']">录入成绩</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['portal:gradeEntry:edit']">修改成绩</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-menu" size="mini" @click="matrixOpen = true" v-hasPermi="['portal:gradeEntry:list']">批量录入</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <app-skeleton :loading="loading && !gradeList.length" :rows="6">
    <el-table v-loading="loading" :data="gradeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学生姓名" align="center" prop="studentName" />
      <el-table-column label="学号" align="center" prop="studentNo" width="140" />
      <el-table-column label="课程名称" align="center" prop="courseName" show-overflow-tooltip />
      <el-table-column label="平时成绩" align="center" prop="regularScore" width="90" />
      <el-table-column label="考试成绩" align="center" prop="examScore" width="90" />
      <el-table-column label="总成绩" align="center" prop="totalScore" width="90" />
      <el-table-column label="绩点" align="center" prop="gradePoint" width="80" />
      <el-table-column label="提交状态" align="center" prop="submitStatus" width="110"><template slot-scope="scope"><el-tag :type="submitStatusTag(scope.row.submitStatus)" size="mini">{{ submitStatusText(scope.row.submitStatus) }}</el-tag></template></el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="100">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['portal:gradeEntry:edit']">修改</el-button>
        </template>
      </el-table-column>
    </el-table>
    <app-empty v-if="!loading && !gradeList.length" description="暂无门户内容，可调整筛选条件或先发布" />
    </app-skeleton>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :close-on-click-modal="false" :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="课程" prop="courseName"><el-input v-model="form.courseName" placeholder="请输入课程名称" /></el-form-item>
        <el-form-item label="学生" prop="studentName"><el-input v-model="form.studentName" placeholder="请输入学生姓名" /></el-form-item>
        <el-form-item label="平时成绩" prop="regularScore"><el-input-number v-model="form.regularScore" controls-position="right" :min="0" :max="100" :precision="1" /></el-form-item>
        <el-form-item label="考试成绩" prop="examScore"><el-input-number v-model="form.examScore" controls-position="right" :min="0" :max="100" :precision="1" /></el-form-item>
        <el-form-item label="考试类型" prop="examType"><el-select v-model="form.examType" placeholder="请选择"><el-option v-for="t in examTypeOptions" :key="t.value" :label="t.label" :value="t.value"/></el-select></el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" placeholder="请输入备注" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
    <!-- 成绩录入矩阵（按教学班批量录入，N7 核心交互组件 Vue2 版） -->
    <grade-entry-matrix :visible.sync="matrixOpen" :rows="gradeList" :loading="loading" @success="getList" />
  </div>
</template>
<script>
// N7/A7 残项收口（自 yu-ui-vue3 同页对齐）：
// 字段口径修正——AemGradeRecord 无 score / examTypeName 字段，原 Vue2 表格这两列恒空白、单条表单提交的
// score 后端不落库；改绑真实的 regularScore / examScore / totalScore / submitStatus。
// 考试类型口径为成绩记录专属「0正考 1补考 2重修」，与考试计划 aem_exam_plan.exam_type 标签不同一套。
// 「批量录入」接入新封装的 GradeEntryMatrix 组件（§6.1 N7 核心交互组件）。
import { listGradeEntry, addGradeEntry, updateGradeEntry } from "@/api/portal/grade"
import GradeEntryMatrix from "@/components/GradeEntryMatrix"
export default {
  name: "PortalGradeEntry", components: { GradeEntryMatrix }, dicts: ['sys_normal_disable'],
  data() { return { loading: true, ids: [], single: true, showSearch: true, total: 0, gradeList: [], title: "", open: false, matrixOpen: false,
    examTypeOptions: [ { value: "0", label: "正考" }, { value: "1", label: "补考" }, { value: "2", label: "重修" } ],
    queryParams: { pageNum: 1, pageSize: 10, courseName: null, studentName: null },
    form: {}, rules: { regularScore: [{ required: true, message: "平时成绩不能为空", trigger: "blur" }], examScore: [{ required: true, message: "考试成绩不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listGradeEntry(this.queryParams).then(response => { this.gradeList = response.rows; this.total = response.total; this.loading = false }).catch(() => { this.loading = false }) },
    submitStatusText(s) { return { '0': '未提交', '1': '已提交待审', '2': '已锁定', '3': '已驳回' }[s || '0'] || '未提交' },
    submitStatusTag(s) { return { '0': 'info', '1': 'warning', '2': 'success', '3': 'danger' }[s || '0'] || 'info' },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { gradeId: null, studentId: null, courseId: null, courseName: null, studentName: null, regularScore: null, examScore: null, examType: "0", remark: null }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.gradeId); this.single = selection.length !== 1 },
    handleAdd() { this.reset(); this.open = true; this.title = "录入成绩" },
    handleUpdate(row) { this.reset(); const gradeId = row.gradeId || this.ids; listGradeEntry({ gradeId }).then(response => { if (response.rows && response.rows.length > 0) { this.form = response.rows[0] } this.open = true; this.title = "修改成绩" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.gradeId != null) { updateGradeEntry(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addGradeEntry(this.form).then(response => { this.$modal.msgSuccess("录入成功"); this.open = false; this.getList() }) } } }) }
  }
}
</script>
