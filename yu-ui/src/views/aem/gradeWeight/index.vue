<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="课程" prop="courseId">
        <el-select v-model="queryParams.courseId" placeholder="请选择课程" clearable filterable>
          <el-option v-for="c in courseOptions" :key="c.courseId" :label="c.courseName" :value="c.courseId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="课程类别" prop="courseCategory">
        <el-select v-model="queryParams.courseCategory" placeholder="请选择类别" clearable>
          <el-option v-for="d in dict.type.tpm_course_category" :key="d.value" :label="d.label" :value="d.value"/>
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aem:gradeWeight:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['aem:gradeWeight:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aem:gradeWeight:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['aem:gradeWeight:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-alert title="总评权重按「课程级 > 课程类别级 > 全局默认」优先级匹配；两者均未配置的科目按平时30% + 考试70% 计算。" type="info" show-icon :closable="false" class="mb8"/>
    <el-table v-loading="loading" :data="weightList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="配置ID" align="center" prop="weightId" width="80" />
      <el-table-column label="配置级别" align="center" width="100">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.courseId" size="small">课程</el-tag>
          <el-tag v-else-if="scope.row.courseCategory" size="small" type="warning">类别</el-tag>
          <el-tag v-else size="small" type="info">全局</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="课程" align="center" prop="courseName" :show-overflow-tooltip="true" />
      <el-table-column label="课程类别" align="center" prop="courseCategory"><template slot-scope="scope"><dict-tag :options="dict.type.tpm_course_category" :value="scope.row.courseCategory"/></template></el-table-column>
      <el-table-column label="平时占比(%)" align="center" prop="regularRatio" width="110" />
      <el-table-column label="考试占比(%)" align="center" prop="examRatio" width="110" />
      <el-table-column label="状态" align="center" prop="status" width="80"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aem:gradeWeight:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aem:gradeWeight:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="课程" prop="courseId">
          <el-select v-model="form.courseId" placeholder="不选则按类别或全局配置" clearable filterable style="width:100%">
            <el-option v-for="c in courseOptions" :key="c.courseId" :label="c.courseName" :value="c.courseId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="课程类别" prop="courseCategory">
          <el-select v-model="form.courseCategory" placeholder="课程为空时按类别匹配" clearable style="width:100%">
            <el-option v-for="d in dict.type.tpm_course_category" :key="d.value" :label="d.label" :value="d.value"/>
          </el-select>
        </el-form-item>
        <el-form-item label="平时占比(%)" prop="regularRatio">
          <el-input-number v-model="form.regularRatio" :min="0" :max="100" :precision="1" :step="5" controls-position="right" style="width:100%" @change="onRegularChange"/>
        </el-form-item>
        <el-form-item label="考试占比(%)" prop="examRatio">
          <el-input-number v-model="form.examRatio" :min="0" :max="100" :precision="1" :step="5" controls-position="right" style="width:100%"/>
        </el-form-item>
        <el-form-item label="状态" prop="status"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listGradeWeight, getGradeWeight, delGradeWeight, addGradeWeight, updateGradeWeight } from "@/api/aem/gradeWeight"
import { listCourseLib } from "@/api/tpm/courseLib"
export default {
  name: "GradeWeight",
  dicts: ['sys_normal_disable', 'tpm_course_category'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      weightList: [],
      courseOptions: [],
      title: "",
      open: false,
      queryParams: { pageNum: 1, pageSize: 10, courseId: null, courseCategory: null, status: null },
      form: {},
      rules: {
        regularRatio: [{ required: true, message: "平时占比不能为空", trigger: "blur" }],
        examRatio: [{ required: true, message: "考试占比不能为空", trigger: "blur" }]
      }
    }
  },
  created() {
    this.getList()
    listCourseLib({ pageNum: 1, pageSize: 1000 }).then(response => { this.courseOptions = response.rows || [] })
  },
  methods: {
    getList() {
      this.loading = true;
      listGradeWeight(this.queryParams).then(response => { this.weightList = response.rows; this.total = response.total; this.loading = false })
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { weightId: null, courseId: null, courseCategory: null, regularRatio: 30, examRatio: 70, status: "0", remark: null };
      this.resetForm("form")
    },
    /** 平时占比联动考试占比，保证和为100 */
    onRegularChange(val) { this.form.examRatio = val == null ? 70 : Math.round((100 - val) * 10) / 10 },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.weightId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加成绩权重配置" },
    handleUpdate(row) {
      this.reset();
      const weightId = row.weightId || this.ids;
      getGradeWeight(weightId).then(response => { this.form = response.data; this.open = true; this.title = "修改成绩权重配置" })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (Math.abs((this.form.regularRatio || 0) + (this.form.examRatio || 0) - 100) > 0.001) {
            this.$modal.msgError("平时占比与考试占比之和必须等于100")
            return
          }
          if (this.form.weightId != null) {
            updateGradeWeight(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() })
          } else {
            addGradeWeight(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() })
          }
        }
      })
    },
    handleDelete(row) {
      const weightIds = row.weightId || this.ids;
      this.$modal.confirm('是否确认删除成绩权重配置编号为"' + weightIds + '"的数据项？').then(function() { return delGradeWeight(weightIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    },
    handleExport() { this.download('aem/gradeWeight/export', { ...this.queryParams }, `gradeWeight_${new Date().getTime()}.xlsx`) }
  }
}
</script>
