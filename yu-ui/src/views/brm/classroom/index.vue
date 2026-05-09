<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="教室名称" prop="classroomName"><el-input v-model="queryParams.classroomName" placeholder="请输入教室名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['brm:classroom:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['brm:classroom:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['brm:classroom:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['brm:classroom:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="classroomList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="教室名称" align="center" prop="classroomName" />
      <el-table-column label="所属教学楼" align="center" prop="buildingId" />
      <el-table-column label="教室类型" align="center" prop="typeId" />
      <el-table-column label="容纳人数" align="center" prop="capacity" />
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['brm:classroom:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['brm:classroom:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="教室名称" prop="classroomName"><el-input v-model="form.classroomName" placeholder="请输入教室名称" /></el-form-item>
        <el-form-item label="所属教学楼" prop="buildingId"><el-input v-model="form.buildingId" placeholder="请输入所属教学楼ID" /></el-form-item>
        <el-form-item label="教室类型" prop="typeId"><el-input v-model="form.typeId" placeholder="请输入教室类型ID" /></el-form-item>
        <el-form-item label="容纳人数"><el-input-number v-model="form.capacity" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listClassroom, getClassroom, delClassroom, addClassroom, updateClassroom } from "@/api/brm/classroom"
export default {
  name: "Classroom", dicts: ['sys_normal_disable'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, classroomList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, classroomName: null, status: null },
    form: {}, rules: { classroomName: [{ required: true, message: "教室名称不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listClassroom(this.queryParams).then(response => { this.classroomList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { classroomId: null, classroomName: null, buildingId: null, typeId: null, capacity: 0, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.classroomId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加教室" },
    handleUpdate(row) { this.reset(); const classroomId = row.classroomId || this.ids; getClassroom(classroomId).then(response => { this.form = response.data; this.open = true; this.title = "修改教室" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.classroomId != null) { updateClassroom(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addClassroom(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const classroomIds = row.classroomId || this.ids; this.$modal.confirm('是否确认删除教室编号为"' + classroomIds + '"的数据项？').then(function() { return delClassroom(classroomIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('brm/classroom/export', { ...this.queryParams }, `classroom_${new Date().getTime()}.xlsx`) }
  }
}
</script>
