<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="开课ID" prop="offeringId"><el-input v-model="queryParams.offeringId" placeholder="请输入开课ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="教室ID" prop="classroomId"><el-input v-model="queryParams.classroomId" placeholder="请输入教室ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['tpm:schedule:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['tpm:schedule:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['tpm:schedule:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['tpm:schedule:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="scheduleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="开课ID" align="center" prop="offeringId" />
      <el-table-column label="教室ID" align="center" prop="classroomId" />
      <el-table-column label="星期几" align="center" prop="weekDay" />
      <el-table-column label="开始节次" align="center" prop="startPeriod" />
      <el-table-column label="结束节次" align="center" prop="endPeriod" />
      <el-table-column label="起始周" align="center" prop="startWeek" />
      <el-table-column label="结束周" align="center" prop="endWeek" />
      <el-table-column label="排课方式" align="center" prop="scheduleType" />
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['tpm:schedule:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['tpm:schedule:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="550px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="开课ID" prop="offeringId"><el-input v-model="form.offeringId" placeholder="请输入开课ID" /></el-form-item>
        <el-form-item label="教室ID" prop="classroomId"><el-input v-model="form.classroomId" placeholder="请输入教室ID" /></el-form-item>
        <el-form-item label="星期几" prop="weekDay"><el-input v-model="form.weekDay" placeholder="请输入星期几" /></el-form-item>
        <el-form-item label="开始节次" prop="startPeriod"><el-input v-model="form.startPeriod" placeholder="请输入开始节次" /></el-form-item>
        <el-form-item label="结束节次" prop="endPeriod"><el-input v-model="form.endPeriod" placeholder="请输入结束节次" /></el-form-item>
        <el-form-item label="起始周" prop="startWeek"><el-input v-model="form.startWeek" placeholder="请输入起始周" /></el-form-item>
        <el-form-item label="结束周" prop="endWeek"><el-input v-model="form.endWeek" placeholder="请输入结束周" /></el-form-item>
        <el-form-item label="排课方式" prop="scheduleType"><el-input v-model="form.scheduleType" placeholder="请输入排课方式" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listSchedule, getSchedule, delSchedule, addSchedule, updateSchedule } from "@/api/tpm/schedule"
export default {
  name: "Schedule", dicts: ['sys_normal_disable'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, scheduleList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, offeringId: null, classroomId: null, status: null },
    form: {}, rules: { offeringId: [{ required: true, message: "开课ID不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listSchedule(this.queryParams).then(response => { this.scheduleList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { scheduleId: null, offeringId: null, classroomId: null, weekDay: null, startPeriod: null, endPeriod: null, startWeek: null, endWeek: null, scheduleType: null, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.scheduleId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加排课" },
    handleUpdate(row) { this.reset(); const scheduleId = row.scheduleId || this.ids; getSchedule(scheduleId).then(response => { this.form = response.data; this.open = true; this.title = "修改排课" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.scheduleId != null) { updateSchedule(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addSchedule(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const scheduleIds = row.scheduleId || this.ids; this.$modal.confirm('是否确认删除排课编号为"' + scheduleIds + '"的数据项？').then(function() { return delSchedule(scheduleIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('tpm/schedule/export', { ...this.queryParams }, `schedule_${new Date().getTime()}.xlsx`) }
  }
}
</script>
