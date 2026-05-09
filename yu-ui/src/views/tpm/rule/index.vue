<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="规则名称" prop="ruleName"><el-input v-model="queryParams.ruleName" placeholder="请输入规则名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="规则类型" prop="ruleType"><el-input v-model="queryParams.ruleType" placeholder="请输入规则类型" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['tpm:rule:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['tpm:rule:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['tpm:rule:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['tpm:rule:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="ruleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="轮次ID" align="center" prop="roundId" />
      <el-table-column label="规则名称" align="center" prop="ruleName" />
      <el-table-column label="规则类型" align="center" prop="ruleType" />
      <el-table-column label="限制目标" align="center" prop="restrictTarget" />
      <el-table-column label="限制值" align="center" prop="restrictValue" />
      <el-table-column label="优先级" align="center" prop="priority" />
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['tpm:rule:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['tpm:rule:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="550px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="轮次ID" prop="roundId"><el-input v-model="form.roundId" placeholder="请输入轮次ID" /></el-form-item>
        <el-form-item label="规则名称" prop="ruleName"><el-input v-model="form.ruleName" placeholder="请输入规则名称" /></el-form-item>
        <el-form-item label="规则类型" prop="ruleType"><el-input v-model="form.ruleType" placeholder="请输入规则类型" /></el-form-item>
        <el-form-item label="限制目标" prop="restrictTarget"><el-input v-model="form.restrictTarget" placeholder="请输入限制目标" /></el-form-item>
        <el-form-item label="限制值" prop="restrictValue"><el-input v-model="form.restrictValue" placeholder="请输入限制值" /></el-form-item>
        <el-form-item label="优先级" prop="priority"><el-input v-model="form.priority" placeholder="请输入优先级" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listRule, getRule, delRule, addRule, updateRule } from "@/api/tpm/rule"
export default {
  name: "Rule", dicts: ['sys_normal_disable'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, ruleList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, ruleName: null, ruleType: null, status: null },
    form: {}, rules: { ruleName: [{ required: true, message: "规则名称不能为空", trigger: "blur" }], ruleType: [{ required: true, message: "规则类型不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listRule(this.queryParams).then(response => { this.ruleList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { ruleId: null, roundId: null, ruleName: null, ruleType: null, restrictTarget: null, restrictValue: null, priority: null, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.ruleId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加选课规则" },
    handleUpdate(row) { this.reset(); const ruleId = row.ruleId || this.ids; getRule(ruleId).then(response => { this.form = response.data; this.open = true; this.title = "修改选课规则" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.ruleId != null) { updateRule(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addRule(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const ruleIds = row.ruleId || this.ids; this.$modal.confirm('是否确认删除选课规则编号为"' + ruleIds + '"的数据项？').then(function() { return delRule(ruleIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('tpm/rule/export', { ...this.queryParams }, `rule_${new Date().getTime()}.xlsx`) }
  }
}
</script>
