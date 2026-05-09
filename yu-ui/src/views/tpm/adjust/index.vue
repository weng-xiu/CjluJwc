<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="调整类型" prop="adjustType"><el-input v-model="queryParams.adjustType" placeholder="请输入调整类型" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="审批状态" prop="approveStatus"><el-select v-model="queryParams.approveStatus" placeholder="请选择审批状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['tpm:adjust:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['tpm:adjust:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['tpm:adjust:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['tpm:adjust:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="adjustList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="排课ID" align="center" prop="scheduleId" />
      <el-table-column label="调整类型" align="center" prop="adjustType" />
      <el-table-column label="原日期" align="center" prop="originalDate" width="180"><template slot-scope="scope"><span>{{ parseTime(scope.row.originalDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="新日期" align="center" prop="newDate" width="180"><template slot-scope="scope"><span>{{ parseTime(scope.row.newDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="申请原因" align="center" prop="reason" width="200" />
      <el-table-column label="申请⼈" align="center" prop="applicant" />
      <el-table-column label="审批状态" align="center" prop="approveStatus"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.approveStatus"/></template></el-table-column>
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['tpm:adjust:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['tpm:adjust:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="排课ID" prop="scheduleId"><el-input v-model="form.scheduleId" placeholder="请输入排课ID" /></el-form-item>
        <el-form-item label="调整类型" prop="adjustType"><el-input v-model="form.adjustType" placeholder="请输入调整类型" /></el-form-item>
        <el-form-item label="原日期" prop="originalDate"><el-date-picker clearable v-model="form.originalDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择原日期" /></el-form-item>
        <el-form-item label="新日期" prop="newDate"><el-date-picker clearable v-model="form.newDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择新日期" /></el-form-item>
        <el-form-item label="新教室ID" prop="newClassroomId"><el-input v-model="form.newClassroomId" placeholder="请输入新教室ID" /></el-form-item>
        <el-form-item label="新星期几" prop="newWeekDay"><el-input v-model="form.newWeekDay" placeholder="请输入新星期几" /></el-form-item>
        <el-form-item label="新开始节次" prop="newStartPeriod"><el-input v-model="form.newStartPeriod" placeholder="请输入新开始节次" /></el-form-item>
        <el-form-item label="新结束节次" prop="newEndPeriod"><el-input v-model="form.newEndPeriod" placeholder="请输入新结束节次" /></el-form-item>
        <el-form-item label="申请原因" prop="reason"><el-input v-model="form.reason" type="textarea" placeholder="请输入申请原因" /></el-form-item>
        <el-form-item label="申请人" prop="applicant"><el-input v-model="form.applicant" placeholder="请输入申请人" /></el-form-item>
        <el-form-item label="审批状态" prop="approveStatus"><el-input v-model="form.approveStatus" placeholder="请输入审批状态" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listAdjust, getAdjust, delAdjust, addAdjust, updateAdjust } from "@/api/tpm/adjust"
export default {
  name: "Adjust", dicts: ['sys_normal_disable'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, adjustList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, adjustType: null, approveStatus: null },
    form: {}, rules: { adjustType: [{ required: true, message: "调整类型不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listAdjust(this.queryParams).then(response => { this.adjustList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { adjustId: null, scheduleId: null, adjustType: null, originalDate: null, newDate: null, newClassroomId: null, newWeekDay: null, newStartPeriod: null, newEndPeriod: null, reason: null, applicant: null, approveStatus: null, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.adjustId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加调停课申请" },
    handleUpdate(row) { this.reset(); const adjustId = row.adjustId || this.ids; getAdjust(adjustId).then(response => { this.form = response.data; this.open = true; this.title = "修改调停课申请" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.adjustId != null) { updateAdjust(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addAdjust(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const adjustIds = row.adjustId || this.ids; this.$modal.confirm('是否确认删除调停课申请编号为"' + adjustIds + '"的数据项？').then(function() { return delAdjust(adjustIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('tpm/adjust/export', { ...this.queryParams }, `adjust_${new Date().getTime()}.xlsx`) }
  }
}
</script>
