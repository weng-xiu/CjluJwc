<template>
  <div class="app-container">
    <!-- F6 服务大厅时效看板（Vue3 栈）：消费后端 ServiceDeskController（N3 已交付）的
         catalog/dashboard/urge 三端点，对 sys_todo 只读聚合；图表走 BaseChart 统一封装。 -->
    <el-row :gutter="16" class="kpi-row" v-loading="loading">
      <el-col :xs="12" :sm="6">
        <div class="kpi-card">
          <div class="kpi-icon" style="background:var(--el-color-primary)"><el-icon><Tickets /></el-icon></div>
          <div class="kpi-body"><div class="kpi-value">{{ dashboard.totalPending }}</div><div class="kpi-label">在办待办总数</div></div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="kpi-card">
          <div class="kpi-icon" style="background:var(--el-color-success)"><el-icon><CircleCheck /></el-icon></div>
          <div class="kpi-body"><div class="kpi-value">{{ dashboard.normalCount }}</div><div class="kpi-label">正常时效内</div></div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="kpi-card">
          <div class="kpi-icon" style="background:var(--el-color-warning)"><el-icon><AlarmClock /></el-icon></div>
          <div class="kpi-body"><div class="kpi-value">{{ dashboard.nearDueCount }}</div><div class="kpi-label">临期待办</div></div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="kpi-card">
          <div class="kpi-icon" style="background:var(--el-color-danger)"><el-icon><Warning /></el-icon></div>
          <div class="kpi-body"><div class="kpi-value">{{ dashboard.overdueCount }}</div><div class="kpi-label">超时待办</div></div>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>各服务事项时效分布（堆叠）</span>
          <span class="header-extra" v-if="dashboard.generatedAt">统计时间：{{ dashboard.generatedAt }}</span>
        </div>
      </template>
      <base-chart :option="agingOption" :loading="loading" height="320px" empty-text="当前没有在办待办" />
    </el-card>

    <el-card shadow="never" class="detail-card">
      <template #header>
        <div class="card-header">
          <span>事项时效明细</span>
          <div>
            <el-button size="small" icon="Refresh" @click="loadDashboard">刷新</el-button>
            <el-button size="small" type="danger" plain icon="Bell" v-hasPermi="['system:serviceDesk:urge']" :loading="urging" @click="handleUrge">一键超时催办</el-button>
          </div>
        </div>
      </template>
      <app-skeleton :loading="loading && !dashboard.items.length" :rows="6">
        <el-table :data="dashboard.items" size="small" border row-key="code">
          <el-table-column label="服务事项" prop="name" min-width="160" show-overflow-tooltip />
          <el-table-column label="编码" prop="code" width="140" />
          <el-table-column label="分类" prop="category" width="90" />
          <el-table-column label="时效基线" width="100">
            <template #default="scope">{{ scope.row.slaHours }} 小时</template>
          </el-table-column>
          <el-table-column label="在办数" prop="total" width="90" sortable />
          <el-table-column label="正常" prop="normal" width="80" />
          <el-table-column label="临期" prop="nearDue" width="80">
            <template #default="scope"><span :class="{ 'text-warn': scope.row.nearDue > 0 }">{{ scope.row.nearDue }}</span></template>
          </el-table-column>
          <el-table-column label="超时" prop="overdue" width="80">
            <template #default="scope"><span :class="{ 'text-danger': scope.row.overdue > 0 }">{{ scope.row.overdue }}</span></template>
          </el-table-column>
          <el-table-column label="最老挂起(小时)" prop="oldestHours" width="130" sortable />
          <el-table-column label="超时率" width="100">
            <template #default="scope">{{ overdueRate(scope.row) }}</template>
          </el-table-column>
        </el-table>
        <app-empty v-if="!loading && !dashboard.items.length" description="暂无在办待办数据" />
      </app-skeleton>
    </el-card>

    <el-card shadow="never" class="detail-card">
      <template #header><span>可办理事项目录与时效基线</span></template>
      <app-skeleton :loading="catalogLoading && !catalog.length" :rows="5">
        <el-table :data="catalog" size="small" border row-key="code">
          <el-table-column label="事项编码" prop="code" width="160" />
          <el-table-column label="事项名称" prop="name" min-width="180" show-overflow-tooltip />
          <el-table-column label="分类" prop="category" width="100" />
          <el-table-column label="SLA 时效基线" width="140">
            <template #default="scope">{{ scope.row.slaHours }} 小时</template>
          </el-table-column>
          <el-table-column label="临期线" min-width="120">
            <template #default="scope">≥ {{ nearDueHours(scope.row) }} 小时</template>
          </el-table-column>
        </el-table>
        <app-empty v-if="!catalogLoading && !catalog.length" description="暂无可办理事项" />
      </app-skeleton>
    </el-card>
  </div>
