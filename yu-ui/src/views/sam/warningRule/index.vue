<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="规则代码" prop="ruleCode"><el-input v-model="queryParams.ruleCode" placeholder="请输入规则代码" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="规则名称" prop="ruleName"><el-input v-model="queryParams.ruleName" placeholder="请输入规则名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="预警类型" prop="warningType"><el-select v-model="queryParams.warningType" placeholder="请选择" clearable><el-option label="成绩预警" value="0"/><el-option label="学分预警" value="1"/><el-option label="出勤预警" value="2"/><el-option label="综合预警" value="3"/></el-select></el-form-item>
      <el-form-item label="预警级别" prop="warningLevel"><el-select v-model="queryParams.warningLevel" placeholder="请选择" clearable><el-option label="一般" value="0"/><el-option label="严重" value="1"/><el-option label="高危" value="2"/></el-select></el-form-item>
      <el-form-item label="是否启用" prop="isEnabled"><el-select v-model="queryParams.isEnabled" placeholder="请选择" clearable><el-option label="否" value="0"/><el-option label="是" value="1"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['sam:warningRule:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['sam:warningRule:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:warningRule:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['sam:warningRule:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="ruleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="规则代码" align="center" prop="ruleCode" />
      <el-table-column label="规则名称" align="center" prop="ruleName" :show-overflow-tooltip="true" />
      <el-table-column label="预警类型" align="center" prop="warningType"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'成绩'},{dictValue:'1',dictLabel:'学分'},{dictValue:'2',dictLabel:'出勤'},{dictValue:'3',dictLabel:'综合'}]" :value="scope.row.warningType"/></template></el-table-column>
      <el-table-column label="阈值" align="center" prop="thresholdValue" />
      <el-table-column label="预警级别" align="center" prop="warningLevel"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'一般'},{dictValue:'1',dictLabel:'严重'},{dictValue:'2',dictLabel:'高危'}]" :value="scope.row.warningLevel"/></template></el-table-column>
      <el-table-column label="是否启用" align="center" prop="isEnabled"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'否'},{dictValue:'1',dictLabel:'是'}]" :value="scope.row.isEnabled"/></template></el-table-column>
      <el-table-column label="适用学期" align="center" prop="semesterId"><template slot-scope="scope"><span>{{ scope.row.semesterId ? scope.row.semesterId : '所有学期' }}</span></template></el-table-column>
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'正常'},{dictValue:'1',dictLabel:'停用'}]" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['sam:warningRule:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['sam:warningRule:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="规则代码" prop="ruleCode"><el-input v-model="form.ruleCode" placeholder="请输入规则代码（如GPA_LOW）" /></el-form-item>
        <el-form-item label="规则名称" prop="ruleName"><el-input v-model="form.ruleName" placeholder="请输入规则名称" /></el-form-item>
        <el-form-item label="预警类型" prop="warningType"><el-select v-model="form.warningType" placeholder="请选择" style="width:100%"><el-option label="成绩预警" value="0"/><el-option label="学分预警" value="1"/><el-option label="出勤预警" value="2"/><el-option label="综合预警" value="3"/></el-select></el-form-item>
        <el-form-item label="阈值" prop="thresholdValue"><el-input v-model="form.thresholdValue" placeholder="请输入阈值" /></el-form-item>
        <el-form-item label="预警级别" prop="warningLevel"><el-select v-model="form.warningLevel" placeholder="请选择" style="width:100%"><el-option label="一般" value="0"/><el-option label="严重" value="1"/><el-option label="高危" value="2"/></el-select></el-form-item>
        <el-form-item label="消息模板" prop="messageTemplate"><el-input v-model="form.messageTemplate" type="textarea" placeholder="请输入消息模板" /></el-form-item>
        <el-form-item label="是否启用"><el-radio-group v-model="form.isEnabled"><el-radio label="0">否</el-radio><el-radio label="1">是</el-radio></el-radio-group></el-form-item>
        <el-form-item label="学年">
          <el-select v-model="formYearId" placeholder="请选择学年" clearable @change="handleFormYearChange" style="width:100%">
            <el-option v-for="y in yearList" :key="y.yearId" :label="y.yearName" :value="y.yearId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="适用学期" prop="semesterId">
          <el-select v-model="form.semesterId" placeholder="留空表示所有学期" clearable :disabled="!formYearId" style="width:100%">
            <el-option v-for="s in formSemesterList" :key="s.semesterId" :label="s.semesterName" :value="s.semesterId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio label="0">正常</el-radio><el-radio label="1">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" placeholder="请输入备注" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listWarningRule, getWarningRule, delWarningRule, addWarningRule, updateWarningRule } from "@/api/sam/warningRule"
import { listYear } from "@/api/brm/year"
import { listSemester, getSemester } from "@/api/brm/semester"
export default {
  name: "WarningRule",
  data() { return {
    loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, ruleList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, ruleCode: null, ruleName: null, warningType: null, warningLevel: null, isEnabled: null },
    form: {}, rules: {
      ruleCode: [{ required: true, message: "规则代码不能为空", trigger: "blur" }],
      ruleName: [{ required: true, message: "规则名称不能为空", trigger: "blur" }]
    },
    yearList: [], formYearId: null, formSemesterList: []
  }},
  created() { this.loadYears(); this.getList() },
  methods: {
    loadYears() {
      listYear({ pageNum: 1, pageSize: 100 }).then(r => { this.yearList = r.rows })
    },
    handleFormYearChange(yearId) {
      this.formSemesterList = []
      this.form.semesterId = null
      if (yearId) {
        listSemester({ academicYearId: yearId, pageNum: 1, pageSize: 50 }).then(r => { this.formSemesterList = r.rows })
      }
    },
    getList() { this.loading = true; listWarningRule(this.queryParams).then(response => { this.ruleList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { ruleId: null, ruleCode: null, ruleName: null, warningType: null, thresholdValue: null, warningLevel: null, messageTemplate: null, isEnabled: "1", semesterId: null, status: "0", remark: null }
      this.formYearId = null; this.formSemesterList = []
      this.resetForm("form")
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.ruleId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加预警规则" },
    handleUpdate(row) {
      this.reset()
      const ruleId = row.ruleId || this.ids
      getWarningRule(ruleId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改预警规则"
        if (this.form.semesterId) {
          getSemester(this.form.semesterId).then(res => {
            if (res.data && res.data.academicYearId) {
              this.formYearId = res.data.academicYearId
              listSemester({ academicYearId: this.formYearId, pageNum: 1, pageSize: 50 }).then(r => { this.formSemesterList = r.rows })
            }
          })
        }
      })
    },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.ruleId != null) { updateWarningRule(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addWarningRule(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const ruleIds = row.ruleId || this.ids; this.$modal.confirm('是否确认删除预警规则编号为"' + ruleIds + '"的数据项？').then(function() { return delWarningRule(ruleIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('sam/warningRule/export', { ...this.queryParams }, `warningRule_${new Date().getTime()}.xlsx`) }
  }
}
</script>
