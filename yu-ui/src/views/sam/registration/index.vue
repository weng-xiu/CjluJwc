<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学期" prop="semesterId">
        <el-select v-model="queryParams.semesterId" placeholder="请选择学期" clearable filterable style="width:200px">
          <el-option v-for="s in semesterOptions" :key="s.semesterId" :label="s.semesterName" :value="s.semesterId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="注册状态" prop="registerStatus">
        <el-select v-model="queryParams.registerStatus" placeholder="请选择状态" clearable style="width:150px">
          <el-option v-for="dict in dict.type.sam_register_status" :key="dict.value" :label="dict.label" :value="dict.value"/>
        </el-select>
      </el-form-item>
      <el-form-item label="学号" prop="studentNo"><el-input v-model="queryParams.studentNo" placeholder="请输入学号" clearable style="width:160px" @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="姓名" prop="studentName"><el-input v-model="queryParams.studentName" placeholder="请输入姓名" clearable style="width:160px" @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <!-- ================= 注册管理 ================= -->
      <el-tab-pane label="注册管理" name="manage">
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-refresh" size="mini" @click="handleInit" v-hasPermi="['sam:registration:init']">报到初始化</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="success" plain icon="el-icon-check" size="mini" :disabled="multiple" @click="handleBatch('1')" v-hasPermi="['sam:registration:register']">批量注册</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" plain icon="el-icon-time" size="mini" :disabled="multiple" @click="openDeferDialog" v-hasPermi="['sam:registration:register']">标记延迟</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="info" plain icon="el-icon-close" size="mini" :disabled="multiple" @click="handleBatch('0')" v-hasPermi="['sam:registration:register']">撤销未注册</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:registration:remove']">删除</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['sam:registration:export']">导出</el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55" align="center"/>
          <el-table-column label="学号" align="center" prop="studentNo" width="140"/>
          <el-table-column label="姓名" align="center" prop="studentName" width="100"/>
          <el-table-column label="院系" align="center" prop="deptName" :show-overflow-tooltip="true"/>
          <el-table-column label="班级" align="center" prop="className" :show-overflow-tooltip="true"/>
          <el-table-column label="学期" align="center" prop="semesterName" width="140"/>
          <el-table-column label="学籍" align="center" prop="studentStatus" width="80">
            <template slot-scope="scope"><span v-if="scope.row.studentStatus==='0'">在读</span><span v-else>非在读</span></template>
          </el-table-column>
          <el-table-column label="注册状态" align="center" prop="registerStatus" width="100">
            <template slot-scope="scope"><dict-tag :options="dict.type.sam_register_status" :value="scope.row.registerStatus"/></template>
          </el-table-column>
          <el-table-column label="注册时间" align="center" prop="registerTime" width="160"/>
          <el-table-column label="经办人" align="center" prop="registerBy" width="100"/>
          <el-table-column label="延迟原因" align="center" prop="deferReason" :show-overflow-tooltip="true"/>
          <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
            <template slot-scope="scope">
              <el-button v-if="scope.row.registerStatus!=='1'" size="mini" type="text" icon="el-icon-check" @click="handleRow(scope.row,'1')" v-hasPermi="['sam:registration:register']">注册</el-button>
              <el-button v-if="scope.row.registerStatus!=='2'" size="mini" type="text" icon="el-icon-time" @click="handleRow(scope.row,'2')" v-hasPermi="['sam:registration:register']">延迟</el-button>
              <el-button v-if="scope.row.registerStatus!=='0'" size="mini" type="text" icon="el-icon-close" @click="handleRow(scope.row,'0')" v-hasPermi="['sam:registration:register']">撤销</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
      </el-tab-pane>

      <!-- ================= 注册率统计 ================= -->
      <el-tab-pane label="注册率统计" name="stat">
        <el-row :gutter="16" class="mb8">
          <el-col :span="6"><div class="stat-card"><div class="stat-num">{{ overview.total || 0 }}</div><div class="stat-label">应注册人数</div></div></el-col>
          <el-col :span="6"><div class="stat-card ok"><div class="stat-num">{{ overview.registered || 0 }}</div><div class="stat-label">已注册</div></div></el-col>
          <el-col :span="6"><div class="stat-card warn"><div class="stat-num">{{ overview.deferred || 0 }}</div><div class="stat-label">延迟注册</div></div></el-col>
          <el-col :span="6"><div class="stat-card danger"><div class="stat-num">{{ overview.unregistered || 0 }}</div><div class="stat-label">未注册 · 注册率 {{ overview.registerRate || 0 }}%</div></div></el-col>
        </el-row>
        <el-table v-loading="statLoading" :data="deptStat" border>
          <el-table-column type="index" label="#" width="55" align="center"/>
          <el-table-column label="院系" align="center" prop="deptName"/>
          <el-table-column label="应注册" align="center" prop="total" width="100"/>
          <el-table-column label="已注册" align="center" prop="registered" width="100"/>
          <el-table-column label="延迟" align="center" prop="deferred" width="100"/>
          <el-table-column label="未注册" align="center" prop="unregistered" width="100"/>
          <el-table-column label="注册率" align="center" width="200">
            <template slot-scope="scope"><el-progress :percentage="Number(scope.row.registerRate || 0)" :color="rateColor"/></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 延迟原因对话框 -->
    <el-dialog title="标记延迟注册" :visible.sync="deferOpen" width="460px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="延迟原因">
          <el-input v-model="deferReason" type="textarea" :rows="3" placeholder="请填写延迟注册原因（如因故保留学籍）"/>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitDefer">确 定</el-button>
        <el-button @click="deferOpen=false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listRegistration, initRegistration, batchRegister, delRegistration, getCurrentSemester, statOverview, statByDept } from "@/api/sam/registration"
