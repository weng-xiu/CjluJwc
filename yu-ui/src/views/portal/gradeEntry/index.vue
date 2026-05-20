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
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="gradeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学生姓名" align="center" prop="studentName" />
      <el-table-column label="学号" align="center" prop="studentNo" />
      <el-table-column label="课程名称" align="center" prop="courseName" />
      <el-table-column label="成绩" align="center" prop="score" />
      <el-table-column label="绩点" align="center" prop="gradePoint" />
      <el-table-column label="考试类型" align="center" prop="examTypeName" />
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="课程" prop="courseName"><el-input v-model="form.courseName" placeholder="请选择课程" /></el-form-item>
        <el-form-item label="学生" prop="studentName"><el-input v-model="form.studentName" placeholder="请选择学生" /></el-form-item>
        <el-form-item label="成绩" prop="score"><el-input v-model="form.score" placeholder="请输入成绩" /></el-form-item>
        <el-form-item label="考试类型" prop="examType"><el-select v-model="form.examType" placeholder="请选择"><el-option label="期末考试" value="0"/><el-option label="补考" value="1"/></el-select></el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" placeholder="请输入备注" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listGradeEntry, addGradeEntry, updateGradeEntry } from "@/api/portal/grade"
export default {
  name: "PortalGradeEntry", dicts: ['sys_normal_disable'],
  data() { return { loading: true, ids: [], single: true, showSearch: true, total: 0, gradeList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, courseName: null, studentName: null },
    form: {}, rules: { score: [{ required: true, message: "成绩不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listGradeEntry(this.queryParams).then(response => { this.gradeList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { studentId: null, courseId: null, courseName: null, studentName: null, score: null, examType: "0", remark: null }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.gradeId); this.single = selection.length !== 1 },
    handleAdd() { this.reset(); this.open = true; this.title = "录入成绩" },
    handleUpdate(row) { this.reset(); const gradeId = row.gradeId || this.ids; listGradeEntry({ gradeId }).then(response => { if (response.rows && response.rows.length > 0) { this.form = response.rows[0] } this.open = true; this.title = "修改成绩" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.gradeId != null) { updateGradeEntry(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addGradeEntry(this.form).then(response => { this.$modal.msgSuccess("录入成功"); this.open = false; this.getList() }) } } }) }
  }
}
</script>
