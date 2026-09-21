<template>
  <div class="app-container">
    <el-form :inline="true" size="small">
      <el-form-item label="学期ID"><el-input v-model="query.semesterId" placeholder="可选，按学期统计利用率/工作量" clearable style="width:200px" @keyup.enter.native="loadAll"/></el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-data-analysis" @click="loadAll" v-hasPermi="['brm:resourceStat:list']">生成分析</el-button>
      </el-form-item>
    </el-form>

    <!-- 总览卡片 -->
    <el-row :gutter="12" class="stat-cards" v-loading="overviewLoading">
      <el-col :xs="12" :sm="8" :md="3"><div class="stat-card"><div class="stat-label">教室总数</div><div class="stat-value">{{ classroom.total || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="3"><div class="stat-card"><div class="stat-label">已排课教室</div><div class="stat-value">{{ classroom.used || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="3"><div class="stat-card"><div class="stat-label">空闲教室</div><div class="stat-value">{{ classroom.idle || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="3"><div class="stat-card pass"><div class="stat-label">平均利用率</div><div class="stat-value">{{ classroom.avgUtilizationRate || 0 }}%</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="3"><div class="stat-card"><div class="stat-label">设备总数</div><div class="stat-value">{{ equipment.total || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="3"><div class="stat-card warn"><div class="stat-label">维保到期</div><div class="stat-value">{{ equipment.dueCount || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="3"><div class="stat-card"><div class="stat-label">在职教师</div><div class="stat-value">{{ teacher.active || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="3"><div class="stat-card"><div class="stat-label">人均周课时</div><div class="stat-value">{{ teacher.avgWeeklySessions || 0 }}</div></div></el-col>
    </el-row>

    <!-- 教室利用率图表 -->
    <el-row :gutter="12" style="margin-top:8px">
      <el-col :span="24">
        <el-card shadow="never"><div slot="header"><span>教室周课时占用 TOP15</span></div><div ref="utilChart" style="height:320px" v-loading="utilLoading"></div></el-card>
      </el-col>
    </el-row>

    <!-- 明细表 -->
    <el-tabs type="border-card" style="margin-top:12px">
      <el-tab-pane label="教室利用率">
        <el-table :data="utilList" v-loading="utilLoading" size="small" max-height="420">
          <el-table-column label="教室" prop="classroomName" show-overflow-tooltip/>
          <el-table-column label="教学楼" prop="buildingName" width="140" show-overflow-tooltip/>
          <el-table-column label="类型" prop="typeName" width="120" show-overflow-tooltip/>
          <el-table-column label="容量" prop="capacity" width="70" align="center"/>
          <el-table-column label="排课数" prop="scheduleCount" width="80" align="center"/>
          <el-table-column label="周课时" prop="weeklySessions" width="80" align="center"/>
          <el-table-column label="利用率(%)" prop="utilizationRate" width="100" align="center">
            <template slot-scope="scope"><el-tag :type="rateTag(scope.row.utilizationRate)" size="mini">{{ scope.row.utilizationRate }}%</el-tag></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="教师工作量">
        <el-table :data="workloadList" v-loading="workLoading" size="small" max-height="420">
          <el-table-column label="排名" type="index" width="55" align="center"/>
          <el-table-column label="教师" prop="teacherName" width="120" show-overflow-tooltip/>
          <el-table-column label="工号" prop="teacherCode" width="120" show-overflow-tooltip/>
          <el-table-column label="院系" prop="deptName" show-overflow-tooltip/>
          <el-table-column label="课程数" prop="courseCount" width="80" align="center"/>
          <el-table-column label="开课数" prop="offeringCount" width="80" align="center"/>
          <el-table-column label="排课数" prop="scheduleCount" width="80" align="center"/>
          <el-table-column label="周课时" prop="weeklySessions" width="90" align="center"/>
        </el-table>
      </el-tab-pane>
      <el-tab-pane>
        <span slot="label">维保到期提醒 <el-badge v-if="dueList.length" :value="dueList.length" class="item"/></span>
        <el-table :data="dueList" v-loading="dueLoading" size="small" max-height="420">
          <el-table-column label="设备" prop="equipName" show-overflow-tooltip/>
          <el-table-column label="类型" prop="equipType" width="120" show-overflow-tooltip/>
          <el-table-column label="型号" prop="model" width="140" show-overflow-tooltip/>
          <el-table-column label="所在教室" prop="classroomName" width="140" show-overflow-tooltip/>
          <el-table-column label="最近维保" prop="lastRepairDate" width="130" align="center"/>
          <el-table-column label="距今(天)" width="100" align="center">
            <template slot-scope="scope">
              <span v-if="scope.row.daysSince == null" style="color:#f56c6c">从未维保</span>
              <el-tag v-else :type="scope.row.daysSince >= 365 ? 'danger' : 'warning'" size="mini">{{ scope.row.daysSince }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { resourceOverview, classroomUtilization, teacherWorkload, maintenanceDue } from "@/api/brm/resourceStat"
export default {
  name: "ResourceStat",
  data() {
    return {
      query: { semesterId: null },
      classroom: {}, equipment: {}, teacher: {},
      overviewLoading: false, utilLoading: false, workLoading: false, dueLoading: false,
      utilList: [], workloadList: [], dueList: [],
      utilChart: null
    }
  },
  created() { this.loadAll() },
  mounted() { window.addEventListener('resize', this.resizeChart) },
  beforeDestroy() { window.removeEventListener('resize', this.resizeChart); if (this.utilChart) this.utilChart.dispose() },
  methods: {
    loadAll() {
      this.loadOverview()
      this.loadUtil()
      this.loadWorkload()
      this.loadDue()
    },
    loadOverview() {
      this.overviewLoading = true
      resourceOverview(this.query).then(res => {
        const d = res.data || {}
        this.classroom = d.classroom || {}
        this.equipment = d.equipment || {}
        this.teacher = d.teacher || {}
      }).finally(() => { this.overviewLoading = false })
    },
    loadUtil() {
      this.utilLoading = true
      classroomUtilization(this.query).then(res => {
        this.utilList = res.data || []
        this.renderChart()
      }).finally(() => { this.utilLoading = false })
    },
    renderChart() {
      const top = this.utilList.slice(0, 15)
      const names = top.map(i => i.classroomName)
      const rates = top.map(i => Number(i.utilizationRate) || 0)
      this.$nextTick(() => {
        if (!this.$refs.utilChart) return
        if (!this.utilChart) this.utilChart = echarts.init(this.$refs.utilChart)
        this.utilChart.setOption({
          tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}: {c}%' },
          grid: { left: 90, right: 30, top: 20, bottom: 30 },
          xAxis: { type: 'value', name: '利用率(%)', max: 100 },
          yAxis: { type: 'category', data: names, axisLabel: { fontSize: 11 } },
          series: [{
            type: 'bar', data: rates, barWidth: '60%',
            itemStyle: { color: p => p.value >= 60 ? '#67c23a' : (p.value >= 30 ? '#409eff' : '#e6a23c') },
            label: { show: true, position: 'right', formatter: '{c}%' }
          }]
        }, true)
      })
    },
    loadWorkload() {
      this.workLoading = true
      teacherWorkload(this.query).then(res => { this.workloadList = res.data || [] }).finally(() => { this.workLoading = false })
    },
    loadDue() {
      this.dueLoading = true
      maintenanceDue().then(res => { this.dueList = res.data || [] }).finally(() => { this.dueLoading = false })
    },
    rateTag(r) { return r >= 60 ? 'success' : (r >= 30 ? '' : 'warning') },
    resizeChart() { this.utilChart && this.utilChart.resize() }
  }
}
</script>

<style scoped>
.stat-cards .stat-card { background:#f5f7fa;border-radius:6px;padding:12px;text-align:center;margin-bottom:12px; }
.stat-cards .stat-card.pass { background:#f0f9eb; }
.stat-cards .stat-card.warn { background:#fdf6ec; }
.stat-label { color:#909399;font-size:13px; }
.stat-value { font-size:20px;font-weight:600;color:#303133;margin-top:6px; }
</style>
