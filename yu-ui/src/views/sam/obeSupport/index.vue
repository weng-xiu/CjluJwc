<template>
  <div class="app-container">
    <!-- F1 OBE 课程支撑矩阵（Vue2 主栈）：后端 SamObeController /sam/obe/support，自 yu-ui-vue3 同名页回植移植。
         课程为大实体→el-select 原生远程搜索（N7 禁全量拉取）；指标点随培养方案联动加载。
         列表已联表返回 courseName/ipCode 直显；编辑回显用详情返回的 courseName 合成课程选项不显裸 ID。 -->
    <el-alert title="用途说明" type="info" :closable="false" style="margin-bottom:10px;">
      <div>课程支撑矩阵定义"某课程以多大权重支撑某指标点"，是达成度计算的数据来源：达成度由支撑课程的成绩按支撑权重加权得出。</div>
    </el-alert>

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="培养方案" prop="planId">
        <el-select v-model="queryParams.planId" placeholder="输入方案名搜索" clearable filterable remote reserve-keyword :remote-method="searchPlan" :loading="planLoading" style="width:220px" @change="onQueryPlanChange">
          <el-option v-for="item in planOptions" :key="item.planId" :label="item.planName" :value="item.planId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="指标点" prop="ipId">
        <el-select v-model="queryParams.ipId" placeholder="请先选方案" clearable filterable style="width:220px">
          <el-option v-for="item in queryIndicatorOptions" :key="item.ipId" :label="item.ipCode + ' ' + item.ipName" :value="item.ipId"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['sam:obeSupport:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['sam:obeSupport:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:obeSupport:remove']">删除</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <app-skeleton :loading="loading && !supportList.length" :rows="6">
    <el-table v-loading="loading" :data="supportList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="课程名称" align="center" prop="courseName" min-width="180" show-overflow-tooltip>
        <template slot-scope="scope">{{ scope.row.courseName || scope.row.courseId }}</template>
      </el-table-column>
      <el-table-column label="指标点编号" align="center" prop="ipCode" width="140" show-overflow-tooltip>
        <template slot-scope="scope">{{ scope.row.ipCode || scope.row.ipId }}</template>
      </el-table-column>
      <el-table-column label="支撑权重" align="center" prop="supportWeight" width="110">
        <template slot-scope="scope">{{ scope.row.supportWeight != null ? scope.row.supportWeight : '—' }}</template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sortOrder" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['sam:obeSupport:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['sam:obeSupport:remove']">删除</el-button>
        </template>
      </el-table-column>
      <template slot="empty"><div class="table-empty">暂无课程支撑关系，可调整查询条件或新增</div></template>
    </el-table>
    <app-empty v-if="!loading && !supportList.length" description="暂无学籍学位数据，可调整筛选条件或先发起对应业务" />
    </app-skeleton>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>

    <el-dialog :close-on-click-modal="false" :title="title" :visible.sync="open" :before-close="guardedBeforeClose" width="640px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="培养方案" prop="planId">
          <el-select v-model="form.planId" placeholder="输入方案名搜索" filterable remote reserve-keyword :remote-method="searchPlan" :loading="planLoading" style="width:100%" @change="onFormPlanChange">
            <el-option v-for="item in formPlanOptions" :key="item.planId" :label="item.planName" :value="item.planId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="支撑指标点" prop="ipId">
          <el-select v-model="form.ipId" placeholder="请先选方案再选指标点" filterable :disabled="!form.planId" style="width:100%">
            <el-option v-for="item in formIndicatorOptions" :key="item.ipId" :label="item.ipCode + ' ' + item.ipName" :value="item.ipId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="支撑课程" prop="courseId">
          <el-select v-model="form.courseId" placeholder="输入课程名搜索" filterable remote reserve-keyword :remote-method="searchCourse" :loading="courseLoading" style="width:100%">
            <el-option v-for="item in courseFormOptions" :key="item.courseId" :label="item.courseName" :value="item.courseId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="支撑权重" prop="supportWeight">
          <el-input-number v-model="form.supportWeight" :min="0" :max="1" :step="0.1" :precision="2" controls-position="right" style="width:100%" />
          <div class="form-tip">该课程对指标点的支撑强度（0~1），达成度按此权重加权课程成绩。</div>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder"><el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width:100%" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="guardedCancel(cancel)">取 消</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import { listSupport, getSupport, delSupport, addSupport, updateSupport, listIndicator } from "@/api/sam/obe"
import { listPlan } from "@/api/tpm/plan"
import { listCourseLib } from "@/api/tpm/courseLib"
import { reqChange } from "@/utils/validate"

import dialogGuard from "@/utils/dialogGuard"
export default {
  mixins: [dialogGuard],
  name: "ObeSupport",
  dicts: ['sys_normal_disable'],
  data() {
    return {
      loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0,
      supportList: [], planOptions: [], planLoading: false,
      queryIndicatorOptions: [], formIndicatorOptions: [],
      courseOptions: [], courseLoading: false,
      title: "", open: false,
      queryParams: { pageNum: 1, pageSize: 10, planId: null, ipId: null },
      form: {},
      rules: {
        planId: [reqChange("培养方案")],
        ipId: [reqChange("支撑指标点")],
        courseId: [reqChange("支撑课程")]
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
    },
    // 编辑回显：详情带 courseName 时合成课程选项，避免远程结果未命中显裸 ID
    courseFormOptions() {
      const opts = this.courseOptions
      if (this.form && this.form.courseId != null && this.form.courseName && !opts.some(c => c.courseId === this.form.courseId)) {
        return [{ courseId: this.form.courseId, courseName: this.form.courseName }, ...opts]
      }
      return opts
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listSupport(this.queryParams).then(response => { this.supportList = response.rows; this.total = response.total; this.loading = false }).catch(() => { this.loading = false })
    },
    searchPlan(query) {
      if (!query) { this.planOptions = []; return }
      this.planLoading = true
      listPlan({ planName: query, pageNum: 1, pageSize: 20 }).then(res => { this.planOptions = res.rows || []; this.planLoading = false }).catch(() => { this.planLoading = false })
    },
    searchCourse(query) {
      if (!query) { this.courseOptions = []; return }
      this.courseLoading = true
      listCourseLib({ courseName: query, pageNum: 1, pageSize: 20 }).then(res => { this.courseOptions = res.rows || []; this.courseLoading = false }).catch(() => { this.courseLoading = false })
    },
    loadIndicators(planId, target) {
      if (!planId) { this[target] = []; return }
      listIndicator({ planId, pageNum: 1, pageSize: 500 }).then(res => { this[target] = res.rows || [] })
    },
    onQueryPlanChange(planId) { this.queryParams.ipId = null; this.loadIndicators(planId, 'queryIndicatorOptions') },
    onFormPlanChange(planId) { this.form.ipId = null; this.loadIndicators(planId, 'formIndicatorOptions') },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { supportId: null, planId: null, planName: null, ipId: null, courseId: null, courseName: null, supportWeight: 0.5, sortOrder: 0, status: "0" }
      this.formIndicatorOptions = []
      this.courseOptions = []
      this.resetForm("form")
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.queryIndicatorOptions = []; this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.supportId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.captureDialogSnapshot(); this.title = "添加课程支撑" },
    handleUpdate(row) {
      this.reset()
      const supportId = row.supportId || this.ids
      getSupport(supportId).then(response => {
        this.form = response.data
        if (this.form.planId) { this.loadIndicators(this.form.planId, 'formIndicatorOptions') }
        if (this.form.courseName) { this.courseOptions = [{ courseId: this.form.courseId, courseName: this.form.courseName }] }
        this.open = true; this.captureDialogSnapshot(); this.title = "修改课程支撑"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.supportId != null) {
            updateSupport(this.form).then(() => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() })
          } else {
            addSupport(this.form).then(() => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() })
          }
        }
      })
    },
    handleDelete(row) {
      const supportIds = row.supportId || this.ids
      this.$modal.confirm('是否确认删除选中的课程支撑关系数据项？').then(function() { return delSupport(supportIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.form-tip { color: var(--dt-text-secondary); font-size: 12px; line-height: 1.5; }
.table-empty { color: var(--dt-text-secondary); font-size: 13px; padding: 20px 0; }
</style>
