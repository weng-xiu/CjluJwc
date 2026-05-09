<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="任职岗位" prop="positionTitle"><el-input v-model="queryParams.positionTitle" placeholder="请输入任职岗位" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="是否现任" prop="isCurrent"><el-select v-model="queryParams.isCurrent" placeholder="请选择是否现任" clearable><el-option label="是" value="0"/><el-option label="否" value="1"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['brm:position:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['brm:position:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['brm:position:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['brm:position:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="positionList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="教师ID" align="center" prop="teacherId" />
      <el-table-column label="任职院系" align="center" prop="deptId" />
      <el-table-column label="任职岗位" align="center" prop="positionTitle" />
      <el-table-column label="开始日期" align="center" prop="startDate" width="180"><template slot-scope="scope"><span>{{ parseTime(scope.row.startDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="结束日期" align="center" prop="endDate" width="180"><template slot-scope="scope"><span>{{ parseTime(scope.row.endDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="是否现任" align="center" prop="isCurrent"><template slot-scope="scope"><dict-tag :options="dict.type.sys_yes_no" :value="scope.row.isCurrent"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['brm:position:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['brm:position:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="教师ID" prop="teacherId"><el-input v-model="form.teacherId" placeholder="请输入教师ID" /></el-form-item>
        <el-form-item label="任职院系" prop="deptId"><el-input v-model="form.deptId" placeholder="请输入任职院系ID" /></el-form-item>
        <el-form-item label="任职岗位" prop="positionTitle"><el-input v-model="form.positionTitle" placeholder="请输入任职岗位" /></el-form-item>
        <el-form-item label="开始日期" prop="startDate"><el-date-picker clearable v-model="form.startDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择开始日期" /></el-form-item>
        <el-form-item label="结束日期" prop="endDate"><el-date-picker clearable v-model="form.endDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择结束日期" /></el-form-item>
        <el-form-item label="是否现任"><el-radio-group v-model="form.isCurrent"><el-radio v-for="dict in dict.type.sys_yes_no" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listPosition, getPosition, delPosition, addPosition, updatePosition } from "@/api/brm/position"
export default {
  name: "Position", dicts: ['sys_yes_no'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, positionList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, positionTitle: null, isCurrent: null },
    form: {}, rules: { teacherId: [{ required: true, message: "教师ID不能为空", trigger: "blur" }], positionTitle: [{ required: true, message: "任职岗位不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listPosition(this.queryParams).then(response => { this.positionList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { posId: null, teacherId: null, deptId: null, positionTitle: null, startDate: null, endDate: null, isCurrent: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.posId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加教师任职" },
    handleUpdate(row) { this.reset(); const posId = row.posId || this.ids; getPosition(posId).then(response => { this.form = response.data; this.open = true; this.title = "修改教师任职" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.posId != null) { updatePosition(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addPosition(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const posIds = row.posId || this.ids; this.$modal.confirm('是否确认删除任职编号为"' + posIds + '"的数据项？').then(function() { return delPosition(posIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('brm/position/export', { ...this.queryParams }, `position_${new Date().getTime()}.xlsx`) }
  }
}
</script>
