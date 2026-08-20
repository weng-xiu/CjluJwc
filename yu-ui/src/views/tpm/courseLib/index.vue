<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="课程名称" prop="courseName"><el-input v-model="queryParams.courseName" placeholder="请输入课程名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="课程编码" prop="courseCode"><el-input v-model="queryParams.courseCode" placeholder="请输入课程编码" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="所属方案" prop="planId">
        <el-select v-model="queryParams.planId" placeholder="请选择培养方案" clearable filterable>
          <el-option v-for="p in planOptions" :key="p.planId" :label="p.planName" :value="p.planId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="课程类型" prop="courseType">
        <el-select v-model="queryParams.courseType" placeholder="请选择课程类型" clearable>
          <el-option v-for="dict in dict.type.tpm_course_type" :key="dict.value" :label="dict.label" :value="dict.value"/>
        </el-select>
      </el-form-item>
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
      <el-table-column label="课程编码" align="center" prop="courseCode" width="120" />
      <el-table-column label="课程名称" align="center" prop="courseName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="英文名称" align="center" prop="courseNameEn" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="学分" align="center" prop="credit" width="70" />
      <el-table-column label="课程类型" align="center" prop="courseType" width="90"><template slot-scope="scope"><dict-tag :options="dict.type.tpm_course_type" :value="scope.row.courseType"/></template></el-table-column>
      <el-table-column label="课程类别" align="center" prop="courseCategory" width="110"><template slot-scope="scope"><dict-tag :options="dict.type.tpm_course_category" :value="scope.row.courseCategory"/></template></el-table-column>
      <el-table-column label="考核方式" align="center" prop="assessmentMethod" width="90"><template slot-scope="scope"><dict-tag :options="dict.type.tpm_assessment" :value="scope.row.assessmentMethod"/></template></el-table-column>
      <el-table-column label="理论学时" align="center" prop="theoryHours" width="80" />
      <el-table-column label="实践学时" align="center" prop="practiceHours" width="80" />
      <el-table-column label="总学时" align="center" prop="totalHours" width="80" />
      <el-table-column label="修读学期" align="center" prop="semesterOrder" width="80" />
      <el-table-column label="所属方案" align="center" prop="planName" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="status" width="70"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['tpm:course:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['tpm:course:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="720px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12"><el-form-item label="课程编码" prop="courseCode"><el-input v-model="form.courseCode" placeholder="请输入课程编码" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="课程名称" prop="courseName"><el-input v-model="form.courseName" placeholder="请输入课程名称" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="英文名称" prop="courseNameEn"><el-input v-model="form.courseNameEn" placeholder="请输入英文名称" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="所属方案" prop="planId">
            <el-select v-model="form.planId" placeholder="请选择培养方案" filterable clearable style="width:100%">
              <el-option v-for="p in planOptions" :key="p.planId" :label="p.planName" :value="p.planId"/>
            </el-select>
          </el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="学分" prop="credit"><el-input-number v-model="form.credit" :min="0" :precision="1" :step="1" controls-position="right" style="width:100%"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="修读学期" prop="semesterOrder"><el-input-number v-model="form.semesterOrder" :min="1" :max="20" :step="1" controls-position="right" style="width:100%"/></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="8"><el-form-item label="理论学时" prop="theoryHours"><el-input-number v-model="form.theoryHours" :min="0" :step="1" controls-position="right" style="width:100%" @change="sumTotalHours"/></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="实践学时" prop="practiceHours"><el-input-number v-model="form.practiceHours" :min="0" :step="1" controls-position="right" style="width:100%" @change="sumTotalHours"/></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="总学时" prop="totalHours"><el-input-number v-model="form.totalHours" :min="0" :step="1" controls-position="right" style="width:100%"/></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="8"><el-form-item label="课程类型" prop="courseType">
            <el-select v-model="form.courseType" placeholder="请选择课程类型" clearable style="width:100%">
              <el-option v-for="dict in dict.type.tpm_course_type" :key="dict.value" :label="dict.label" :value="dict.value"/>
            </el-select>
          </el-form-item></el-col>
          <el-col :span="8"><el-form-item label="课程类别" prop="courseCategory">
            <el-select v-model="form.courseCategory" placeholder="请选择课程类别" clearable style="width:100%">
              <el-option v-for="dict in dict.type.tpm_course_category" :key="dict.value" :label="dict.label" :value="dict.value"/>
            </el-select>
          </el-form-item></el-col>
          <el-col :span="8"><el-form-item label="考核方式" prop="assessmentMethod">
            <el-select v-model="form.assessmentMethod" placeholder="请选择考核方式" clearable style="width:100%">
              <el-option v-for="dict in dict.type.tpm_assessment" :key="dict.value" :label="dict.label" :value="dict.value"/>
            </el-select>
          </el-form-item></el-col>
        </el-row>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listCourseLib, getCourseLib, delCourseLib, addCourseLib, updateCourseLib } from "@/api/tpm/courseLib"
import { listPlan } from "@/api/tpm/plan"

export default {
  name: "CourseLib",
  dicts: ['sys_normal_disable', 'tpm_course_type', 'tpm_course_category', 'tpm_assessment'],
  data() {
    return {
      loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, courseLibList: [], title: "", open: false,
      planOptions: [],
      queryParams: { pageNum: 1, pageSize: 10, courseName: null, courseCode: null, planId: null, courseType: null, status: null },
      form: {},
      rules: {
        courseCode: [{ required: true, message: "课程编码不能为空", trigger: "blur" }],
        courseName: [{ required: true, message: "课程名称不能为空", trigger: "blur" }]
      }
    }
  },
  created() {
    this.getList()
    this.loadPlans()
  },
  methods: {
    getList() {
      this.loading = true
      listCourseLib(this.queryParams).then(response => { this.courseLibList = response.rows; this.total = response.total; this.loading = false })
    },
    loadPlans() {
      listPlan({ pageNum: 1, pageSize: 1000 }).then(response => { this.planOptions = response.rows || [] })
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { courseId: null, courseCode: null, courseName: null, courseNameEn: null, credit: null, theoryHours: null, practiceHours: null, totalHours: null, courseType: null, courseCategory: null, assessmentMethod: null, semesterOrder: null, planId: null, status: "0" }
      this.resetForm("form")
    },
    sumTotalHours() {
      const t = Number(this.form.theoryHours) || 0
      const p = Number(this.form.practiceHours) || 0
      this.form.totalHours = t + p
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.courseId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加课程" },
    handleUpdate(row) { this.reset(); const courseId = row.courseId || this.ids; getCourseLib(courseId).then(response => { this.form = response.data; this.open = true; this.title = "修改课程" }) },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.courseId != null) {
            updateCourseLib(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() })
          } else {
            addCourseLib(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() })
          }
        }
      })
    },
    handleDelete(row) {
      const courseIds = row.courseId || this.ids
      this.$modal.confirm('是否确认删除课程编号为"' + courseIds + '"的数据项？').then(function() { return delCourseLib(courseIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    },
    handleExport() { this.download('tpm/courseLib/export', { ...this.queryParams }, `courseLib_${new Date().getTime()}.xlsx`) }
  }
}
</script>
