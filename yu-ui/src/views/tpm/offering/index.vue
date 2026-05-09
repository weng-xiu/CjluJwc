<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="课程ID" prop="courseId"><el-input v-model="queryParams.courseId" placeholder="请输入课程ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="教师ID" prop="teacherId"><el-input v-model="queryParams.teacherId" placeholder="请输入教师ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['tpm:offering:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['tpm:offering:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['tpm:offering:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['tpm:offering:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="offeringList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学期ID" align="center" prop="semesterId" />
      <el-table-column label="课程ID" align="center" prop="courseId" />
      <el-table-column label="教师ID" align="center" prop="teacherId" />
      <el-table-column label="校区ID" align="center" prop="campusId" />
      <el-table-column label="教学班数" align="center" prop="classCount" />
      <el-table-column label="容量上限" align="center" prop="maxStudents" />
      <el-table-column label="开课状态" align="center" prop="offeringStatus" />
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['tpm:offering:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['tpm:offering:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="550px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="学期ID" prop="semesterId"><el-input v-model="form.semesterId" placeholder="请输入学期ID" /></el-form-item>
        <el-form-item label="课程ID" prop="courseId"><el-input v-model="form.courseId" placeholder="请输入课程ID" /></el-form-item>
        <el-form-item label="教师ID" prop="teacherId"><el-input v-model="form.teacherId" placeholder="请输入教师ID" /></el-form-item>
        <el-form-item label="校区ID" prop="campusId"><el-input v-model="form.campusId" placeholder="请输入校区ID" /></el-form-item>
        <el-form-item label="教学班数" prop="classCount"><el-input v-model="form.classCount" placeholder="请输入教学班数" /></el-form-item>
        <el-form-item label="容量上限" prop="maxStudents"><el-input v-model="form.maxStudents" placeholder="请输入容量上限" /></el-form-item>
        <el-form-item label="开课状态" prop="offeringStatus"><el-input v-model="form.offeringStatus" placeholder="请输入开课状态" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listOffering, getOffering, delOffering, addOffering, updateOffering } from "@/api/tpm/offering"
export default {
  name: "Offering", dicts: ['sys_normal_disable'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, offeringList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, courseId: null, teacherId: null, status: null },
    form: {}, rules: { semesterId: [{ required: true, message: "学期ID不能为空", trigger: "blur" }], courseId: [{ required: true, message: "课程ID不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listOffering(this.queryParams).then(response => { this.offeringList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { offeringId: null, semesterId: null, courseId: null, teacherId: null, campusId: null, classCount: null, maxStudents: null, offeringStatus: null, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.offeringId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加开课计划" },
    handleUpdate(row) { this.reset(); const offeringId = row.offeringId || this.ids; getOffering(offeringId).then(response => { this.form = response.data; this.open = true; this.title = "修改开课计划" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.offeringId != null) { updateOffering(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addOffering(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const offeringIds = row.offeringId || this.ids; this.$modal.confirm('是否确认删除开课计划编号为"' + offeringIds + '"的数据项？').then(function() { return delOffering(offeringIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('tpm/offering/export', { ...this.queryParams }, `offering_${new Date().getTime()}.xlsx`) }
  }
}
</script>
