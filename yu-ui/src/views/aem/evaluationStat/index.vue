<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <el-form :inline="true" size="small">
      <el-form-item label="问卷ID"><el-input v-model="query.questionnaireId" placeholder="可选，按问卷过滤" clearable style="width:180px" @keyup.enter.native="loadAll"/></el-form-item>
      <el-form-item label="学期ID"><el-input v-model="query.semesterId" placeholder="可选，按学期过滤" clearable style="width:180px" @keyup.enter.native="loadAll"/></el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-data-analysis" @click="loadAll" v-hasPermi="['aem:evaluationStat:list']">统计分析</el-button>
      </el-form-item>
    </el-form>

    <!-- 总览卡片 -->
    <el-row :gutter="12" class="stat-cards" v-loading="overviewLoading">
      <el-col :xs="12" :sm="8" :md="4"><div class="stat-card"><div class="stat-label">评教人次</div><div class="stat-value">{{ overview.totalCount || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="4"><div class="stat-card"><div class="stat-label">覆盖教师</div><div class="stat-value">{{ overview.teacherCount || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="4"><div class="stat-card"><div class="stat-label">覆盖课程</div><div class="stat-value">{{ overview.courseCount || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="4"><div class="stat-card"><div class="stat-label">参评学生</div><div class="stat-value">{{ overview.studentCount || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="4"><div class="stat-card"><div class="stat-label">平均分</div><div class="stat-value">{{ overview.avgScore || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="4"><div class="stat-card pass"><div class="stat-label">满意度</div><div class="stat-value">{{ overview.satisfactionRate || 0 }}%</div></div></el-col>
    </el-row>

    <!-- 分布 + 趋势 -->
    <el-row :gutter="12" style="margin-top:8px">
      <el-col :xs="24" :md="10">
        <el-card shadow="never"><div slot="header"><span>评分分布</span></div><div ref="distChart" style="height:300px" v-loading="distLoading"></div></el-card>
      </el-col>
      <el-col :xs="24" :md="14">
        <el-card shadow="never"><div slot="header"><span>月度趋势</span></div><div ref="trendChart" style="height:300px" v-loading="trendLoading"></div></el-card>
      </el-col>
    </el-row>

    <!-- 课程 TOP + 教师排名 -->
    <el-row :gutter="12" style="margin-top:12px">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <div slot="header"><span>课程评教 TOP10（按平均分）</span></div>
          <el-table :data="courseList.slice(0,10)" v-loading="courseLoading" size="small" height="320">
            <el-table-column label="排名" type="index" width="55" align="center"/>
            <el-table-column label="课程" prop="courseName" show-overflow-tooltip/>
            <el-table-column label="人次" prop="totalCount" width="70" align="center"/>
            <el-table-column label="平均分" prop="avgScore" width="80" align="center"/>
            <el-table-column label="满意度%" prop="satisfactionRate" width="90" align="center"/>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <div slot="header"><span>教师评教排名 TOP10</span></div>
          <el-table :data="teacherList.slice(0,10)" v-loading="teacherLoading" size="small" height="320">
            <el-table-column label="名次" prop="rank" width="55" align="center"/>
            <el-table-column label="教师" prop="teacherName" show-overflow-tooltip/>
            <el-table-column label="课程数" prop="courseCount" width="70" align="center"/>
            <el-table-column label="人次" prop="totalCount" width="70" align="center"/>
            <el-table-column label="平均分" prop="avgScore" width="80" align="center"/>
            <el-table-column label="满意度%" prop="satisfactionRate" width="90" align="center"/>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 班级维度 + 评语词频 -->
    <el-row :gutter="12" style="margin-top:12px">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <div slot="header"><span>班级维度统计</span></div>
          <el-table :data="classList" v-loading="classLoading" size="small" height="320">
            <el-table-column label="班级" prop="className" show-overflow-tooltip/>
            <el-table-column label="人次" prop="totalCount" width="80" align="center"/>
            <el-table-column label="平均分" prop="avgScore" width="90" align="center"/>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <div slot="header"><span>评语关键词分析（共 {{ commentCount }} 条评语）</span></div>
          <div ref="wordChart" style="height:320px" v-loading="wordLoading"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { evalOverview, evalByCourse, evalByTeacher, evalByClass, evalTrend, evalCommentAnalysis } from "@/api/aem/evaluationStat"
export default {
  name: "EvaluationStat",
  data() {
    return {
      query: { questionnaireId: null, semesterId: null },
      overview: {}, overviewLoading: false, distLoading: false,
      courseList: [], courseLoading: false,
      teacherList: [], teacherLoading: false,
      classList: [], classLoading: false,
      trendList: [], trendLoading: false,
      commentCount: 0, wordLoading: false,
      distChart: null, trendChart: null, wordChart: null
    }
  },
  mounted() { window.addEventListener('resize', this.resizeCharts) },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCharts)
    ;[this.distChart, this.trendChart, this.wordChart].forEach(c => c && c.dispose())
  },
  methods: {
    loadAll() {
      this.loadOverview()
      this.loadCourse()
      this.loadTeacher()
      this.loadClass()
      this.loadTrend()
      this.loadComment()
    },
    loadOverview() {
      this.overviewLoading = true; this.distLoading = true
      evalOverview(this.query).then(res => {
        const d = res.data || {}
        this.overview = d.overview || {}
        this.renderDist(d.distribution || {})
      }).finally(() => { this.overviewLoading = false; this.distLoading = false })
    },
    renderDist(dist) {
      const data = [
        { name: '优秀(≥90)', value: Number(dist.excellentCount) || 0 },
        { name: '良好(80-89)', value: Number(dist.goodCount) || 0 },
        { name: '中等(70-79)', value: Number(dist.mediumCount) || 0 },
        { name: '及格(60-69)', value: Number(dist.passCount) || 0 },
        { name: '待改进(<60)', value: Number(dist.needsCount) || 0 }
      ]
      this.$nextTick(() => {
        if (!this.$refs.distChart) return
        if (!this.distChart) this.distChart = echarts.init(this.$refs.distChart)
        this.distChart.setOption({
          tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
          legend: { bottom: 0, type: 'scroll' },
          color: ['#67c23a', '#409eff', '#909399', '#e6a23c', '#f56c6c'],
          series: [{ type: 'pie', radius: ['40%', '65%'], center: ['50%', '45%'], data, label: { formatter: '{b}\n{c}' } }]
        }, true)
      })
    },
    loadCourse() {
      this.courseLoading = true
      evalByCourse(this.query).then(res => { this.courseList = res.data || [] }).finally(() => { this.courseLoading = false })
    },
    loadTeacher() {
      this.teacherLoading = true
      evalByTeacher(this.query).then(res => { this.teacherList = res.data || [] }).finally(() => { this.teacherLoading = false })
    },
    loadClass() {
      this.classLoading = true
      evalByClass(this.query).then(res => { this.classList = res.data || [] }).finally(() => { this.classLoading = false })
    },
    loadTrend() {
      this.trendLoading = true
      evalTrend(this.query).then(res => {
        this.trendList = res.data || []
        this.renderTrend()
      }).finally(() => { this.trendLoading = false })
    },
    renderTrend() {
      const months = this.trendList.map(i => i.month)
      const avgs = this.trendList.map(i => Number(i.avgScore) || 0)
      const counts = this.trendList.map(i => Number(i.totalCount) || 0)
      this.$nextTick(() => {
        if (!this.$refs.trendChart) return
        if (!this.trendChart) this.trendChart = echarts.init(this.$refs.trendChart)
        this.trendChart.setOption({
          tooltip: { trigger: 'axis' },
          legend: { data: ['平均分', '人次'], bottom: 0 },
          grid: { left: 40, right: 40, top: 30, bottom: 40 },
          xAxis: { type: 'category', data: months },
          yAxis: [{ type: 'value', name: '平均分', min: 0, max: 100 }, { type: 'value', name: '人次', minInterval: 1 }],
          series: [
            { name: '平均分', type: 'line', smooth: true, data: avgs, itemStyle: { color: '#409eff' } },
            { name: '人次', type: 'bar', yAxisIndex: 1, data: counts, itemStyle: { color: '#67c23a' }, barWidth: '40%' }
          ]
        }, true)
      })
    },
    loadComment() {
      this.wordLoading = true
      evalCommentAnalysis(this.query).then(res => {
        const d = res.data || {}
        this.commentCount = d.commentCount || 0
        this.renderWord(d.positive || [], d.improve || [])
      }).finally(() => { this.wordLoading = false })
    },
    renderWord(positive, improve) {
      const items = []
      positive.slice(0, 8).forEach(w => items.push({ name: w.word, value: w.count, cat: 0 }))
      improve.slice(0, 8).forEach(w => items.push({ name: w.word, value: w.count, cat: 1 }))
      this.$nextTick(() => {
        if (!this.$refs.wordChart) return
        if (!this.wordChart) this.wordChart = echarts.init(this.$refs.wordChart)
        this.wordChart.setOption({
          tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
          legend: { data: ['正面', '待改进'], bottom: 0 },
          grid: { left: 60, right: 20, top: 20, bottom: 40 },
          xAxis: { type: 'value', minInterval: 1 },
          yAxis: { type: 'category', data: items.map(i => i.name) },
          series: [{
            type: 'bar', data: items.map(i => ({ value: i.value, itemStyle: { color: i.cat === 0 ? '#67c23a' : '#e6a23c' } })),
            label: { show: true, position: 'right' }
          }]
        }, true)
      })
    },
    resizeCharts() { [this.distChart, this.trendChart, this.wordChart].forEach(c => c && c.resize()) }
  }
}
</script>

<style scoped>
.stat-cards .stat-card { background:#f5f7fa;border-radius:6px;padding:14px;text-align:center;margin-bottom:12px; }
.stat-cards .stat-card.pass { background:#f0f9eb; }
.stat-label { color:#909399;font-size:13px; }
.stat-value { font-size:22px;font-weight:600;color:#303133;margin-top:6px; }
</style>
