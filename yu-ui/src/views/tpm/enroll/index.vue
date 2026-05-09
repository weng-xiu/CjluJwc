<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学生ID" prop="studentId"><el-input v-model="queryParams.studentId" placeholder="请输入学生ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="开课ID" prop="courseOfferingId"><el-input v-model="queryParams.courseOfferingId" placeholder="请输入开课ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['tpm:enroll:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['tpm:enroll:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['tpm:enroll:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['tpm:enroll:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="enrollList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="轮次ID" align="center" prop="roundId" />
      <el-table-column label="学生ID" align="center" prop="studentId" />
      <el-table-column label="开课ID" align="center" prop="courseOfferingId" />
      <el-table-column label="选课时间" align="center" prop="selectTime" width="180"><template slot-scope="scope"><span>{{ parseTime(scope.row.selectTime, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="抽签结果" align="center" prop="lotteryResult"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.lotteryResult"/></template></el-table-column>
      <el-table-column label="结果状态" align="center" prop="resultStatus"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.resultStatus"/></template></el-table-column>
      <el-table-column label="退课时间" align="center" prop="dropTime" width="180"><template slot-scope="scope"><span>{{ parseTime(scope.row.dropTime, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['tpm:enroll:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['tpm:enroll:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="550px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="轮次ID" prop="roundId"><el-input v-model="form.roundId" placeholder="请输入轮次ID" /></el-form-item>
        <el-form-item label="学生ID" prop="studentId"><el-input v-model="form.studentId" placeholder="请输入学生ID" /></el-form-item>
        <el-form-item label="开课ID" prop="courseOfferingId"><el-input v-model="form.courseOfferingId" placeholder="请输入开课ID" /></el-form-item>
        <el-form-item label="选课时间" prop="selectTime"><el-date-picker clearable v-model="form.selectTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择选课时间" /></el-form-item>
        <el-form-item label="抽签结果" prop="lotteryResult"><el-input v-model="form.lotteryResult" placeholder="请输入抽签结果" /></el-form-item>
        <el-form-item label="结果状态" prop="resultStatus"><el-input v-model="form.resultStatus" placeholder="请输入结果状态" /></el-form-item>
        <el-form-item label="退课时间" prop="dropTime"><el-date-picker clearable v-model="form.dropTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择退课时间" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listEnroll, getEnroll, delEnroll, addEnroll, updateEnroll } from "@/api/tpm/enroll"
export default {
  name: "Enroll", dicts: ['sys_normal_disable'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, enrollList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, studentId: null, courseOfferingId: null, status: null },
    form: {}, rules: { roundId: [{ required: true, message: "轮次ID不能为空", trigger: "blur" }], studentId: [{ required: true, message: "学生ID不能为空", trigger: "blur" }], courseOfferingId: [{ required: true, message: "开课ID不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listEnroll(this.queryParams).then(response => { this.enrollList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { enrollId: null, roundId: null, studentId: null, courseOfferingId: null, selectTime: null, lotteryResult: null, resultStatus: null, dropTime: null, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.enrollId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加选课名单" },
    handleUpdate(row) { this.reset(); const enrollId = row.enrollId || this.ids; getEnroll(enrollId).then(response => { this.form = response.data; this.open = true; this.title = "修改选课名单" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.enrollId != null) { updateEnroll(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addEnroll(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const enrollIds = row.enrollId || this.ids; this.$modal.confirm('是否确认删除选课名单编号为"' + enrollIds + '"的数据项？').then(function() { return delEnroll(enrollIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('tpm/enroll/export', { ...this.queryParams }, `enroll_${new Date().getTime()}.xlsx`) }
  }
}
</script>
