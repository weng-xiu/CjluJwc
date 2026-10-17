<template>
  <div class="app-container">
    <!-- F1 OBE 毕业要求：后端已交付（SamObeController /sam/obe/requirement），Vue2/Vue3 双端此前均无页面，本 Vue3 页新建接线。
         约定：loadView 自动路由（views/sam/obeRequirement/index.vue 命中 sys_menu 组件路径 sam/obeRequirement/index）；
         培养方案为远程搜索 el-select（N7 禁全量拉取），编辑回显用详情返回的 planName 合成选项不显裸 ID。 -->
    <el-alert title="用途说明" type="info" :closable="false" style="margin-bottom:10px;">
      <div>毕业要求是 OBE 反向设计的顶层锚点：先按培养方案确定若干条毕业要求，再向下拆分指标点、由课程支撑，最终核算达成度。</div>
      <div>删除毕业要求会级联软删其下的指标点与课程支撑关系，请谨慎操作。</div>
    </el-alert>

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="培养方案" prop="planId">
        <el-select v-model="queryParams.planId" placeholder="输入方案名搜索" clearable filterable remote reserve-keyword :remote-method="searchPlan" :loading="planLoading" style="width:220px">
          <el-option v-for="item in planOptions" :key="item.planId" :label="item.planName" :value="item.planId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="要求编号" prop="grCode">
        <el-input v-model="queryParams.grCode" placeholder="请输入毕业要求编号" clearable @keyup.enter="handleQuery" style="width:180px"/>
      </el-form-item>
      <el-form-item label="要求名称" prop="grName">
        <el-input v-model="queryParams.grName" placeholder="请输入毕业要求名称" clearable @keyup.enter="handleQuery" style="width:180px"/>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width:140px">
          <el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['sam:obeRequirement:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['sam:obeRequirement:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:obeRequirement:remove']">删除</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <app-skeleton :loading="loading && !requirementList.length" :rows="6">
      <el-table v-loading="loading" :data="requirementList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="编号" align="center" prop="grCode" width="120" show-overflow-tooltip />
        <el-table-column label="毕业要求名称" align="center" prop="grName" min-width="160" show-overflow-tooltip />
        <el-table-column label="描述" align="center" prop="grDesc" min-width="200" show-overflow-tooltip />
        <el-table-column label="达成目标值" align="center" prop="targetValue" width="110">
          <template #default="scope">{{ scope.row.targetValue != null ? scope.row.targetValue : '—' }}</template>
        </el-table-column>
        <el-table-column label="排序" align="center" prop="sortOrder" width="80" />
        <el-table-column label="状态" align="center" prop="status" width="90">
          <template #default="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
          <template #default="scope">
            <el-button size="small" link icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['sam:obeRequirement:edit']">修改</el-button>
            <el-button size="small" link icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['sam:obeRequirement:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <app-empty v-if="!loading && !requirementList.length" description="暂无毕业要求，可调整查询条件或新增" />
    </app-skeleton>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList"/>

    <el-dialog :title="title" v-model="open" width="640px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="培养方案" prop="planId">
          <el-select v-model="form.planId" placeholder="输入方案名搜索" filterable remote reserve-keyword :remote-method="searchPlan" :loading="planLoading" style="width:100%">
            <el-option v-for="item in planFormOptions" :key="item.planId" :label="item.planName" :value="item.planId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="毕业要求编号" prop="grCode"><el-input v-model="form.grCode" placeholder="如 GR-01" /></el-form-item>
        <el-form-item label="毕业要求名称" prop="grName"><el-input v-model="form.grName" placeholder="如 工程知识" /></el-form-item>
        <el-form-item label="毕业要求描述" prop="grDesc"><el-input v-model="form.grDesc" type="textarea" :rows="3" placeholder="毕业要求的完整表述" /></el-form-item>
        <el-form-item label="达成目标值" prop="targetValue">
          <el-input-number v-model="form.targetValue" :min="0" :max="1" :step="0.05" :precision="2" controls-position="right" style="width:100%" />
          <div class="form-tip">取值 0~1，达成度达到该值判定为达成（如 0.7）。</div>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder"><el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width:100%" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :value="dict.value">{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer><div class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listRequirement, getRequirement, delRequirement, addRequirement, updateRequirement } from "@/api/sam/obe"
import { listPlan } from "@/api/tpm/plan"
import { req, reqChange } from "@/utils/validate"

export default {
  name: "ObeRequirement",
  dicts: ['sys_normal_disable'],
  data() {
    return {
      loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0,
      requirementList: [], planOptions: [], planLoading: false,
      title: "", open: false,
      queryParams: { pageNum: 1, pageSize: 10, planId: null, grCode: null, grName: null, status: null },
      form: {},
      rules: {
        planId: [reqChange("培养方案")],
        grCode: [req("毕业要求编号")],
        grName: [req("毕业要求名称")]
      }
    }
  },
  computed: {
    // 编辑回显：详情若带回 planName 则合成选项，避免下拉显裸 ID
    planFormOptions() {
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
      listRequirement(this.queryParams).then(response => { this.requirementList = response.rows; this.total = response.total; this.loading = false })
    },
    searchPlan(query) {
      if (!query) { this.planOptions = []; return }
      this.planLoading = true
      listPlan({ planName: query, pageNum: 1, pageSize: 20 }).then(res => { this.planOptions = res.rows || []; this.planLoading = false }).catch(() => { this.planLoading = false })
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { grId: null, planId: null, planName: null, grCode: null, grName: null, grDesc: null, targetValue: 0.7, sortOrder: 0, status: "0" }
      this.resetForm("form")
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.grId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加毕业要求" },
    handleUpdate(row) {
      this.reset()
      const grId = row.grId || this.ids
      getRequirement(grId).then(response => { this.form = response.data; this.open = true; this.title = "修改毕业要求" })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.grId != null) {
            updateRequirement(this.form).then(() => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() })
          } else {
            addRequirement(this.form).then(() => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() })
          }
        }
      })
    },
    handleDelete(row) {
      const grIds = row.grId || this.ids
      this.$modal.confirm('删除毕业要求将级联软删其下指标点与课程支撑，是否确认删除？').then(function() { return delRequirement(grIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.form-tip { color: var(--dt-text-secondary, #6b7280); font-size: 12px; line-height: 1.5; }
</style>
