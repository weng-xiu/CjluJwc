<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="可授课程类别" prop="courseCategory"><el-input v-model="queryParams.courseCategory" placeholder="请输入可授课程类别" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['brm:qualification:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['brm:qualification:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['brm:qualification:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['brm:qualification:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="qualificationList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="教师ID" align="center" prop="teacherId" />
      <el-table-column label="可授课程类别" align="center" prop="courseCategory" />
      <el-table-column label="认证机构" align="center" prop="certifyAuthority" />
      <el-table-column label="获证日期" align="center" prop="qualifyDate" width="180"><template slot-scope="scope"><span>{{ parseTime(scope.row.qualifyDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="到期日期" align="center" prop="expireDate" width="180"><template slot-scope="scope"><span>{{ parseTime(scope.row.expireDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['brm:qualification:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['brm:qualification:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="教师ID" prop="teacherId"><el-input v-model="form.teacherId" placeholder="请输入教师ID" /></el-form-item>
        <el-form-item label="可授课程类别" prop="courseCategory"><el-input v-model="form.courseCategory" placeholder="请输入可授课程类别" /></el-form-item>
        <el-form-item label="认证机构" prop="certifyAuthority"><el-input v-model="form.certifyAuthority" placeholder="请输入认证机构" /></el-form-item>
        <el-form-item label="获证日期" prop="qualifyDate"><el-date-picker clearable v-model="form.qualifyDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择获证日期" /></el-form-item>
        <el-form-item label="到期日期" prop="expireDate"><el-date-picker clearable v-model="form.expireDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择到期日期" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listQualification, getQualification, delQualification, addQualification, updateQualification } from "@/api/brm/qualification"
export default {
  name: "Qualification", dicts: ['sys_normal_disable'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, qualificationList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, courseCategory: null, status: null },
    form: {}, rules: { teacherId: [{ required: true, message: "教师ID不能为空", trigger: "blur" }], courseCategory: [{ required: true, message: "可授课程类别不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listQualification(this.queryParams).then(response => { this.qualificationList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { qualId: null, teacherId: null, courseCategory: null, certifyAuthority: null, qualifyDate: null, expireDate: null, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.qualId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加授课资格" },
    handleUpdate(row) { this.reset(); const qualId = row.qualId || this.ids; getQualification(qualId).then(response => { this.form = response.data; this.open = true; this.title = "修改授课资格" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.qualId != null) { updateQualification(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addQualification(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const qualIds = row.qualId || this.ids; this.$modal.confirm('是否确认删除资格编号为"' + qualIds + '"的数据项？').then(function() { return delQualification(qualIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('brm/qualification/export', { ...this.queryParams }, `qualification_${new Date().getTime()}.xlsx`) }
  }
}
</script>
