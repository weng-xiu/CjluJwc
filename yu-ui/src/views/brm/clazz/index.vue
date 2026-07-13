<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="班级名称" prop="className"><el-input v-model="queryParams.className" placeholder="请输入班级名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['brm:class:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['brm:class:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['brm:class:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['brm:class:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="clazzList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="班级编码" align="center" prop="classCode" />
      <el-table-column label="班级名称" align="center" prop="className" />
      <el-table-column label="所属专业" align="center" prop="majorId">
        <template slot-scope="scope">
          <span>{{ getMajorName(scope.row.majorId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="年级" align="center" prop="grade" />
      <el-table-column label="学生人数" align="center" prop="studentCount" />
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['brm:class:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['brm:class:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="班级编码" prop="classCode"><el-input v-model="form.classCode" placeholder="请输入班级编码" /></el-form-item>
        <el-form-item label="班级名称" prop="className"><el-input v-model="form.className" placeholder="请输入班级名称" /></el-form-item>
        <el-form-item label="所属院系">
          <el-select v-model="selectedDeptId" placeholder="请选择院系" clearable filterable @change="handleDeptChange">
            <el-option v-for="d in deptList" :key="d.deptId" :label="d.deptName" :value="d.deptId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="所属专业" prop="majorId">
          <el-select v-model="form.majorId" placeholder="请先选择院系" clearable filterable :disabled="!selectedDeptId">
            <el-option v-for="m in majorList" :key="m.majorId" :label="m.majorName" :value="m.majorId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="年级" prop="grade"><el-input v-model="form.grade" placeholder="请输入年级" /></el-form-item>
        <el-form-item label="学生人数"><el-input-number v-model="form.studentCount" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listClazz, getClazz, delClazz, addClazz, updateClazz } from "@/api/brm/clazz"
import { listDept } from "@/api/brm/dept"
import { listMajor, getMajor } from "@/api/brm/major"
export default {
  name: "Clazz", dicts: ['sys_normal_disable'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, clazzList: [], deptList: [], majorList: [], selectedDeptId: null, title: "", open: false, majorNameMap: {},
    queryParams: { pageNum: 1, pageSize: 10, className: null, status: null },
    form: {}, rules: { classCode: [{ required: true, message: "班级编码不能为空", trigger: "blur" }], className: [{ required: true, message: "班级名称不能为空", trigger: "blur" }], majorId: [{ required: true, message: "请选择专业", trigger: "change" }] } }
  },
  created() { this.loadMajorNameMap(); this.getList() },
  methods: {
    getList() { this.loading = true; listClazz(this.queryParams).then(response => { this.clazzList = response.rows; this.total = response.total; this.loading = false }) },
    loadDepts() { listDept({ pageNum: 1, pageSize: 200 }).then(response => { this.deptList = response.rows }) },
    handleDeptChange(deptId) {
      this.form.majorId = null
      this.majorList = []
      if (deptId) {
        listMajor({ deptId: deptId, pageNum: 1, pageSize: 200 }).then(response => { this.majorList = response.rows })
      }
    },
    loadMajorNameMap() {
      listMajor({ pageNum: 1, pageSize: 500 }).then(response => {
        const map = {}
        response.rows.forEach(m => { map[m.majorId] = m.majorName })
        this.majorNameMap = map
      })
    },
    getMajorName(majorId) {
      return this.majorNameMap[majorId] || majorId
    },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { classId: null, classCode: null, className: null, majorId: null, grade: null, studentCount: 0, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.classId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.loadDepts(); this.selectedDeptId = null; this.majorList = []; this.open = true; this.title = "添加班级" },
    handleUpdate(row) {
      this.reset(); this.loadDepts(); this.selectedDeptId = null; this.majorList = [];
      const classId = row.classId || this.ids;
      getClazz(classId).then(response => {
        this.form = response.data;
        if (this.form.majorId) {
          getMajor(this.form.majorId).then(majorRes => {
            this.selectedDeptId = majorRes.data.deptId;
            listMajor({ deptId: this.selectedDeptId, pageNum: 1, pageSize: 200 }).then(r => {
              this.majorList = r.rows;
              this.open = true; this.title = "修改班级";
            });
          });
        } else {
          this.open = true; this.title = "修改班级";
        }
      });
    },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.classId != null) { updateClazz(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addClazz(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const classIds = row.classId || this.ids; this.$modal.confirm('是否确认删除班级编号为"' + classIds + '"的数据项？').then(function() { return delClazz(classIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('brm/clazz/export', { ...this.queryParams }, `clazz_${new Date().getTime()}.xlsx`) }
  }
}
</script>
