<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学年名称" prop="yearName"><el-input v-model="queryParams.yearName" placeholder="请输入学年名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['brm:year:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['brm:year:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['brm:year:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['brm:year:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="yearList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学年名称" align="center" prop="yearName" />
      <el-table-column label="开始日期" align="center" prop="startDate" width="180"><template slot-scope="scope"><span>{{ parseTime(scope.row.startDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="结束日期" align="center" prop="endDate" width="180"><template slot-scope="scope"><span>{{ parseTime(scope.row.endDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['brm:year:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['brm:year:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="学年名称" prop="yearName"><el-input v-model="form.yearName" placeholder="请输入学年名称" /></el-form-item>
        <el-form-item label="开始日期" prop="startDate"><el-date-picker clearable v-model="form.startDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择开始日期" /></el-form-item>
        <el-form-item label="结束日期" prop="endDate"><el-date-picker clearable v-model="form.endDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择结束日期" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listYear, getYear, delYear, addYear, updateYear } from "@/api/brm/year"
export default {
  name: "Year", dicts: ['sys_normal_disable'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, yearList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, yearName: null, status: null },
    form: {}, rules: { yearName: [{ required: true, message: "学年名称不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listYear(this.queryParams).then(response => { this.yearList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { yearId: null, yearName: null, startDate: null, endDate: null, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.yearId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加学年" },
    handleUpdate(row) { this.reset(); const yearId = row.yearId || this.ids; getYear(yearId).then(response => { this.form = response.data; this.open = true; this.title = "修改学年" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.yearId != null) { updateYear(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addYear(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const yearIds = row.yearId || this.ids; this.$modal.confirm('是否确认删除学年编号为"' + yearIds + '"的数据项？').then(function() { return delYear(yearIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('brm/year/export', { ...this.queryParams }, `year_${new Date().getTime()}.xlsx`) }
  }
}
</script>
