<template>
  <div class="app-container">
    <!-- F1 OBE 指标点（Vue2 主栈）：后端 SamObeController /sam/obe/indicator，自 yu-ui-vue3 同名页回植移植。
         毕业要求随培养方案联动：先选方案→按方案加载该方案下毕业要求作为下拉（体量小，非全量实体，不违反 N7）。 -->
    <el-alert title="用途说明" type="info" :closable="false" style="margin-bottom:10px;">
      <div>指标点是毕业要求的可衡量分解项，每条毕业要求可拆分为多个指标点，各自带权重与达成目标值，达成度按权重加权汇总回毕业要求。</div>
    </el-alert>

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="培养方案" prop="planId">
        <el-select v-model="queryParams.planId" placeholder="输入方案名搜索" clearable filterable remote reserve-keyword :remote-method="searchPlan" :loading="planLoading" style="width:220px" @change="onQueryPlanChange">
          <el-option v-for="item in planOptions" :key="item.planId" :label="item.planName" :value="item.planId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="所属要求" prop="grId">
        <el-select v-model="queryParams.grId" placeholder="请先选方案" clearable filterable style="width:200px">
          <el-option v-for="item in queryRequirementOptions" :key="item.grId" :label="item.grCode + ' ' + item.grName" :value="item.grId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="指标点编号" prop="ipCode">
        <el-input v-model="queryParams.ipCode" placeholder="请输入指标点编号" clearable @keyup.enter.native="handleQuery" style="width:160px"/>
      </el-form-item>
      <el-form-item label="指标点名称" prop="ipName">
        <el-input v-model="queryParams.ipName" placeholder="请输入指标点名称" clearable @keyup.enter.native="handleQuery" style="width:160px"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['sam:obeIndicator:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['sam:obeIndicator:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:obeIndicator:remove']">删除</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="indicatorList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="ipCode" width="120" show-overflow-tooltip />
      <el-table-column label="指标点名称" align="center" prop="ipName" min-width="160" show-overflow-tooltip />
      <el-table-column label="描述" align="center" prop="ipDesc" min-width="180" show-overflow-tooltip />
      <el-table-column label="权重" align="center" prop="weight" width="90">
        <template slot-scope="scope">{{ scope.row.weight != null ? scope.row.weight : '—' }}</template>
      </el-table-column>
      <el-table-column label="达成目标值" align="center" prop="targetValue" width="110">
        <template slot-scope="scope">{{ scope.row.targetValue != null ? scope.row.targetValue : '—' }}</template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sortOrder" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['sam:obeIndicator:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['sam:obeIndicator:remove']">删除</el-button>
        </template>
      </el-table-column>
      <template slot="empty"><div class="table-empty">暂无指标点，可调整查询条件或新增</div></template>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>

    <el-dialog :title="title" :visible.sync="open" width="640px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="培养方案" prop="planId">
          <el-select v-model="form.planId" placeholder="输入方案名搜索" filterable remote reserve-keyword :remote-method="searchPlan" :loading="planLoading" style="width:100%" @change="onFormPlanChange">
            <el-option v-for="item in formPlanOptions" :key="item.planId" :label="item.planName" :value="item.planId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="所属毕业要求" prop="grId">
          <el-select v-model="form.grId" placeholder="请先选方案再选毕业要求" filterable :disabled="!form.planId" style="width:100%">
            <el-option v-for="item in formRequirementOptions" :key="item.grId" :label="item.grCode + ' ' + item.grName" :value="item.grId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="指标点编号" prop="ipCode"><el-input v-model="form.ipCode" placeholder="如 GR-01-1" /></el-form-item>
        <el-form-item label="指标点名称" prop="ipName"><el-input v-model="form.ipName" placeholder="如 掌握工程基础原理" /></el-form-item>
        <el-form-item label="指标点描述" prop="ipDesc"><el-input v-model="form.ipDesc" type="textarea" :rows="3" placeholder="指标点的可衡量表述" /></el-form-item>
        <el-form-item label="权重" prop="weight">
          <el-input-number v-model="form.weight" :min="0" :max="1" :step="0.1" :precision="2" controls-position="right" style="width:100%" />
          <div class="form-tip">同一毕业要求下各指标点权重之和建议为 1。</div>
        </el-form-item>
        <el-form-item label="达成目标值" prop="targetValue">
          <el-input-number v-model="form.targetValue" :min="0" :max="1" :step="0.05" :precision="2" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder"><el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width:100%" /></el-form-item>
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
import { listIndicator, getIndicator, delIndicator, addIndicator, updateIndicator, listRequirement } from "@/api/sam/obe"
import { listPlan } from "@/api/tpm/plan"
import { req, reqChange } from "@/utils/validate"

export default {
  name: "ObeIndicator",
  dicts: ['sys_normal_disable'],
  data() {
    return {
      loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0,
      indicatorList: [], planOptions: [], planLoading: false,
      queryRequirementOptions: [], formRequirementOptions: [],
      title: "", open: false,
      queryParams: { pageNum: 1, pageSize: 10, planId: null, grId: null, ipCode: null, ipName: null },
      form: {},
      rules: {
        planId: [reqChange("培养方案")],
        grId: [reqChange("所属毕业要求")],
        ipCode: [req("指标点编号")],
        ipName: [req("指标点名称")]
      }
    }
  },
  computed: {
    // 编辑回显：详情带 planId/planName 时合成方案选项
    formPlanOptions() {
      const opts = this.planOptions
      if (this.form && this.form.planId != null && this.form.planName && !opts.some(p => p.planId === this.form.planId)) {
        return [{ planId: this.form.planId, planName: this.form.planName }, ...opts]
      }
      return opts
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listIndicator(this.queryParams).then(response => { this.indicatorList = response.rows; this.total = response.total; this.loading = false })
    },
    searchPlan(query) {
      if (!query) { this.planOptions = []; return }
      this.planLoading = true
      listPlan({ planName: query, pageNum: 1, pageSize: 20 }).then(res => { this.planOptions = res.rows || []; this.planLoading = false }).catch(() => { this.planLoading = false })
    },
    loadRequirements(planId, target) {
      if (!planId) { this[target] = []; return }
      listRequirement({ planId, pageNum: 1, pageSize: 500 }).then(res => { this[target] = res.rows || [] })
    },
    onQueryPlanChange(planId) { this.queryParams.grId = null; this.loadRequirements(planId, 'queryRequirementOptions') },
    onFormPlanChange(planId) { this.form.grId = null; this.loadRequirements(planId, 'formRequirementOptions') },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { ipId: null, planId: null, planName: null, grId: null, ipCode: null, ipName: null, ipDesc: null, weight: 0.5, targetValue: 0.7, sortOrder: 0, status: "0" }
      this.formRequirementOptions = []
      this.resetForm("form")
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.queryRequirementOptions = []; this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.ipId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加指标点" },
    handleUpdate(row) {
      this.reset()
      const ipId = row.ipId || this.ids
      getIndicator(ipId).then(response => {
        this.form = response.data
        if (this.form.planId) { this.loadRequirements(this.form.planId, 'formRequirementOptions') }
        this.open = true; this.title = "修改指标点"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.ipId != null) {
            updateIndicator(this.form).then(() => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() })
          } else {
            addIndicator(this.form).then(() => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() })
          }
        }
      })
    },
    handleDelete(row) {
      const ipIds = row.ipId || this.ids
      this.$modal.confirm('删除指标点将级联软删其课程支撑关系，是否确认删除？').then(function() { return delIndicator(ipIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.form-tip { color: var(--dt-text-secondary, #6b7280); font-size: 12px; line-height: 1.5; }
.table-empty { color: var(--dt-text-secondary, #909399); font-size: 13px; padding: 20px 0; }
</style>
