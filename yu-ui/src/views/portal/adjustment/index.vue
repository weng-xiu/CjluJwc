<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="课程名称" prop="courseName"><el-input v-model="queryParams.courseName" placeholder="请输入课程名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="审批状态" prop="approveStatus"><el-select v-model="queryParams.approveStatus" placeholder="请选择" clearable><el-option label="待审批" value="0"/><el-option label="已通过" value="1"/><el-option label="已驳回" value="2"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['portal:adjustment:add']">提交申请</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="adjustmentList">
      <el-table-column label="课程名称" align="center" prop="courseName" />
      <el-table-column label="原上课时间" align="center" prop="originTime" />
      <el-table-column label="调整后时间" align="center" prop="newTime" />
      <el-table-column label="调整类型" align="center" prop="adjustType"><template slot-scope="scope"><dict-tag :options="dict.type.portal_adjust_type" :value="scope.row.adjustType"/></template></el-table-column>
      <el-table-column label="申请日期" align="center" prop="applyDate" width="120"><template slot-scope="scope"><span>{{ parseTime(scope.row.applyDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="审批状态" align="center" prop="approveStatus"><template slot-scope="scope"><dict-tag :options="dict.type.portal_approve_status" :value="scope.row.approveStatus"/></template></el-table-column>
      <el-table-column label="原因" align="center" prop="reason" show-overflow-tooltip />
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="550px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="课程" prop="courseName"><el-input v-model="form.courseName" placeholder="请选择课程" /></el-form-item>
        <el-form-item label="原上课时间" prop="originTime"><el-input v-model="form.originTime" placeholder="请输入原上课时间" /></el-form-item>
        <el-form-item label="调整后时间" prop="newTime"><el-input v-model="form.newTime" placeholder="请输入调整后时间" /></el-form-item>
        <el-form-item label="调整类型" prop="adjustType"><el-select v-model="form.adjustType" placeholder="请选择"><el-option label="调课" value="1"/><el-option label="停课" value="2"/><el-option label="补课" value="3"/></el-select></el-form-item>
        <el-form-item label="申请原因" prop="reason"><el-input v-model="form.reason" type="textarea" placeholder="请输入申请原因" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">提 交</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listAdjustment, addAdjustment } from "@/api/portal/adjustment"
export default {
  name: "PortalAdjustment",
  data() { return { loading: true, showSearch: true, total: 0, adjustmentList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, courseName: null, approveStatus: null },
    form: {}, rules: { courseName: [{ required: true, message: "课程不能为空", trigger: "blur" }], originTime: [{ required: true, message: "原上课时间不能为空", trigger: "blur" }], newTime: [{ required: true, message: "调整后时间不能为空", trigger: "blur" }], reason: [{ required: true, message: "申请原因不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listAdjustment(this.queryParams).then(response => { this.adjustmentList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { courseName: null, originTime: null, newTime: null, adjustType: "1", reason: null }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleAdd() { this.reset(); this.open = true; this.title = "调停课申请" },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { addAdjustment(this.form).then(response => { this.$modal.msgSuccess("申请提交成功"); this.open = false; this.getList() }) } }) }
  }
}
</script>
