<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="课程名称" prop="courseName"><el-input v-model="queryParams.courseName" placeholder="请输入课程名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="课程编码" prop="courseCode"><el-input v-model="queryParams.courseCode" placeholder="请输入课程编码" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['tpm:course:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['tpm:course:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['tpm:course:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['tpm:course:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="courseLibList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="课程编码" align="center" prop="courseCode" />
      <el-table-column label="课程名称" align="center" prop="courseName" />
      <el-table-column label="学分" align="center" prop="credit" />
      <el-table-column label="理论学时" align="center" prop="theoryHours" />
      <el-table-column label="实践学时" align="center" prop="practiceHours" />
      <el-table-column label="总学时" align="center" prop="totalHours" />
      <el-table-column label="课程类型" align="center" prop="courseType" />
      <el-table-column label="考核方式" align="center" prop="assessmentMethod" />
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['tpm:course:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['tpm:course:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="700px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="课程编码" prop="courseCode"><el-input v-model="form.courseCode" placeholder="请输入课程编码" /></el-form-item>
        <el-form-item label="课程名称" prop="courseName"><el-input v-model="form.courseName" placeholder="请输入课程名称" /></el-form-item>
        <el-form-item label="英文名称" prop="courseNameEn"><el-input v-model="form.courseNameEn" placeholder="请输入英文名称" /></el-form-item>
        <el-form-item label="学分" prop="credit"><el-input v-model="form.credit" placeholder="请输入学分" /></el-form-item>
        <el-form-item label="理论学时" prop="theoryHours"><el-input v-model="form.theoryHours" placeholder="请输入理论学时" /></el-form-item>
        <el-form-item label="实践学时" prop="practiceHours"><el-input v-model="form.practiceHours" placeholder="请输入实践学时" /></el-form-item>
        <el-form-item label="总学时" prop="totalHours"><el-input v-model="form.totalHours" placeholder="请输入总学时" /></el-form-item>
        <el-form-item label="课程类型" prop="courseType"><el-input v-model="form.courseType" placeholder="请输入课程类型" /></el-form-item>
        <el-form-item label="课程类别" prop="courseCategory"><el-input v-model="form.courseCategory" placeholder="请输入课程类别" /></el-form-item>
        <el-form-item label="考核方式" prop="assessmentMethod"><el-input v-model="form.assessmentMethod" placeholder="请输入考核方式" /></el-form-item>
        <el-form-item label="建议修读学期" prop="semesterOrder"><el-input v-model="form.semesterOrder" placeholder="请输入建议修读学期" /></el-form-item>
        <el-form-item label="所属方案ID" prop="planId"><el-input v-model="form.planId" placeholder="请输入所属方案ID" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listCourseLib, getCourseLib, delCourseLib, addCourseLib, updateCourseLib } from "@/api/tpm/courseLib"
export default {
  name: "CourseLib", dicts: ['sys_normal_disable'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, courseLibList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, courseName: null, courseCode: null, status: null },
    form: {}, rules: { courseCode: [{ required: true, message: "课程编码不能为空", trigger: "blur" }], courseName: [{ required: true, message: "课程名称不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listCourseLib(this.queryParams).then(response => { this.courseLibList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { courseId: null, courseCode: null, courseName: null, courseNameEn: null, credit: null, theoryHours: null, practiceHours: null, totalHours: null, courseType: null, courseCategory: null, assessmentMethod: null, semesterOrder: null, planId: null, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.courseId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加课程" },
    handleUpdate(row) { this.reset(); const courseId = row.courseId || this.ids; getCourseLib(courseId).then(response => { this.form = response.data; this.open = true; this.title = "修改课程" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.courseId != null) { updateCourseLib(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addCourseLib(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const courseIds = row.courseId || this.ids; this.$modal.confirm('是否确认删除课程编号为"' + courseIds + '"的数据项？').then(function() { return delCourseLib(courseIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('tpm/courseLib/export', { ...this.queryParams }, `courseLib_${new Date().getTime()}.xlsx`) }
  }
}
</script>
