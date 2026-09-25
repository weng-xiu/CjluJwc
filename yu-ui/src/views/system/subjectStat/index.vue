<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" type="border-card" @tab-click="onTabChange">
      <!-- ================= 学生结构 ================= -->
      <el-tab-pane label="学生结构分析" name="student">
        <el-row :gutter="12" class="stat-cards" v-loading="stLoading">
          <el-col :xs="12" :sm="8" :md="4"><div class="stat-card"><div class="stat-label">学籍总数</div><div class="stat-value">{{ st.overview.totalCount || 0 }}</div></div></el-col>
          <el-col :xs="12" :sm="8" :md="4"><div class="stat-card pass"><div class="stat-label">在读学生</div><div class="stat-value">{{ st.overview.enrolledCount || 0 }}</div></div></el-col>
          <el-col :xs="12" :sm="8" :md="4"><div class="stat-card"><div class="stat-label">休学</div><div class="stat-value">{{ st.overview.suspendCount || 0 }}</div></div></el-col>
          <el-col :xs="12" :sm="8" :md="4"><div class="stat-card warn"><div class="stat-label">退学</div><div class="stat-value">{{ st.overview.dropoutCount || 0 }}</div></div></el-col>
          <el-col :xs="12" :sm="8" :md="4"><div class="stat-card"><div class="stat-label">毕业</div><div class="stat-value">{{ st.overview.graduateCount || 0 }}</div></div></el-col>
          <el-col :xs="12" :sm="8" :md="4"><div class="stat-card"><div class="stat-label">转出/保留</div><div class="stat-value">{{ (st.overview.transferOutCount||0) + (st.overview.keepCount||0) }}</div></div></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :xs="24" :md="12"><el-card shadow="never"><div slot="header"><span>在读学生按院系分布</span></div><div ref="stDeptChart" class="chart" v-loading="stLoading"></div></el-card></el-col>
          <el-col :xs="24" :md="12"><el-card shadow="never"><div slot="header"><span>在读学生按入学年份分布</span></div><div ref="stYearChart" class="chart" v-loading="stLoading"></div></el-card></el-col>
        </el-row>
        <el-row :gutter="12" style="margin-top:12px">
          <el-col :xs="24" :md="12"><el-card shadow="never"><div slot="header"><span>在读学生性别构成</span></div><div ref="stGenderChart" class="chart" v-loading="stLoading"></div></el-card></el-col>
          <el-col :xs="24" :md="12"><el-card shadow="never"><div slot="header"><span>在读学生培养层次</span></div><div ref="stEduChart" class="chart" v-loading="stLoading"></div></el-card></el-col>
        </el-row>
      </el-tab-pane>

      <!-- ================= 成绩分析 ================= -->
      <el-tab-pane label="成绩分析" name="grade">
        <el-form :inline="true" size="small">
          <el-form-item label="学期ID"><el-input v-model="gradeQuery.semesterId" placeholder="可选，按学期统计" clearable style="width:200px" @keyup.enter.native="loadGrade"/></el-form-item>
          <el-form-item><el-button type="primary" icon="el-icon-data-analysis" @click="loadGrade">生成分析</el-button></el-form-item>
        </el-form>
        <el-row :gutter="12" class="stat-cards" v-loading="grLoading">
          <el-col :xs="12" :sm="8" :md="5"><div class="stat-card"><div class="stat-label">成绩记录数</div><div class="stat-value">{{ gr.overview.recordCount || 0 }}</div></div></el-col>
          <el-col :xs="12" :sm="8" :md="5"><div class="stat-card"><div class="stat-label">平均分</div><div class="stat-value">{{ gr.overview.avgScore || 0 }}</div></div></el-col>
          <el-col :xs="12" :sm="8" :md="5"><div class="stat-card"><div class="stat-label">平均绩点</div><div class="stat-value">{{ gr.overview.avgGpa || 0 }}</div></div></el-col>
          <el-col :xs="12" :sm="8" :md="4"><div class="stat-card pass"><div class="stat-label">及格率</div><div class="stat-value">{{ gr.overview.passRate || 0 }}%</div></div></el-col>
          <el-col :xs="12" :sm="8" :md="5"><div class="stat-card pass"><div class="stat-label">优秀率(≥85)</div><div class="stat-value">{{ gr.overview.excellentRate || 0 }}%</div></div></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :xs="24" :md="10"><el-card shadow="never"><div slot="header"><span>总评分数段分布</span></div><div ref="grBandChart" class="chart" v-loading="grLoading"></div></el-card></el-col>
          <el-col :xs="24" :md="14"><el-card shadow="never"><div slot="header"><span>学期成绩趋势</span></div><div ref="grTrendChart" class="chart" v-loading="grLoading"></div></el-card></el-col>
        </el-row>
        <el-card shadow="never" style="margin-top:12px"><div slot="header"><span>按院系成绩聚合</span></div>
          <el-table :data="gr.byDept" size="small" max-height="360" v-loading="grLoading">
            <el-table-column label="院系" prop="deptName" show-overflow-tooltip/>
            <el-table-column label="成绩记录数" prop="recordCount" width="120" align="center"/>
            <el-table-column label="平均分" prop="avgScore" width="100" align="center"/>
            <el-table-column label="及格率(%)" prop="passRate" width="120" align="center">
              <template slot-scope="scope"><el-tag :type="rateTag(scope.row.passRate)" size="mini">{{ scope.row.passRate }}%</el-tag></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ================= 师资分析 ================= -->
      <el-tab-pane label="师资分析" name="teacher">
        <el-form :inline="true" size="small">
          <el-form-item label="学期ID"><el-input v-model="teacherQuery.semesterId" placeholder="可选，按学期统计工作量" clearable style="width:200px" @keyup.enter.native="loadTeacher"/></el-form-item>
          <el-form-item><el-button type="primary" icon="el-icon-data-analysis" @click="loadTeacher">生成分析</el-button></el-form-item>
          <el-form-item>
            <el-button type="warning" icon="el-icon-download" @click="handleExport" v-hasPermi="['system:subjectStat:export']">导出师生数据上报</el-button>
          </el-form-item>
        </el-form>
        <el-row :gutter="12" class="stat-cards" v-loading="teLoading">
          <el-col :xs="12" :sm="8" :md="8"><div class="stat-card"><div class="stat-label">在职教师</div><div class="stat-value">{{ te.overview.teacherCount || 0 }}</div></div></el-col>
          <el-col :xs="12" :sm="8" :md="8"><div class="stat-card"><div class="stat-label">开课门次</div><div class="stat-value">{{ te.overview.offeringCount || 0 }}</div></div></el-col>
          <el-col :xs="12" :sm="8" :md="8"><div class="stat-card"><div class="stat-label">授课班级数</div><div class="stat-value">{{ te.overview.classCount || 0 }}</div></div></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :xs="24" :md="8"><el-card shadow="never"><div slot="header"><span>教师按院系分布</span></div><div ref="teDeptChart" class="chart" v-loading="teLoading"></div></el-card></el-col>
          <el-col :xs="24" :md="8"><el-card shadow="never"><div slot="header"><span>教师职称结构</span></div><div ref="teTitleChart" class="chart" v-loading="teLoading"></div></el-card></el-col>
          <el-col :xs="24" :md="8"><el-card shadow="never"><div slot="header"><span>教师学历结构</span></div><div ref="teEduChart" class="chart" v-loading="teLoading"></div></el-card></el-col>
        </el-row>
        <el-card shadow="never" style="margin-top:12px"><div slot="header"><span>教师授课工作量 TOP10</span></div>
          <el-table :data="te.workloadTop" size="small" max-height="360" v-loading="teLoading">
            <el-table-column label="排名" type="index" width="55" align="center"/>
            <el-table-column label="教师" prop="teacherName" width="140" show-overflow-tooltip/>
            <el-table-column label="院系" prop="deptName" show-overflow-tooltip/>
            <el-table-column label="开课门次" prop="offeringCount" width="100" align="center"/>
            <el-table-column label="班级数" prop="classCount" width="90" align="center"/>
            <el-table-column label="容量合计" prop="studentCapacity" width="100" align="center"/>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { studentStructure, gradeAnalysis, teacherStructure } from '@/api/system/subjectStat'