import { listSemester } from "@/api/brm/semester"

export default {
  name: "Registration",
  dicts: ['sam_register_status'],
  data() {
    return {
      loading: false, statLoading: false,
      ids: [], multiple: true, showSearch: true, total: 0,
      list: [], semesterOptions: [], activeTab: "manage",
      overview: {}, deptStat: [],
      deferOpen: false, deferReason: "",
      queryParams: { pageNum: 1, pageSize: 10, semesterId: null, registerStatus: null, studentNo: null, studentName: null }
    }
  },
  created() {
    this.loadSemesters()
    this.getList()
  },
  methods: {
    rateColor(p) { return p >= 95 ? '#67C23A' : p >= 80 ? '#E6A23C' : '#F56C6C' },
    loadSemesters() {
      listSemester({ pageNum: 1, pageSize: 100 }).then(res => { this.semesterOptions = res.rows || [] })
      getCurrentSemester().then(res => { if (res.data && this.queryParams.semesterId == null) { this.queryParams.semesterId = res.data } })
    },
    getList() {
      this.loading = true
      listRegistration(this.queryParams).then(res => { this.list = res.rows; this.total = res.total; this.loading = false }).catch(() => { this.loading = false })
    },
    loadStat() {
      this.statLoading = true
      statOverview(this.queryParams.semesterId).then(res => { this.overview = res.data || {}; }).finally(() => { this.statLoading = false })
      statByDept(this.queryParams.semesterId).then(res => { this.deptStat = res.data || [] }).finally(() => { this.statLoading = false })
    },
    handleTabClick(tab) { if (tab.name === 'stat') { this.loadStat() } },
    handleQuery() { this.queryParams.pageNum = 1; this.getList(); if (this.activeTab === 'stat') this.loadStat() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(i => i.registrationId); this.multiple = !selection.length },
    handleInit() {
      if (this.queryParams.semesterId == null) { this.$modal.msgError("请先选择要初始化报到的学期"); return }
      const sem = this.semesterOptions.find(s => s.semesterId === this.queryParams.semesterId)
      this.$modal.confirm('将为学期「' + (sem ? sem.semesterName : this.queryParams.semesterId) + '」的全部在读学生生成未注册记录（已存在的自动跳过），是否继续？').then(() => initRegistration(this.queryParams.semesterId))
        .then(res => { this.$modal.msgSuccess(res.msg || "初始化完成"); this.getList() }).catch(() => {})
    },
    handleBatch(status) {
      const map = { '1': '确认将选中的 ' + this.ids.length + ' 条记录标记为已注册？', '0': '确认将选中的 ' + this.ids.length + ' 条记录撤销为未注册？' }
      this.$modal.confirm(map[status]).then(() => batchRegister({ registrationIds: this.ids, status: status })).then(() => { this.$modal.msgSuccess("操作成功"); this.getList() }).catch(() => {})
    },
    handleRow(row, status) {
      if (status === '2') { this.ids = [row.registrationId]; this.deferReason = ""; this.deferOpen = true; return }
      const label = status === '1' ? '注册' : '撤销为未注册'
      this.$modal.confirm('确认对该生执行「' + label + '」？').then(() => batchRegister({ registrationIds: [row.registrationId], status: status }))
        .then(() => { this.$modal.msgSuccess("操作成功"); this.getList() }).catch(() => {})
    },
    openDeferDialog() { this.deferReason = ""; this.deferOpen = true },
    submitDefer() {
      if (!this.deferReason || !this.deferReason.trim()) { this.$modal.msgError("请填写延迟原因"); return }
      batchRegister({ registrationIds: this.ids, status: '2', deferReason: this.deferReason }).then(() => {
        this.$modal.msgSuccess("已标记延迟"); this.deferOpen = false; this.getList()
      })
    },
    handleDelete() {
      this.$modal.confirm('是否确认删除选中的 ' + this.ids.length + ' 条注册记录？').then(() => delRegistration(this.ids))
        .then(() => { this.$modal.msgSuccess("删除成功"); this.getList() }).catch(() => {})
    },
    handleExport() {
      this.download('sam/registration/export', { ...this.queryParams }, `registration_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style scoped>
.stat-card { background: #f5f7fa; border-radius: 6px; padding: 18px 16px; text-align: center; }
.stat-card.ok { background: #f0f9eb; }
.stat-card.warn { background: #fdf6ec; }
.stat-card.danger { background: #fef0f0; }
.stat-num { font-size: 26px; font-weight: 600; color: #303133; }
.stat-label { margin-top: 6px; font-size: 13px; color: #909399; }
</style>
