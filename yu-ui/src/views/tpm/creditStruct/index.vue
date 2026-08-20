<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="所属方案" prop="planId">
        <el-select v-model="queryParams.planId" placeholder="请选择培养方案" clearable filterable>
          <el-option v-for="p in planOptions" :key="p.planId" :label="p.planName" :value="p.planId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="学分类型" prop="creditType">
        <el-select v-model="queryParams.creditType" placeholder="请选择学分类型" clearable>
          <el-option v-for="dict in dict.type.tpm_credit_type" :key="dict.value" :label="dict.label" :value="dict.value"/>
        </el-select>
      </el-form-item>
      <el-form-item label="类型名称" prop="creditTypeName"><el-input v-model="queryParams.creditTypeName" placeholder="请输入学分类型名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['tpm:credit:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['tpm:credit:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['tpm:credit:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['tpm:credit:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="creditStructList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="所属方案" align="center" prop="planName" :show-overflow-tooltip="true" />
      <el-table-column label="学分类型" align="center" prop="creditType" width="120"><template slot-scope="scope"><dict-tag :options="dict.type.tpm_credit_type" :value="scope.row.creditType"/></template></el-table-column>
      <el-table-column label="类型名称" align="center" prop="creditTypeName" />
      <el-table-column label="要求学分" align="center" prop="requiredCredit" width="90" />
      <el-table-column label="最低学分" align="center" prop="minCredit" width="90" />
      <el-table-column label="说明" align="center" prop="description" width="220" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="status" width="80"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['tpm:credit:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['tpm:credit:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属方案" prop="planId">
          <el-select v-model="form.planId" placeholder="请选择培养方案" filterable clearable style="width:100%">
            <el-option v-for="p in planOptions" :key="p.planId" :label="p.planName" :value="p.planId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="学分类型" prop="creditType">
          <el-select v-model="form.creditType" placeholder="请选择学分类型" clearable filterable style="width:100%" @change="handleCreditTypeChange">
            <el-option v-for="dict in dict.type.tpm_credit_type" :key="dict.value" :label="dict.label" :value="dict.value"/>
          </el-select>
        </el-form-item>
        <el-form-item label="类型名称" prop="creditTypeName"><el-input v-model="form.creditTypeName" placeholder="请输入学分类型名称" /></el-form-item>
        <el-form-item label="要求学分" prop="requiredCredit"><el-input-number v-model="form.requiredCredit" :min="0" :precision="1" :step="1" controls-position="right" style="width:100%"/></el-form-item>
        <el-form-item label="最低学分" prop="minCredit"><el-input-number v-model="form.minCredit" :min="0" :precision="1" :step="1" controls-position="right" style="width:100%"/></el-form-item>
        <el-form-item label="说明" prop="description"><el-input v-model="form.description" type="textarea" placeholder="请输入说明" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listCreditStruct, getCreditStruct, delCreditStruct, addCreditStruct, updateCreditStruct } from "@/api/tpm/creditStruct"
import { listPlan } from "@/api/tpm/plan"

export default {
  name: "CreditStruct",
  dicts: ['sys_normal_disable', 'tpm_credit_type'],
  data() {
    return {
      loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, creditStructList: [], title: "", open: false,
      planOptions: [],
      queryParams: { pageNum: 1, pageSize: 10, planId: null, creditType: null, creditTypeName: null, status: null },
      form: {},
      rules: {
        planId: [{ required: true, message: "所属方案不能为空", trigger: "change" }],
        creditType: [{ required: true, message: "学分类型不能为空", trigger: "change" }],
        creditTypeName: [{ required: true, message: "学分类型名称不能为空", trigger: "blur" }]
      }
    }
  },
  created() {
    this.getList()
    this.loadPlans()
  },
  methods: {
    getList() {
      this.loading = true
      listCreditStruct(this.queryParams).then(response => { this.creditStructList = response.rows; this.total = response.total; this.loading = false })
    },
    loadPlans() {
      listPlan({ pageNum: 1, pageSize: 1000 }).then(response => { this.planOptions = response.rows || [] })
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { structId: null, planId: null, creditType: null, creditTypeName: null, requiredCredit: null, minCredit: null, description: null, status: "0" }
      this.resetForm("form")
    },
    handleCreditTypeChange(val) {
      const opt = (this.dict.type.tpm_credit_type || []).find(d => d.value === val)
      if (opt) { this.form.creditTypeName = opt.label }
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.structId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加学分结构" },
    handleUpdate(row) { this.reset(); const structId = row.structId || this.ids; getCreditStruct(structId).then(response => { this.form = response.data; this.open = true; this.title = "修改学分结构" }) },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.structId != null) {
            updateCreditStruct(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() })
          } else {
            addCreditStruct(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() })
          }
        }
      })
    },
    handleDelete(row) {
      const structIds = row.structId || this.ids
      this.$modal.confirm('是否确认删除学分结构编号为"' + structIds + '"的数据项？').then(function() { return delCreditStruct(structIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    },
    handleExport() { this.download('tpm/creditStruct/export', { ...this.queryParams }, `creditStruct_${new Date().getTime()}.xlsx`) }
  }
}
</script>