</template>

<script>
import { getServiceDeskCatalog, getServiceDeskDashboard, urgeOverdue } from "@/api/system/serviceDesk"
import { Tickets, CircleCheck, AlarmClock, Warning } from "@element-plus/icons-vue"

export default {
  name: "ServiceDesk",
  components: { Tickets, CircleCheck, AlarmClock, Warning },
  data() {
    return {
      loading: false,
      catalogLoading: false,
      urging: false,
      dashboard: { generatedAt: null, totalPending: 0, normalCount: 0, nearDueCount: 0, overdueCount: 0, items: [] },
      catalog: []
    }
  },
  computed: {
    // 正常/临期/超时三档堆叠柱（与后端 ServiceItemDef.AgingLevel 同口径）
    agingOption() {
      const items = this.dashboard.items
      const names = items.map(i => i.name)
      return {
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        legend: { data: ['正常', '临期', '超时'], bottom: 0 },
        grid: { left: '3%', right: '4%', bottom: '18%', containLabel: true },
        xAxis: { type: 'category', data: names, axisLabel: { interval: 0, rotate: names.length > 8 ? 30 : 0 } },
        yAxis: { type: 'value', minInterval: 1 },
        series: [
          { name: '正常', type: 'bar', stack: 'aging', data: items.map(i => i.normal), itemStyle: { color: '#67c23a' }, barMaxWidth: 32 },
          { name: '临期', type: 'bar', stack: 'aging', data: items.map(i => i.nearDue), itemStyle: { color: '#e6a23c' }, barMaxWidth: 32 },
          { name: '超时', type: 'bar', stack: 'aging', data: items.map(i => i.overdue), itemStyle: { color: '#f56c6c' }, barMaxWidth: 32 }
        ]
      }
    }
  },
  created() {
    this.loadDashboard()
    this.loadCatalog()
  },
  methods: {
    loadDashboard() {
      this.loading = true
      getServiceDeskDashboard().then(res => {
        const d = res.data || {}
        this.dashboard = {
          generatedAt: d.generatedAt || null,
          totalPending: d.totalPending || 0,
          normalCount: d.normalCount || 0,
          nearDueCount: d.nearDueCount || 0,
          overdueCount: d.overdueCount || 0,
          items: d.items || []
        }
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    loadCatalog() {
      this.catalogLoading = true
      getServiceDeskCatalog().then(res => {
        this.catalog = res.data || []
        this.catalogLoading = false
      }).catch(() => { this.catalogLoading = false })
    },
    handleUrge() {
      this.$modal.confirm('将向全部超时待办的承办人下发催办提醒，是否继续？').then(() => {
        this.urging = true
        return urgeOverdue()
      }).then(res => {
        this.$modal.msgSuccess('催办完成，本次催办 ' + (res.data || 0) + ' 项')
        this.loadDashboard()
      }).catch(() => {}).finally(() => { this.urging = false })
    },
    // 与后端 ServiceItemDef.NEAR_DUE_RATIO=0.7 同口径的展示值（仅用于目录说明列）
    nearDueHours(row) {
      return Math.ceil(row.slaHours * 0.7)
    },
    overdueRate(row) {
      if (!row.total) return '—'
      return Math.round(row.overdue * 1000 / row.total) / 10 + '%'
    }
  }
}
</script>

<style scoped>
.kpi-row { margin-bottom: 16px; }
.kpi-card { display: flex; align-items: center; background: var(--dt-bg-container); border: 1px solid var(--dt-border-color-weak); border-radius: 8px; padding: 16px; margin-bottom: 8px; }
.kpi-icon { width: 44px; height: 44px; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 22px; margin-right: 12px; flex-shrink: 0; }
.kpi-value { font-size: 22px; font-weight: 600; color: var(--dt-text-primary); line-height: 1.2; }
.kpi-label { font-size: 13px; color: var(--dt-text-secondary); }
.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; }
.header-extra { font-weight: 400; font-size: 12px; color: var(--dt-text-secondary); }
.chart-card { margin-bottom: 16px; }
.detail-card { margin-bottom: 16px; }
.text-warn { color: var(--el-color-warning); font-weight: 600; }
.text-danger { color: var(--el-color-danger); font-weight: 600; }
</style>
