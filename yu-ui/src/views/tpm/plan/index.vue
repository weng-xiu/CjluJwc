<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="方案名称" prop="planName"><el-input v-model="queryParams.planName" placeholder="请输入方案名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['tpm:plan:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['tpm:plan:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['tpm:plan:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['tpm:plan:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="planList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="方案名称" align="center" prop="planName" />
      <el-table-column label="专业ID" align="center" prop="majorId" />
      <el-table-column label="院系ID" align="center" prop="deptId" />
      <el-table-column label="学历层次" align="center" prop="educationLevel" />
      <el-table-column label="方案年份" align="center" prop="planYear" />
      <el-table-column label="总学分" align="center" prop="totalCredits" />
      <el-table-column label="发布状态" align="center" prop="publishStatus"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.publishStatus"/></template></el-table-column>
      <el-table-column label="发布日期" align="center" prop="publishDate" width="180"><template slot-scope="scope"><span>{{ parseTime(scope.row.publishDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['tpm:plan:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['tpm:plan:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="方案名称" prop="planName"><el-input v-model="form.planName" placeholder="请输入方案名称" /></el-form-item>
        <el-form-item label="专业ID" prop="majorId"><el-input v-model="form.majorId" placeholder="请输入专业ID" /></el-form-item>
        <el-form-item label="院系ID" prop="deptId"><el-input v-model="form.deptId" placeholder="请输入院系ID" /></el-form-item>
        <el-form-item label="学历层次" prop="educationLevel"><el-input v-model="form.educationLevel" placeholder="请输入学历层次" /></el-form-item>
        <el-form-item label="方案年份" prop="planYear"><el-input v-model="form.planYear" placeholder="请输入方案年份" /></el-form-item>
        <el-form-item label="总学分" prop="totalCredits"><el-input v-model="form.totalCredits" placeholder="请输入总学分" /></el-form-item>
        <el-form-item label="版本号" prop="version"><el-input v-model="form.version" placeholder="请输入版本号" /></el-form-item>
        <el-form-item label="发布日期" prop="publishDate"><el-date-picker clearable v-model="form.publishDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择发布日期" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listPlan, getPlan, delPlan, addPlan, updatePlan } from "@/api/tpm/plan"
export default {
  name: "Plan", dicts: ['sys_normal_disable'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, planList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, planName: null, status: null },
    form: {}, rules: { planName: [{ required: true, message: "方案名称不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listPlan(this.queryParams).then(response => { this.planList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { planId: null, planName: null, majorId: null, deptId: null, educationLevel: null, planYear: null, totalCredits: null, version: null, publishDate: null, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.planId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加培养方案" },
    handleUpdate(row) { this.reset(); const planId = row.planId || this.ids; getPlan(planId).then(response => { this.form = response.data; this.open = true; this.title = "修改培养方案" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.planId != null) { updatePlan(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addPlan(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const planIds = row.planId || this.ids; this.$modal.confirm('是否确认删除培养方案编号为"' + planIds + '"的数据项？').then(function() { return delPlan(planIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('tpm/plan/export', { ...this.queryParams }, `plan_${new Date().getTime()}.xlsx`) }
  }
}
</script>
