<template>
  <div class="app-container">
    <!-- F1 OBE 达成度看板：后端 SamObeController /sam/obe/dashboard + /calculate/*。Vue3 页新建接线，loadView 自动路由。
         群体口径走 dashboard（含汇总），个体口径走 calculate/student（远程搜索学生，N7 禁全量拉取）。
         图表统一用全局 base-chart（U4），仅构造 echarts option，主题/resize/空态内置。 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="88px">
      <el-form-item label="培养方案" prop="planId">
        <el-select v-model="queryParams.planId" placeholder="输入方案名搜索" filterable remote reserve-keyword :remote-method="searchPlan" :loading="planLoading" style="width:240px">
          <el-option v-for="item in planOptions" :key="item.planId" :label="item.planName" :value="item.planId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="统计口径">
        <el-radio-group v-model="scope">
          <el-radio-button value="cohort">群体（毕业生）</el-radio-button>
          <el-radio-button value="student">个体（学生）</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="scope === 'cohort'" label="学期" prop="semesterId">
        <el-select v-model="queryParams.semesterId" placeholder="全部学期" clearable filterable style="width:200px">
          <el-option v-for="item in semesterOptions" :key="item.semesterId" :label="item.semesterName" :value="item.semesterId"/>
        </el-select>
      </el-form-item>
      <el-form-item v-if="scope === 'student'" label="学生" prop="studentId">
        <el-select v-model="queryParams.studentId" placeholder="输入学号搜索" filterable remote reserve-keyword :remote-method="searchStudent" :loading="studentLoading" style="width:220px">
          <el-option v-for="item in studentOptions" :key="item.studentId" :label="fmtStudent(item)" :value="item.studentId"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="DataAnalysis" size="small" @click="handleCompute">计算达成度</el-button>
      </el-form-item>
    </el-form>

    <app-error-state v-if="error" :message="error" @retry="handleCompute" />
    <template v-else>
      <!-- 汇总 KPI -->
      <el-row :gutter="16" class="kpi-row" v-loading="loading">
        <el-col :xs="12" :sm="6">
          <div class="kpi-card">
            <div class="kpi-icon" style="background:#409eff"><el-icon><Document /></el-icon></div>
            <div class="kpi-body"><div class="kpi-value">{{ summary.totalRequirements }}</div><div class="kpi-label">毕业要求总数</div></div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="kpi-card">
            <div class="kpi-icon" style="background:#67c23a"><el-icon><CircleCheck /></el-icon></div>
            <div class="kpi-body"><div class="kpi-value">{{ summary.achievedRequirements }}</div><div class="kpi-label">已达成要求</div></div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="kpi-card">
            <div class="kpi-icon" style="background:#e6a23c"><el-icon><TrendCharts /></el-icon></div>
            <div class="kpi-body"><div class="kpi-value">{{ pct(summary.achievedRate) }}</div><div class="kpi-label">达成率</div></div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="kpi-card">
            <div class="kpi-icon" style="background:#909399"><el-icon><Odometer /></el-icon></div>
            <div class="kpi-body"><div class="kpi-value">{{ summary.averageAttainment != null ? summary.averageAttainment.toFixed(3) : '—' }}</div><div class="kpi-label">平均达成度</div></div>
          </div>
        </el-col>
      </el-row>

      <!-- 图表：达成度雷达 + 达成度对比条形 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :xs="24" :md="12">
          <el-card shadow="never"><template #header>毕业要求达成度雷达</template>
            <base-chart :option="radarOption" :loading="loading" height="340px" empty-text="请选择培养方案并计算达成度" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card shadow="never"><template #header>达成度 vs 目标值</template>
            <base-chart :option="barOption" :loading="loading" height="340px" empty-text="请选择培养方案并计算达成度" />
          </el-card>
        </el-col>
      </el-row>

      <!-- 明细：毕业要求 → 指标点 → 支撑课程 三级下钻 -->
      <el-card shadow="never" class="detail-card">
        <template #header>达成度明细（点击行下钻指标点与支撑课程）</template>
        <app-skeleton :loading="loading && !requirements.length" :rows="6">
          <el-table :data="requirements" row-key="grId">
            <el-table-column type="expand">
              <template #default="props">
                <el-table :data="props.row.indicators" size="small" border row-key="ipId">
                  <el-table-column type="expand">
                    <template #default="p2">
                      <el-table :data="p2.row.courses" size="small" border empty-text="该指标点暂无支撑课程成绩">
                        <el-table-column label="课程名称" prop="courseName" min-width="180" show-overflow-tooltip />
                        <el-table-column label="支撑权重" prop="supportWeight" width="120" />
                        <el-table-column label="课程成绩" prop="score" width="120">
                          <template #default="s">{{ s.row.score != null ? s.row.score : '—' }}</template>
                        </el-table-column>
                      </el-table>
                    </template>
                  </el-table-column>
                  <el-table-column label="指标点编号" prop="ipCode" width="140" />
                  <el-table-column label="指标点名称" prop="ipName" min-width="160" show-overflow-tooltip />
                  <el-table-column label="权重" prop="weight" width="80" />
                  <el-table-column label="达成度" prop="attainment" width="100">
                    <template #default="s">{{ s.row.attainment != null ? s.row.attainment.toFixed(3) : '—' }}</template>
                  </el-table-column>
                  <el-table-column label="目标值" prop="targetValue" width="90" />
                  <el-table-column label="是否达成" width="100">
                    <template #default="s"><el-tag :type="s.row.achieved === '1' ? 'success' : 'danger'">{{ s.row.achieved === '1' ? '达成' : '未达成' }}</el-tag></template>
                  </el-table-column>
                  <el-table-column label="有效支撑课程数" prop="courseCount" width="140" />
                </el-table>
              </template>
            </el-table-column>
            <el-table-column label="毕业要求编号" prop="grCode" width="140" />
            <el-table-column label="毕业要求名称" prop="grName" min-width="180" show-overflow-tooltip />
            <el-table-column label="达成度" prop="attainment" width="120">
              <template #default="scope">
                <el-progress :percentage="pct(scope.row.attainment)" :status="scope.row.achieved === '1' ? 'success' : 'exception'" />
              </template>
            </el-table-column>
            <el-table-column label="目标值" prop="targetValue" width="100">
              <template #default="scope">{{ scope.row.targetValue != null ? pct(scope.row.targetValue) : '—' }}</template>
            </el-table-column>
            <el-table-column label="是否达成" width="110">
              <template #default="scope"><el-tag :type="scope.row.achieved === '1' ? 'success' : 'danger'">{{ scope.row.achieved === '1' ? '达成' : '未达成' }}</el-tag></template>
            </el-table-column>
            <el-table-column label="指标点数" width="100">
              <template #default="scope">{{ (scope.row.indicators || []).length }}</template>
            </el-table-column>
          </el-table>
          <app-empty v-if="!loading && !requirements.length" description="请选择培养方案并点击“计算达成度”" />
        </app-skeleton>
      </el-card>
    </template>
  </div>
</template>

<script>
import { obeDashboard, calculateByStudent } from "@/api/sam/obe"
import { listPlan } from "@/api/tpm/plan"
import { listSemester } from "@/api/brm/semester"
import { listStudent } from "@/api/sam/student"

export default {
  name: "ObeAttainment",
  data() {
    return {
      loading: false, error: "",
      scope: "cohort",
      planOptions: [], planLoading: false, semesterOptions: [],
      studentOptions: [], studentLoading: false,
      queryParams: { planId: null, semesterId: null, studentId: null },
      summary: { totalRequirements: 0, achievedRequirements: 0, achievedRate: 0, averageAttainment: 0 },
      requirements: []
    }
  },
  computed: {
    // 雷达：达成度与目标值两维叠加，指标为各毕业要求
    radarOption() {
      if (!this.requirements.length) return {}
      const indicator = this.requirements.map(r => ({ name: r.grCode || r.grName, max: 1 }))
      return {
        tooltip: {},
        legend: { data: ["达成度", "目标值"], bottom: 0 },
        radar: { indicator, radius: "65%" },
        series: [{
          type: "radar",
          data: [
            { name: "达成度", value: this.requirements.map(r => r.attainment || 0), areaStyle: { opacity: 0.25 } },
            { name: "目标值", value: this.requirements.map(r => r.targetValue || 0), lineStyle: { type: "dashed" } }
          ]
        }]
      }
    },
    // 条形：各毕业要求达成度对比，目标值以散点标注
    barOption() {
      if (!this.requirements.length) return {}
      const names = this.requirements.map(r => r.grCode || r.grName)
      return {
        tooltip: { trigger: "axis", axisPointer: { type: "shadow" } },
        grid: { left: "3%", right: "6%", bottom: "3%", containLabel: true },
        xAxis: { type: "value", max: 1 },
        yAxis: { type: "category", data: names },
        series: [
          { name: "达成度", type: "bar", data: this.requirements.map(r => Number(((r.attainment || 0)).toFixed(3))), itemStyle: { color: "#409eff" }, barMaxWidth: 22 },
          { name: "目标值", type: "scatter", symbolSize: 12, data: this.requirements.map(r => r.targetValue || 0), itemStyle: { color: "#e6a23c" } }
        ]
      }
    }
  },
  created() { this.getOptions() },
  methods: {
    getOptions() {
      listSemester({ pageNum: 1, pageSize: 1000 }).then(res => { this.semesterOptions = res.rows || [] })
    },
    searchPlan(query) {
      if (!query) { this.planOptions = []; return }
      this.planLoading = true
      listPlan({ planName: query, pageNum: 1, pageSize: 20 }).then(res => { this.planOptions = res.rows || []; this.planLoading = false }).catch(() => { this.planLoading = false })
    },
    searchStudent(query) {
      if (!query) { this.studentOptions = []; return }
      this.studentLoading = true
      listStudent({ studentNo: query, pageNum: 1, pageSize: 20 }).then(res => { this.studentOptions = res.rows || []; this.studentLoading = false })
        .catch(() => { this.studentLoading = false })
    },
    fmtStudent(r) { return (r.studentName || '') + (r.studentNo ? '（' + r.studentNo + '）' : '') },
    pct(v) { return v != null ? Math.round(v * 1000) / 10 + '%' : '—' },
    handleCompute() {
      if (!this.queryParams.planId) { this.$modal.msgWarning("请先选择培养方案"); return }
      if (this.scope === 'student' && !this.queryParams.studentId) { this.$modal.msgWarning("个体口径请先选择学生"); return }
      this.error = ""
      this.loading = true
      if (this.scope === 'student') {
        calculateByStudent(this.queryParams.planId, this.queryParams.studentId).then(res => {
          this.requirements = res.data || []
          this.fillSummaryFromList(this.requirements)
          this.loading = false
        }).catch(() => { this.error = "达成度计算失败，请重试"; this.loading = false })
      } else {
        obeDashboard(this.queryParams.planId, this.queryParams.semesterId).then(res => {
          const d = res.data || {}
          this.summary = { totalRequirements: d.totalRequirements || 0, achievedRequirements: d.achievedRequirements || 0, achievedRate: d.achievedRate || 0, averageAttainment: d.averageAttainment || 0 }
          this.requirements = d.requirements || []
          this.loading = false
        }).catch(() => { this.error = "达成度看板加载失败，请重试"; this.loading = false })
      }
    },
    // 个体口径后端不返回汇总，前端按列表统计
    fillSummaryFromList(list) {
      const total = list.length
      let achieved = 0, sum = 0
      list.forEach(r => { if (r.achieved === '1') achieved++; sum += (r.attainment || 0) })
      this.summary = { totalRequirements: total, achievedRequirements: achieved, achievedRate: total ? achieved / total : 0, averageAttainment: total ? sum / total : 0 }
    }
  }
}
</script>

<style scoped>
.kpi-row { margin-bottom: 16px; }
.kpi-card { display: flex; align-items: center; background: var(--dt-bg-2, #fff); border: 1px solid var(--dt-border, #ebeef5); border-radius: 8px; padding: 16px; }
.kpi-icon { width: 44px; height: 44px; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 22px; margin-right: 12px; flex-shrink: 0; }
.kpi-value { font-size: 22px; font-weight: 600; color: var(--dt-text-1, #303133); line-height: 1.2; }
.kpi-label { font-size: 13px; color: var(--dt-text-3, #909399); }
.chart-row { margin-bottom: 16px; }
.chart-row .el-col { margin-bottom: 12px; }
:deep(.el-card__header) { padding: 12px 16px; font-weight: 600; }
</style>
