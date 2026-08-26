<template>
  <div class="app-container">
    <!-- 分析查询条件 -->
    <el-form :inline="true" size="small" label-width="68px">
      <el-form-item label="学期ID"><el-input v-model="analysis.semesterId" placeholder="学期ID" clearable style="width:140px" @keyup.enter.native="loadAnalysis"/></el-form-item>
      <el-form-item label="课程ID"><el-input v-model="analysis.courseId" placeholder="课程ID（分布/排名必填）" clearable style="width:200px" @keyup.enter.native="loadAnalysis"/></el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-data-analysis" @click="loadAnalysis">统计分析</el-button>
        <el-button type="success" icon="el-icon-refresh" @click="aggregateSemester" v-hasPermi="['aem:gradeStatistics:edit']">刷新学期统计</el-button>
        <el-button type="warning" icon="el-icon-download" @click="handleExport" v-hasPermi="['aem:gradeStatistics:export']">导出</el-button>
      </el-form-item>
    </el-form>

    <!-- 总览卡片 -->
    <el-row :gutter="12" class="stat-cards" v-loading="overviewLoading">
      <el-col :xs="12" :sm="8" :md="4"><div class="stat-card"><div class="stat-label">课程数</div><div class="stat-value">{{ overview.courseCount || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="4"><div class="stat-card"><div class="stat-label">选课人次</div><div class="stat-value">{{ overview.totalRecords || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="4"><div class="stat-card"><div class="stat-label">参考学生</div><div class="stat-value">{{ overview.studentCount || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="4"><div class="stat-card"><div class="stat-label">平均分</div><div class="stat-value">{{ overview.avgScore || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="4"><div class="stat-card"><div class="stat-label">平均绩点</div><div class="stat-value">{{ overview.avgGpa || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="4"><div class="stat-card pass"><div class="stat-label">通过率</div><div class="stat-value">{{ overview.passRate || 0 }}%</div></div></el-col>
    </el-row>

    <!-- 分数段分布图 -->
    <el-row :gutter="12" style="margin-top:12px">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <div slot="header"><span>分数段分布</span></div>
          <div ref="distChart" style="height:300px" v-loading="chartLoading"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <div slot="header"><span>课程成绩排名</span></div>
          <el-table :data="rankingList" v-loading="rankLoading" size="small" height="300">
            <el-table-column label="排名" type="index" width="60" align="center"/>
            <el-table-column label="学号" prop="studentNo" width="110"/>
            <el-table-column label="姓名" prop="studentName" width="90"/>
            <el-table-column label="班级" prop="className" show-overflow-tooltip/>
            <el-table-column label="总分" prop="totalScore" width="70" align="center"/>
            <el-table-column label="绩点" prop="gradePoint" width="70" align="center"/>
            <el-table-column label="等级" prop="gradeLevel" width="70" align="center"/>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 历史统计快照 -->
    <el-card shadow="never" style="margin-top:12px">
      <div slot="header"><span>统计快照</span></div>
      <el-table v-loading="loading" :data="gradeStatisticsList" size="small">
        <el-table-column label="课程ID" align="center" prop="courseId" width="90"/>
        <el-table-column label="学期ID" align="center" prop="semesterId" width="90"/>
        <el-table-column label="总人数" align="center" prop="totalStudents" width="80"/>
        <el-table-column label="最高分" align="center" prop="maxScore" width="80"/>
        <el-table-column label="最低分" align="center" prop="minScore" width="80"/>
        <el-table-column label="平均分" align="center" prop="avgScore" width="80"/>
        <el-table-column label="通过/不及格" align="center" width="100">
          <template slot-scope="scope">{{ scope.row.passCount }} / {{ scope.row.failCount }}</template>
        </el-table-column>
        <el-table-column label="通过率(%)" align="center" prop="passRate" width="90"/>
        <el-table-column label="优秀率(%)" align="center" prop="excellentRate" width="90"/>
        <el-table-column label="统计时间" align="center" prop="statTime" width="160"/>
      </el-table>
      <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    </el-card>
  </div>
</template>
<script>
import * as echarts from 'echarts'
import { listGradeStatistics, aggregateGrade, aggregateSemester as aggSemester, scoreDistribution, semesterOverview, courseRanking } from "@/api/aem/gradeStatistics"
export default {
  name: "GradeStatistics",
  dicts: [],
  data() {
    return {
      loading: false, showSearch: true, total: 0, gradeStatisticsList: [],
      queryParams: { pageNum: 1, pageSize: 10, courseId: null, semesterId: null, classId: null },
      analysis: { semesterId: null, courseId: null },
      overview: {}, overviewLoading: false,
      chartLoading: false, rankLoading: false, rankingList: [],
      chart: null
    }
  },
  created() { this.getList() },
  mounted() { window.addEventListener('resize', this.resizeChart) },
  beforeDestroy() { window.removeEventListener('resize', this.resizeChart); if (this.chart) { this.chart.dispose(); this.chart = null } },
  methods: {
    getList() {
      this.loading = true
      listGradeStatistics(this.queryParams).then(response => {
        this.gradeStatisticsList = response.rows; this.total = response.total; this.loading = false
      })
    },
    loadAnalysis() {
      if (!this.analysis.semesterId) { this.$modal.msgWarning("请先输入学期ID"); return }
      this.loadOverview()
      if (this.analysis.courseId) { this.loadDistribution(); this.loadRanking() }
    },
    loadOverview() {
      this.overviewLoading = true
      semesterOverview(this.analysis.semesterId).then(res => { this.overview = res.data || {} }).finally(() => { this.overviewLoading = false })
    },
    loadDistribution() {
      this.chartLoading = true
      scoreDistribution(this.analysis.courseId, this.analysis.semesterId).then(res => {
        const d = res.data || {}
        this.renderChart([
          { name: '不及格(<60)', value: Number(d.failCount) || 0 },
          { name: '及格(60-69)', value: Number(d.passCount) || 0 },
          { name: '中等(70-79)', value: Number(d.mediumCount) || 0 },
          { name: '良好(80-89)', value: Number(d.goodCount) || 0 },
          { name: '优秀(>=90)', value: Number(d.excellentCount) || 0 }
        ])
      }).finally(() => { this.chartLoading = false })
    },
    renderChart(data) {
      this.$nextTick(() => {
        if (!this.$refs.distChart) return
        if (!this.chart) { this.chart = echarts.init(this.$refs.distChart) }
        this.chart.setOption({
          tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
          grid: { left: 40, right: 20, top: 30, bottom: 30 },
          xAxis: { type: 'category', data: data.map(i => i.name), axisLabel: { interval: 0, fontSize: 11 } },
          yAxis: { type: 'value', minInterval: 1 },
          series: [{
            type: 'bar', barWidth: '50%', data: data.map(i => i.value),
            itemStyle: {
              color: (p) => ['#f56c6c', '#e6a23c', '#909399', '#409eff', '#67c23a'][p.dataIndex]
            },
            label: { show: true, position: 'top' }
          }]
        }, true)
      })
    },
    resizeChart() { this.chart && this.chart.resize() },
    loadRanking() {
      this.rankLoading = true
      courseRanking({ courseId: this.analysis.courseId, semesterId: this.analysis.semesterId, pageNum: 1, pageSize: 50 })
        .then(res => { this.rankingList = res.rows || [] }).finally(() => { this.rankLoading = false })
    },
    aggregateSemester() {
      if (!this.analysis.semesterId) { this.$modal.msgWarning("请先输入学期ID"); return }
      this.$modal.confirm('确认对学期[' + this.analysis.semesterId + ']所有课程重新聚合统计？').then(() => {
        return aggSemester(this.analysis.semesterId)
      }).then(res => {
        this.$modal.msgSuccess(res.msg || "聚合完成")
        this.getList(); this.loadOverview()
      }).catch(() => {})
    },
    handleExport() { this.download('aem/gradeStatistics/export', { ...this.queryParams }, `gradeStatistics_${new Date().getTime()}.xlsx`) }
  }
}
</script>
<style scoped>
.stat-cards .stat-card { background:#f5f7fa;border-radius:6px;padding:14px;text-align:center;margin-bottom:12px; }
.stat-cards .stat-card.pass { background:#f0f9eb; }
.stat-label { color:#909399;font-size:13px; }
.stat-value { font-size:22px;font-weight:600;color:#303133;margin-top:6px; }
</style>
