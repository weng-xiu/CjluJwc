<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="专业名称" prop="majorName"><el-input v-model="queryParams.majorName" placeholder="请输入专业名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="所属院系" prop="deptId"><el-select v-model="queryParams.deptId" placeholder="请选择院系" clearable filterable><el-option v-for="d in deptList" :key="d.deptId" :label="d.deptName" :value="d.deptId"/></el-select></el-form-item>
      <el-form-item label="学历层次" prop="educationLevel"><el-select v-model="queryParams.educationLevel" placeholder="请选择学历层次" clearable><el-option v-for="dict in dict.type.brm_education_level" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['brm:major:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['brm:major:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['brm:major:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['brm:major:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="majorList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="专业编码" align="center" prop="majorCode" />
      <el-table-column label="专业名称" align="center" prop="majorName" />
      <el-table-column label="所属院系" align="center" prop="deptName" />
      <el-table-column label="学历层次" align="center" prop="educationLevel"><template slot-scope="scope"><dict-tag :options="dict.type.brm_education_level" :value="scope.row.educationLevel"/></template></el-table-column>
      <el-table-column label="学制(年)" align="center" prop="duration" />
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['brm:major:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['brm:major:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="专业编码" prop="majorCode"><el-input v-model="form.majorCode" placeholder="请输入专业编码" /></el-form-item>
        <el-form-item label="专业名称" prop="majorName"><el-input v-model="form.majorName" placeholder="请输入专业名称" /></el-form-item>
        <el-form-item label="所属院系" prop="deptId">
          <el-select v-model="form.deptId" placeholder="请选择院系" clearable filterable>
            <el-option v-for="d in deptList" :key="d.deptId" :label="d.deptName" :value="d.deptId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="学历层次"><el-select v-model="form.educationLevel" placeholder="请选择学历层次" style="width:100%"><el-option v-for="dict in dict.type.brm_education_level" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
        <el-form-item label="学制(年)"><el-input-number v-model="form.duration" :min="1" :max="10" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listMajor, getMajor, delMajor, addMajor, updateMajor } from "@/api/brm/major"
import { listDept } from "@/api/brm/dept"
export default {
  name: "Major", dicts: ['sys_normal_disable', 'brm_education_level'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, majorList: [], deptList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, majorName: null, deptId: null, educationLevel: null, status: null },
    form: {}, rules: { majorCode: [{ required: true, message: "专业编码不能为空", trigger: "blur" }], majorName: [{ required: true, message: "专业名称不能为空", trigger: "blur" }], deptId: [{ required: true, message: "请选择院系", trigger: "change" }] } }
  },
  created() { this.getList(); this.loadDepts() },
  methods: {
    getList() { this.loading = true; listMajor(this.queryParams).then(response => { this.majorList = response.rows; this.total = response.total; this.loading = false }) },
    loadDepts() { listDept({ pageNum: 1, pageSize: 200 }).then(response => { this.deptList = response.data }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { majorId: null, majorCode: null, majorName: null, deptId: null, educationLevel: null, duration: 4, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.majorId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加专业" },
    handleUpdate(row) { this.reset(); const majorId = row.majorId || this.ids; getMajor(majorId).then(response => { this.form = response.data; this.open = true; this.title = "修改专业" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.majorId != null) { updateMajor(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addMajor(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const majorIds = row.majorId || this.ids; this.$modal.confirm('是否确认删除专业编号为"' + majorIds + '"的数据项？').then(function() { return delMajor(majorIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('brm/major/export', { ...this.queryParams }, `major_${new Date().getTime()}.xlsx`) }
  }
}
</script>
