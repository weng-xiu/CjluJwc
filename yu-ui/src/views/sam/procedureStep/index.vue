<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="环节名称" prop="stepName"><el-input v-model="queryParams.stepName" placeholder="请输入环节名称" clearable style="width:180px" @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width:120px">
          <el-option label="启用" value="0"/><el-option label="停用" value="1"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['sam:procedureStep:add']">新增环节</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:procedureStep:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['sam:procedureStep:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="环节编码" align="center" prop="stepKey" width="150"/>
      <el-table-column label="环节名称" align="center" prop="stepName" :show-overflow-tooltip="true"/>
      <el-table-column label="排序" align="center" prop="orderNum" width="70"/>
      <el-table-column label="是否必办" align="center" prop="requiredFlag" width="90">
        <template slot-scope="scope"><el-tag :type="scope.row.requiredFlag==='1'?'danger':'info'" size="small">{{ scope.row.requiredFlag==='1'?'必办':'可选' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="自动判定数据源" align="center" prop="autoCheckType" width="140">
        <template slot-scope="scope"><dict-tag :options="dict.type.sam_step_auto_check" :value="scope.row.autoCheckType"/></template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template slot-scope="scope"><el-tag :type="scope.row.status==='0'?'success':'info'" size="small">{{ scope.row.status==='0'?'启用':'停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['sam:procedureStep:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['sam:procedureStep:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>

    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="环节编码" prop="stepKey"><el-input v-model="form.stepKey" placeholder="如 LIBRARY / 自定义大写字母" :disabled="form.stepId!=null"/></el-form-item>
        <el-form-item label="环节名称" prop="stepName"><el-input v-model="form.stepName" placeholder="请输入环节名称"/></el-form-item>
        <el-form-item label="排序" prop="orderNum"><el-input-number v-model="form.orderNum" :min="0" controls-position="right"/></el-form-item>
        <el-form-item label="是否必办" prop="requiredFlag"><el-radio-group v-model="form.requiredFlag"><el-radio label="1">必办</el-radio><el-radio label="0">可选</el-radio></el-radio-group></el-form-item>
        <el-form-item label="自动判定数据源" prop="autoCheckType">
          <el-select v-model="form.autoCheckType" placeholder="请选择" style="width:100%">
            <el-option v-for="dict in dict.type.sam_step_auto_check" :key="dict.value" :label="dict.label" :value="dict.value"/>
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status"><el-radio-group v-model="form.status"><el-radio label="0">启用</el-radio><el-radio label="1">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" placeholder="请输入备注"/></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import { listProcedureStep, getProcedureStep, addProcedureStep, updateProcedureStep, delProcedureStep } from "@/api/sam/procedureStep"

export default {
  name: "ProcedureStep",
  dicts: ['sam_step_auto_check'],
  data() {
    return {
      loading: true, ids: [], multiple: true, showSearch: true, total: 0, list: [], title: "", open: false,
      queryParams: { pageNum: 1, pageSize: 10, stepName: null, status: null },
      form: {},
      rules: {
        stepKey: [{ required: true, message: "环节编码不能为空", trigger: "blur" }],
        stepName: [{ required: true, message: "环节名称不能为空", trigger: "blur" }]
      }
    }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listProcedureStep(this.queryParams).then(res => { this.list = res.rows; this.total = res.total; this.loading = false }).catch(() => { this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { stepId: null, stepKey: null, stepName: null, orderNum: 0, requiredFlag: "1", autoCheckType: "NONE", status: "0", remark: null }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(i => i.stepId); this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加离校环节" },
    handleUpdate(row) { this.reset(); getProcedureStep(row.stepId).then(res => { this.form = res.data; this.open = true; this.title = "修改离校环节" }) },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) return
        if (this.form.stepId != null) { updateProcedureStep(this.form).then(() => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) }
        else { addProcedureStep(this.form).then(() => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) }
      })
    },
    handleDelete(row) {
      const stepIds = row.stepId ? [row.stepId] : this.ids
      this.$modal.confirm('是否确认删除选中的环节？已有办理明细的环节建议改为停用。').then(() => delProcedureStep(stepIds)).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    },
    handleExport() { this.download('sam/procedureStep/export', { ...this.queryParams }, `procedureStep_${new Date().getTime()}.xlsx`) }
  }
}
</script>