const PIE_COLORS = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#36cfc9', '#b37feb']

export default {
  name: 'SubjectStat',
  data() {
    return {
      activeTab: 'student',
      st: { overview: {} }, stLoading: false, stLoaded: false,
      gr: { overview: {}, byDept: [] }, grLoading: false, grLoaded: false,
      te: { overview: {}, workloadTop: [] }, teLoading: false, teLoaded: false,
      gradeQuery: { semesterId: null },
      teacherQuery: { semesterId: null },
      charts: {}
    }
  },
  mounted() {
    this.loadStudent()
    window.addEventListener('resize', this.resizeAll)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeAll)
    Object.values(this.charts).forEach(c => c && c.dispose())
  },
  methods: {
    onTabChange() {
      if (this.activeTab === 'student' && !this.stLoaded) this.loadStudent()
      else if (this.activeTab === 'grade' && !this.grLoaded) this.loadGrade()
      else if (this.activeTab === 'teacher' && !this.teLoaded) this.loadTeacher()
      this.$nextTick(this.resizeAll)
    },
    chart(refName) {
      const el = this.$refs[refName]
      if (!el) return null
      if (!this.charts[refName]) this.charts[refName] = echarts.init(el)
      return this.charts[refName]
    },
    resizeAll() { Object.values(this.charts).forEach(c => c && c.resize()) },
    rateTag(rate) {
      const r = Number(rate) || 0
      return r >= 90 ? 'success' : r >= 75 ? '' : r >= 60 ? 'warning' : 'danger'
    },
    pie(refName, data, title) {
      this.$nextTick(() => {
        const c = this.chart(refName)
        if (!c) return
        c.setOption({
          color: PIE_COLORS,
          tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
          legend: { bottom: 0, type: 'scroll' },
          series: [{ name: title, type: 'pie', radius: ['42%', '66%'], center: ['50%', '45%'],
            data: (data || []).map(i => ({ name: i.name, value: Number(i.value) || 0 })), label: { formatter: '{b}\n{c}' } }]
        }, true)
      })
    },
    bar(refName, data, color, horizontal) {
      this.$nextTick(() => {
        const c = this.chart(refName)
        if (!c) return
        const names = (data || []).map(i => i.name)
        const vals = (data || []).map(i => Number(i.value) || 0)
        const cat = { type: 'category', data: names, axisLabel: { interval: 0, rotate: horizontal ? 0 : 30 } }
        const val = { type: 'value', minInterval: 1 }
        c.setOption({
          tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
          grid: { left: 60, right: 20, top: 20, bottom: horizontal ? 30 : 60 },
          xAxis: horizontal ? val : cat,
          yAxis: horizontal ? cat : { type: 'category', data: names },
          series: [{ type: 'bar', data: vals, itemStyle: { color: color || '#409eff' }, barMaxWidth: 40,
            label: { show: true, position: horizontal ? 'right' : 'top' } }]
        }, true)
      })
    },
    // ===== 学生结构 =====
    loadStudent() {
      this.stLoading = true
      studentStructure().then(res => {
        const d = res.data || {}
        this.st = { overview: d.overview || {} }
        this.stLoaded = true
        this.bar('stDeptChart', d.byDept, '#409eff', false)
        this.bar('stYearChart', d.byEnrollmentYear, '#36cfc9', false)
        this.pie('stGenderChart', d.byGender, '性别')
        this.pie('stEduChart', d.byEducationLevel, '培养层次')
      }).finally(() => { this.stLoading = false })
    },
    // ===== 成绩分析 =====
    loadGrade() {
      this.grLoading = true
      gradeAnalysis(this.gradeQuery).then(res => {
        const d = res.data || {}
        this.gr = { overview: d.overview || {}, byDept: d.byDept || [] }
        this.grLoaded = true
        this.pie('grBandChart', d.scoreBand, '分数段')
        this.renderTrend(d.trend || [])
      }).finally(() => { this.grLoading = false })
    },
    renderTrend(trend) {
      this.$nextTick(() => {
        const c = this.chart('grTrendChart')
        if (!c) return
        const names = trend.map(i => i.name)
        const avg = trend.map(i => Number(i.avgScore) || 0)
        const pass = trend.map(i => Number(i.passRate) || 0)
        c.setOption({
          tooltip: { trigger: 'axis' }, legend: { data: ['平均分', '及格率'], bottom: 0 },
          grid: { left: 45, right: 45, top: 30, bottom: 45 },
          xAxis: { type: 'category', data: names },
          yAxis: [{ type: 'value', name: '平均分', min: 0, max: 100 }, { type: 'value', name: '及格率%', min: 0, max: 100 }],
          series: [
            { name: '平均分', type: 'line', smooth: true, data: avg, itemStyle: { color: '#409eff' } },
            { name: '及格率', type: 'bar', yAxisIndex: 1, data: pass, itemStyle: { color: '#67c23a' }, barWidth: '40%' }
          ]
        }, true)
      })
    },
    // ===== 师资分析 =====
    loadTeacher() {
      this.teLoading = true
      teacherStructure(this.teacherQuery).then(res => {
        const d = res.data || {}
        this.te = { overview: d.overview || {}, workloadTop: d.workloadTop || [] }
        this.teLoaded = true
        this.bar('teDeptChart', d.byDept, '#409eff', false)
        this.pie('teTitleChart', d.byTitle, '职称')
        this.pie('teEduChart', d.byEducation, '学历')
      }).finally(() => { this.teLoading = false })
    },
    handleExport() {
      this.download('system/subjectStat/export', { semesterId: this.teacherQuery.semesterId },
        `subject_report_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style scoped>
.chart { height: 300px; }
.stat-cards .stat-card { background:#f5f7fa;border-radius:6px;padding:14px;text-align:center;margin-bottom:12px; }
.stat-cards .stat-card.pass { background:#f0f9eb; }
.stat-cards .stat-card.warn { background:#fdf6ec; }
.stat-label { color:#909399;font-size:13px; }
.stat-value { font-size:22px;font-weight:600;color:#303133;margin-top:6px; }
</style>
