<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学号" prop="studentNo"><el-input v-model="queryParams.studentNo" placeholder="请输入学号" clearable style="width:150px" @keyup.enter="handleQuery"/></el-form-item>
      <el-form-item label="姓名" prop="studentName"><el-input v-model="queryParams.studentName" placeholder="请输入姓名" clearable style="width:150px" @keyup.enter="handleQuery"/></el-form-item>
      <el-form-item label="手续状态" prop="procedureStatus">
        <el-select v-model="queryParams.procedureStatus" placeholder="请选择" clearable style="width:140px">
          <el-option label="未办理" value="0" /><el-option label="办理中" value="1" /><el-option label="已完成" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="离校手续" name="manage">
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5"><el-button type="primary" plain icon="Refresh" size="small" @click="handleInit" v-hasPermi="['sam:graduationProcedure:init']">初始化手续</el-button></el-col>
          <el-col :span="1.5"><el-button type="success" plain icon="Cpu" size="small" @click="handleAutoCheck" v-hasPermi="['sam:graduationProcedure:autoCheck']">自动判定</el-button></el-col>
          <el-col :span="1.5"><el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:graduationProcedure:remove']">删除</el-button></el-col>
          <el-col :span="1.5"><el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['sam:graduationProcedure:export']">导出</el-button></el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="procedureList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="学号" align="center" prop="studentNo" width="140"/>
          <el-table-column label="姓名" align="center" prop="studentName" width="100"/>
          <el-table-column label="班级" align="center" prop="className" :show-overflow-tooltip="true"/>
          <el-table-column label="环节进度" align="center" width="120">
            <template #default="scope"><span>{{ scope.row.doneStepCount || 0 }} / {{ scope.row.totalStepCount || 0 }}</span></template>
          </el-table-column>
          <el-table-column label="手续状态" align="center" prop="procedureStatus" width="100">
            <template #default="scope"><el-tag :type="scope.row.procedureStatus === '2' ? 'success' : (scope.row.procedureStatus === '1' ? 'warning' : 'info')" size="small">{{ statusFormat(scope.row.procedureStatus) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="完成日期" align="center" prop="completeDate" width="110" />
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
            <template #default="scope">
              <el-button size="small" link icon="Finished" @click="openHandle(scope.row)" v-hasPermi="['sam:graduationProcedure:handle']">环节办理</el-button>
              <el-button size="small" link icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['sam:graduationProcedure:remove']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </el-tab-pane>

      <el-tab-pane label="办理总览" name="stat">
        <el-row :gutter="16" class="mb8">
          <el-col :span="6"><div class="stat-card"><div class="stat-num">{{ overview.total || 0 }}</div><div class="stat-label">离校手续总数</div></div></el-col>
          <el-col :span="6"><div class="stat-card ok"><div class="stat-num">{{ overview.completed || 0 }}</div><div class="stat-label">已完成</div></div></el-col>
          <el-col :span="6"><div class="stat-card warn"><div class="stat-num">{{ overview.inProgress || 0 }}</div><div class="stat-label">办理中</div></div></el-col>
          <el-col :span="6"><div class="stat-card danger"><div class="stat-num">{{ overview.notStarted || 0 }}</div><div class="stat-label">未办理</div></div></el-col>
        </el-row>
        <el-table v-loading="statLoading" :data="stepStat" border>
          <el-table-column type="index" label="#" width="55" align="center"/>
          <el-table-column label="环节名称" align="center" prop="stepName"/>
          <el-table-column label="环节编码" align="center" prop="stepKey"/>
          <el-table-column label="应办理数" align="center" prop="total" width="120"/>
          <el-table-column label="已办理数" align="center" prop="doneCount" width="120"/>
          <el-table-column label="完成率" align="center" width="220">
            <template #default="scope"><el-progress :percentage="pct(scope.row)" :color="rateColor"/></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 环节办理对话框 -->
    <el-dialog title="离校环节办理" v-model="handleOpen" width="640px" append-to-body>
      <el-table v-loading="itemLoading" :data="items" border>
        <el-table-column label="环节" align="center" prop="stepName"/>
        <el-table-column label="必办" align="center" width="80"><template #default="scope"><el-tag v-if="scope.row.requiredFlag==='1'" type="danger" size="small">必办</el-tag><span v-else>可选</span></template></el-table-column>
        <el-table-column label="判定方式" align="center" width="110"><template #default="scope"><el-tag :type="scope.row.checkType==='1'?'success':'info'" size="small">{{ scope.row.checkType==='1'?'自动':'人工' }}</el-tag></template></el-table-column>
        <el-table-column label="办理时间" align="center" prop="checkTime" width="160"/>
        <el-table-column label="已办" align="center" width="80">
          <template #default="scope"><el-checkbox :model-value="scope.row.itemStatus==='1'" @change="v => onToggle(scope.row, v)"/></template>
        </el-table-column>
      </el-table>
      <template #footer>
        <div class="dialog-footer" style="text-align:right"><el-button type="primary" @click="handleOpen=false">关 闭</el-button></div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
