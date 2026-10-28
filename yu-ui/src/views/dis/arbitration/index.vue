<template>
  <div class="app-container">
    <!-- 裁决矩阵：数据源权威优先级配置 -->
    <el-card shadow="never" class="mb8">
      <div slot="header" class="clearfix">
        <span>主数据裁决矩阵（数据源权威优先级）</span>
        <span style="float:right;color:var(--dt-text-secondary);font-size:12px">数值越大越权威，未登记源回落 0；增删改后实时生效无需发版</span>
      </div>
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="68px">
        <el-form-item label="源编码" prop="sourceCode"><el-input v-model="queryParams.sourceCode" placeholder="请输入源编码" clearable @keyup.enter.native="handleQuery"/></el-form-item>
        <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
        <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
      </el-form>
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['dis:priority:add']">新增</el-button></el-col>
        <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['dis:priority:export']">导出</el-button></el-col>
      </el-row>
      <app-skeleton :loading="loading && !priorityList.length" :rows="6">
      <el-table v-loading="loading" :data="priorityList">
        <el-table-column label="源编码" align="center" prop="sourceCode" />
        <el-table-column label="源名称" align="center" prop="sourceName" />
        <el-table-column label="优先级" align="center" prop="priority" sortable />
        <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
        <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip />
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['dis:priority:edit']">修改</el-button>
            <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['dis:priority:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <app-empty v-if="!loading && !priorityList.length" description="暂无裁决配置，可新增数据源优先级（种子已内置 GRAD/JW/HR/FINANCE/CARD）" />
      </app-skeleton>
    </el-card>

    <!-- 试裁决：验证"以谁为准" -->
    <el-card shadow="never">
      <div slot="header">试裁决（按当前优先级裁决权威版本）</div>
      <el-form :model="pv" size="small" label-width="80px">
        <el-form-item label="业务主键"><el-input v-model="pv.businessKey" placeholder="如学号/工号，仅用于展示" style="width:260px"/></el-form-item>
        <el-form-item label="候选源">
          <el-table :data="pv.candidates" size="mini" border style="width:560px">
            <el-table-column label="源编码"><template slot-scope="scope"><el-input v-model="scope.row.sourceCode" placeholder="如 GRAD" size="mini"/></template></el-table-column>
            <el-table-column label="更新时间"><template slot-scope="scope"><el-date-picker v-model="scope.row.updateTime" type="datetime" size="mini" value-format="yyyy-MM-dd HH:mm:ss" placeholder="选择时间" style="width:100%"/></template></el-table-column>
            <el-table-column label="操作" width="70" align="center"><template slot-scope="scope"><el-button size="mini" type="text" icon="el-icon-delete" @click="removeCandidate(scope.$index)">删除</el-button></template></el-table-column>
          </el-table>
          <el-button size="mini" type="text" icon="el-icon-plus" @click="addCandidate">添加候选源</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="mini" :loading="pvLoading" @click="handlePreview" v-hasPermi="['dis:priority:query']">开始裁决</el-button>
        </el-form-item>
      </el-form>
      <div v-if="pvResult">
        <el-alert :title="'权威版本：' + (pvResult.winner ? pvResult.winner.sourceCode + '（优先级 ' + pvResult.winner.priority + '）' : '无有效候选')" :description="pvResult.note" type="success" :closable="false" show-icon class="mb8"/>
        <el-table :data="pvResult.candidates" size="mini" border style="width:560px">
          <el-table-column label="源编码" prop="sourceCode" align="center"/>
          <el-table-column label="优先级" prop="priority" align="center"/>
          <el-table-column label="更新时间" align="center"><template slot-scope="scope">{{ scope.row.updateTime || '—' }}</template></el-table-column>
          <el-table-column label="结果" align="center"><template slot-scope="scope"><el-tag v-if="scope.row.winner" type="success">胜出</el-tag><span v-else style="color:var(--dt-text-secondary)">—</span></template></el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-dialog :close-on-click-modal="false" :title="title" :visible.sync="open" :before-close="guardedBeforeClose" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="源编码" prop="sourceCode"><el-input v-model="form.sourceCode" placeholder="与裁决候选 sourceCode 对齐，如 GRAD" /></el-form-item>
        <el-form-item label="源名称" prop="sourceName"><el-input v-model="form.sourceName" placeholder="请输入来源系统名称" /></el-form-item>
        <el-form-item label="优先级" prop="priority"><el-input-number v-model="form.priority" :min="0" :max="9999" controls-position="right" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" placeholder="请输入备注" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="guardedCancel(cancel)">取 消</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import { listPriority, getPriority, delPriority, addPriority, updatePriority, previewArbitration } from "@/api/dis/priority"
import dialogGuard from "@/utils/dialogGuard"
export default {
  mixins: [dialogGuard],
  name: "DisArbitration",
  dicts: ['sys_normal_disable'],
  data() {
    return {
      loading: true, total: 0, priorityList: [], title: "", open: false,
      queryParams: { pageNum: 1, pageSize: 10, sourceCode: null, status: null },
      form: {},
      rules: {
        sourceCode: [{ required: true, message: "来源系统编码不能为空", trigger: "blur" }],
        priority: [{ required: true, message: "优先级不能为空", trigger: "blur" }]
      },
      pv: { businessKey: "", candidates: [{ sourceCode: "", updateTime: "" }] },
      pvLoading: false,
      pvResult: null
    }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listPriority(this.queryParams).then(res => { this.priorityList = res.rows; this.total = res.total; this.loading = false }).catch(() => { this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { priorityId: null, sourceCode: null, sourceName: null, priority: 0, status: "0", remark: null }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleAdd() { this.reset(); this.open = true; this.captureDialogSnapshot(); this.title = "新增数据源优先级" },
    handleUpdate(row) { this.reset(); getPriority(row.priorityId).then(res => { this.form = res.data; this.open = true; this.captureDialogSnapshot(); this.title = "修改数据源优先级" }) },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) return
        if (this.form.priorityId != null) { updatePriority(this.form).then(() => { this.$modal.msgSuccess("修改成功，裁决优先级已实时生效"); this.open = false; this.getList() }) }
        else { addPriority(this.form).then(() => { this.$modal.msgSuccess("新增成功，裁决优先级已实时生效"); this.open = false; this.getList() }) }
      })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除源"' + row.sourceCode + '"的优先级配置？').then(() => delPriority(row.priorityId)).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    },
    handleExport() { this.download('dis/priority/export', { ...this.queryParams }, `priority_${new Date().getTime()}.xlsx`) },
    addCandidate() { this.pv.candidates.push({ sourceCode: "", updateTime: "" }) },
    removeCandidate(i) { if (this.pv.candidates.length > 1) this.pv.candidates.splice(i, 1) },
    handlePreview() {
      const list = this.pv.candidates.filter(c => c.sourceCode && c.sourceCode.trim())
      if (!list.length) { this.$modal.msgWarning("请至少填写一个候选源编码"); return }
      this.pvLoading = true
      previewArbitration({ businessKey: this.pv.businessKey, candidates: list }).then(res => { this.pvResult = res.data; this.pvLoading = false })
        .catch(() => { this.pvLoading = false })
    }
  }
}
</script>
