<template>
  <div class="app-container">
    <el-alert
      title="规则说明"
      type="info"
      :closable="false"
      style="margin-bottom:10px;"
    >
      <div>规则类型：1专业限制 / 2年级限制 / 3院系限制 / 4人数上限 / 5先修课程。</div>
      <div>限制值填写：专业/院系填ID（多个用逗号分隔）；年级填年份（如2024）；人数上限填数字；先修课填课程ID（多个用逗号分隔）。</div>
    </el-alert>
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="规则名称" prop="ruleName">
        <el-input v-model="queryParams.ruleName" placeholder="请输入规则名称" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="轮次" prop="roundId">
        <el-select v-model="queryParams.roundId" placeholder="请选择轮次" clearable>
          <el-option v-for="item in roundOptions" :key="item.roundId" :label="item.roundName" :value="item.roundId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="规则类型" prop="ruleType">
        <el-select v-model="queryParams.ruleType" placeholder="请选择规则类型" clearable>
          <el-option v-for="dict in dict.type.tpm_rule_type" :key="dict.value" :label="dict.label" :value="dict.value"/>
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
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
      <el-table-column label="规则名称" align="center" prop="ruleName" min-width="120" show-overflow-tooltip />
      <el-table-column label="所属轮次" align="center" prop="roundId" min-width="120" show-overflow-tooltip>
        <template slot-scope="scope">{{ roundFormat(scope.row.roundId) }}</template>
      </el-table-column>
      <el-table-column label="规则类型" align="center" prop="ruleType" width="120">
        <template slot-scope="scope"><dict-tag :options="dict.type.tpm_rule_type" :value="scope.row.ruleType"/></template>
      </el-table-column>
      <el-table-column label="限制目标" align="center" prop="restrictTarget" width="120" show-overflow-tooltip />
      <el-table-column label="限制值" align="center" prop="restrictValue" min-width="160" show-overflow-tooltip />
      <el-table-column label="优先级" align="center" prop="priority" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['tpm:rule:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['tpm:rule:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="轮次" prop="roundId">
          <el-select v-model="form.roundId" placeholder="请选择轮次" style="width:100%">
            <el-option v-for="item in roundOptions" :key="item.roundId" :label="item.roundName" :value="item.roundId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="规则名称" prop="ruleName"><el-input v-model="form.ruleName" placeholder="请输入规则名称" /></el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="form.ruleType" placeholder="请选择规则类型" style="width:100%">
            <el-option v-for="dict in dict.type.tpm_rule_type" :key="dict.value" :label="dict.label" :value="dict.value"/>
          </el-select>
        </el-form-item>
        <el-form-item label="限制目标" prop="restrictTarget"><el-input v-model="form.restrictTarget" placeholder="规则对象描述，如 专业/年级/院系" /></el-form-item>
        <el-form-item label="限制值" prop="restrictValue">
          <el-input v-model="form.restrictValue" type="textarea" :rows="2" placeholder="专业/院系填ID（逗号分隔）；年级填年份如2024；人数填数字；先修课填课程ID（逗号分隔）" />
        </el-form-item>
        <el-form-item label="优先级" prop="priority"><el-input-number v-model="form.priority" :min="0" controls-position="right" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listRule, getRule, delRule, addRule, updateRule } from "@/api/tpm/rule"
import { listRound } from "@/api/tpm/round"

export default {
  name: "Rule",
  dicts: ['sys_normal_disable', 'tpm_rule_type'],
  data() {
    return {
      loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0,
      ruleList: [], roundOptions: [], title: "", open: false,
      queryParams: { pageNum: 1, pageSize: 10, ruleName: null, ruleType: null, status: null, roundId: null },
      form: {},
      rules: {
        roundId: [{ required: true, message: "轮次不能为空", trigger: "change" }],
        ruleName: [{ required: true, message: "规则名称不能为空", trigger: "blur" }],
        ruleType: [{ required: true, message: "规则类型不能为空", trigger: "change" }]
      }
    }
  },
  created() { this.getList(); this.getRounds() },
  methods: {
    getList() {
      this.loading = true
      listRule(this.queryParams).then(response => { this.ruleList = response.rows; this.total = response.total; this.loading = false })
    },
    getRounds() {
      listRound({ pageNum: 1, pageSize: 1000 }).then(response => { this.roundOptions = response.rows || [] })
    },
    roundFormat(roundId) {
      const item = this.roundOptions.find(r => r.roundId === roundId)
      return item ? item.roundName : roundId
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { ruleId: null, roundId: null, ruleName: null, ruleType: null, restrictTarget: null, restrictValue: null, priority: 0, status: "0" }
      this.resetForm("form")
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.ruleId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加选课规则" },
    handleUpdate(row) {
      this.reset()
      const ruleId = row.ruleId || this.ids
      getRule(ruleId).then(response => { this.form = response.data; this.open = true; this.title = "修改选课规则" })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.ruleId != null) {
            updateRule(this.form).then(() => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() })
          } else {
            addRule(this.form).then(() => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() })
          }
        }
      })
    },
    handleDelete(row) {
      const ruleIds = row.ruleId || this.ids
      this.$modal.confirm('是否确认删除选课规则编号为"' + ruleIds + '"的数据项？').then(function() { return delRule(ruleIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    },
    handleExport() { this.download('tpm/rule/export', { ...this.queryParams }, `rule_${new Date().getTime()}.xlsx`) }
  }
}
</script>