// Vue2→Vue3 迁移：el-icon-* → 图标组件名；slot-scope → #default；:visible.sync → v-model；
// .sync → v-model:xxx；slot="footer" → <template #footer>；type="text" → link；size mini → small；
// el-checkbox :value → :model-value；tab-click 回调 tab.name → tab.paneName；this.$set → 直接赋值；
// @keyup.enter.native → @keyup.enter。业务逻辑与 Vue2 保持一致。
import { listGraduationProcedure, delGraduationProcedure, initProcedure, autoCheckProcedure, listProcedureItems, toggleProcedureItem, procedureStatOverview } from '@/api/sam/graduationProcedure'

export default {
  name: 'GraduationProcedure',
  data() {
    return {
      loading: true, statLoading: false, ids: [], multiple: true, showSearch: true, total: 0,
      procedureList: [], activeTab: 'manage', overview: {}, stepStat: [],
      handleOpen: false, itemLoading: false, items: [], curProcedureId: null,
      queryParams: { pageNum: 1, pageSize: 10, studentNo: undefined, studentName: undefined, procedureStatus: undefined }
    }
  },
  created() { this.getList() },
  methods: {
    statusFormat(s) { return { '0': '未办理', '1': '办理中', '2': '已完成' }[s] || s },
    rateColor(p) { return p >= 100 ? '#67C23A' : p >= 50 ? '#E6A23C' : '#F56C6C' },
    pct(row) { const t = Number(row.total || 0); return t === 0 ? 0 : Math.round(Number(row.doneCount || 0) * 100 / t) },
    getList() {
      this.loading = true
      listGraduationProcedure(this.queryParams).then(res => { this.procedureList = res.rows; this.total = res.total }).finally(() => { this.loading = false })
    },
    loadStat() {
      this.statLoading = true
      procedureStatOverview().then(res => { const d = res.data || {}; this.overview = d.procedure || {}; this.stepStat = d.steps || [] }).finally(() => { this.statLoading = false })
    },
    handleTabClick(tab) { if (tab.paneName === 'stat') this.loadStat() },
    handleQuery() { this.queryParams.pageNum = 1; this.getList(); if (this.activeTab === 'stat') this.loadStat() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.procedureId); this.multiple = !selection.length },
    handleInit() {
      this.$modal.confirm('将为全部已毕业（学籍状态=毕业）且尚无离校手续的学生初始化手续及环节明细（已存在自动跳过），是否继续？').then(() => initProcedure(null))
        .then(res => { this.$modal.msgSuccess(res.msg || '初始化完成'); this.getList() }).catch(() => {})
    },
    handleAutoCheck() {
      this.$modal.confirm('将按各环节配置的数据源自动判定办理状态并重算手续完成度，是否继续？').then(() => autoCheckProcedure(null))
        .then(res => { this.$modal.msgSuccess(res.msg || '自动判定完成'); this.getList(); if (this.activeTab === 'stat') this.loadStat() }).catch(() => {})
    },
    openHandle(row) {
      this.curProcedureId = row.procedureId; this.handleOpen = true; this.itemLoading = true; this.items = []
      listProcedureItems(row.procedureId).then(res => { this.items = res.data || [] }).finally(() => { this.itemLoading = false })
    },
    onToggle(item, checked) {
      toggleProcedureItem({ procedureId: this.curProcedureId, stepId: item.stepId, done: checked }).then(() => {
        item.itemStatus = checked ? '1' : '0'
        item.checkType = '0'
        item.checkTime = checked ? new Date() : null
        this.$modal.msgSuccess('操作成功'); this.getList()
      })
    },
    handleDelete(row) {
      const procedureIds = row.procedureId ? [row.procedureId] : this.ids
      this.$modal.confirm('是否确认删除选中的离校手续？').then(() => delGraduationProcedure(procedureIds)).then(() => { this.getList(); this.$modal.msgSuccess('删除成功') }).catch(() => {})
    },
    handleExport() { this.download('/sam/graduationProcedure/export', { ...this.queryParams }, `graduationProcedure_${new Date().getTime()}.xlsx`) }
  }
}
</script>

<style scoped>
.stat-card { background: var(--dt-fill-light); border-radius: 6px; padding: 18px 16px; text-align: center; }
.stat-card.ok { background: var(--el-color-success-light-9); }
.stat-card.warn { background: var(--el-color-warning-light-9); }
.stat-card.danger { background: var(--el-color-danger-light-9); }
.stat-num { font-size: 26px; font-weight: 600; color: var(--dt-text-primary); }
.stat-label { margin-top: 6px; font-size: 13px; color: var(--dt-text-secondary); }
</style>
