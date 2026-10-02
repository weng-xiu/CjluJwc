<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-click="onTabClick">
      <!-- 学生学业风险预测 -->
      <el-tab-pane label="学生风险预测" name="student">
        <el-form :inline="true" size="small">
          <el-form-item label="学生">
            <el-select
              v-model="studentQuery.studentId"
              filterable
              remote
              reserve-keyword
              placeholder="输入学号或姓名搜索"
              :remote-method="searchStudent"
              :loading="studentSearchLoading"
              style="width:260px">
              <el-option
                v-for="s in studentOptions"
                :key="s.studentId"
                :label="s.studentName + '（' + s.studentNo + '）'"
                :value="s.studentId"/>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-data-line" :disabled="!studentQuery.studentId" @click="loadStudentRisk">预测分析</el-button>
          </el-form-item>
        </el-form>

        <div v-if="risk" v-loading="studentLoading">
          <!-- 基本信息 + 风险等级 -->
          <el-row :gutter="12">
            <el-col :xs="24" :md="16">
              <el-card shadow="never">
                <div slot="header"><span>学生概况</span></div>
                <el-descriptions :column="2" size="small" border>
                  <el-descriptions-item label="学号">{{ (risk.base && risk.base.studentNo) || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="姓名">{{ (risk.base && risk.base.studentName) || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="班级">{{ (risk.base && risk.base.className) || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="专业">{{ (risk.base && risk.base.majorName) || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="统计学期数">{{ risk.semesterCount }}</el-descriptions-item>
                  <el-descriptions-item label="趋势斜率">{{ risk.trendSlope }}</el-descriptions-item>
                  <el-descriptions-item label="预测下学期均分">{{ risk.predictedNextScore }}</el-descriptions-item>
                  <el-descriptions-item label="预测下学期绩点">{{ risk.predictedNextGpa }}</el-descriptions-item>
                </el-descriptions>
              </el-card>
            </el-col>
            <el-col :xs="24" :md="8">
              <el-card shadow="never">
                <div slot="header"><span>风险分级</span></div>
                <div class="risk-box">
                  <el-tag :type="riskTagType(risk.riskLevel)" effect="dark" class="risk-tag">{{ risk.riskLabel }}</el-tag>
                  <div class="risk-tip">依据：最近学期表现 + 趋势斜率 + 累计不及格门数（阈值可配置）</div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <!-- 趋势图 + 判据因子 -->
          <el-row :gutter="12" style="margin-top:12px">
            <el-col :xs="24" :md="14">
              <el-card shadow="never">
                <div slot="header"><span>逐学期成绩/GPA 趋势</span></div>
                <div ref="trendChart" style="height:300px"></div>
              </el-card>
            </el-col>
            <el-col :xs="24" :md="10">
              <el-card shadow="never">
                <div slot="header"><span>判据因子</span></div>
                <el-table :data="risk.factors" size="small" border>
                  <el-table-column label="因子" prop="name" width="120"/>
                  <el-table-column label="取值" prop="value" width="80" align="center"/>
                  <el-table-column label="说明" prop="desc" show-overflow-tooltip/>
                </el-table>
              </el-card>
            </el-col>
          </el-row>

          <!-- 学期序列 + 建议 -->
          <el-row :gutter="12" style="margin-top:12px">
            <el-col :xs="24" :md="14">
              <el-card shadow="never">
                <div slot="header"><span>学期明细</span></div>
                <el-table :data="risk.series" size="small" max-height="280">
                  <el-table-column label="学期" prop="semesterName" show-overflow-tooltip/>
                  <el-table-column label="课程数" prop="courseCount" width="70" align="center"/>
                  <el-table-column label="平均分" prop="avgScore" width="80" align="center"/>
                  <el-table-column label="平均绩点" prop="avgGpa" width="80" align="center"/>
                  <el-table-column label="已获学分" prop="creditEarned" width="80" align="center"/>
                  <el-table-column label="不及格" prop="failCount" width="70" align="center"/>
                </el-table>
              </el-card>
            </el-col>
            <el-col :xs="24" :md="10">
              <el-card shadow="never">
                <div slot="header"><span>干预建议</span></div>
                <ul class="sug-list">
                  <li v-for="(s, i) in risk.suggestions" :key="i">{{ s }}</li>
                </ul>
              </el-card>
            </el-col>
          </el-row>
        </div>
        <el-empty v-else description="请选择学生并进行预测分析"/>
      </el-tab-pane>

      <!-- 课程难度画像 -->
      <el-tab-pane label="课程难度画像" name="course">
        <el-form :inline="true" size="small">
          <el-form-item label="学期">
            <el-select v-model="courseQuery.semesterId" clearable placeholder="全部学期" style="width:200px">
              <el-option v-for="s in semesterOptions" :key="s.semesterId" :label="s.semesterName" :value="s.semesterId"/>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="loadCourseDifficulty">查询</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="courseLoading" :data="courseList" size="small" border>
          <el-table-column label="课程编码" prop="courseCode" width="120"/>
          <el-table-column label="课程名称" prop="courseName" show-overflow-tooltip/>
          <el-table-column label="学分" prop="credit" width="70" align="center"/>
          <el-table-column label="修读人次" prop="takeCount" width="90" align="center"/>
          <el-table-column label="平均分" prop="avgScore" width="90" align="center"/>
          <el-table-column label="通过率(%)" prop="passRate" width="100" align="center"/>
          <el-table-column label="优秀率(%)" prop="excellentRate" width="100" align="center"/>
          <el-table-column label="不及格率(%)" prop="failRate" width="110" align="center"/>
          <el-table-column label="难度指数" prop="difficultyIndex" width="100" align="center"/>
          <el-table-column label="难度等级" width="90" align="center">
            <template slot-scope="scope">
              <el-tag :type="difficultyTagType(scope.row.difficultyLevel)" size="mini">{{ scope.row.difficultyLevel }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 学业风险看板 -->
      <el-tab-pane label="风险看板" name="board">
        <el-form :inline="true" size="small">
          <el-form-item label="学期">
            <el-select v-model="boardQuery.semesterId" placeholder="选择学期" style="width:200px">
              <el-option v-for="s in semesterOptions" :key="s.semesterId" :label="s.semesterName" :value="s.semesterId"/>
            </el-select>
          </el-form-item>
          <el-form-item label="班级">
            <el-select v-model="boardQuery.classId" clearable filterable placeholder="全部班级" style="width:220px">
              <el-option v-for="c in classOptions" :key="c.classId" :label="c.className" :value="c.classId"/>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-data-analysis" :disabled="!boardQuery.semesterId" @click="loadRiskBoard">生成看板</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="12" class="stat-cards" v-loading="boardLoading">
          <el-col :xs="12" :sm="6"><div class="stat-card danger"><div class="stat-label">高危</div><div class="stat-value">{{ board.distribution && board.distribution['高危'] || 0 }}</div></div></el-col>
          <el-col :xs="12" :sm="6"><div class="stat-card warn"><div class="stat-label">预警</div><div class="stat-value">{{ board.distribution && board.distribution['预警'] || 0 }}</div></div></el-col>
          <el-col :xs="12" :sm="6"><div class="stat-card watch"><div class="stat-label">关注</div><div class="stat-value">{{ board.distribution && board.distribution['关注'] || 0 }}</div></div></el-col>
          <el-col :xs="12" :sm="6"><div class="stat-card pass"><div class="stat-label">平稳</div><div class="stat-value">{{ board.distribution && board.distribution['平稳'] || 0 }}</div></div></el-col>
        </el-row>

        <el-table :data="board.list" v-loading="boardLoading" size="small" border style="margin-top:12px" max-height="480">
          <el-table-column label="学号" prop="studentNo" width="120"/>
          <el-table-column label="姓名" prop="studentName" width="100"/>
          <el-table-column label="班级" prop="className" show-overflow-tooltip/>
          <el-table-column label="课程数" prop="courseCount" width="80" align="center"/>
          <el-table-column label="平均分" prop="avgScore" width="90" align="center"/>
          <el-table-column label="平均绩点" prop="avgGpa" width="90" align="center"/>
          <el-table-column label="不及格" prop="failCount" width="80" align="center"/>
          <el-table-column label="风险等级" width="100" align="center">
            <template slot-scope="scope">
              <el-tag :type="riskTagType(scope.row.riskLevel)" size="mini">{{ scope.row.riskLabel }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div class="board-total" v-if="board.total">共 {{ board.total }} 名学生</div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { studentRisk, courseDifficulty, riskBoard } from '@/api/aem/gradePrediction'
import { listSemester } from '@/api/brm/semester'
import { listClazz } from '@/api/brm/clazz'
import { listStudent } from '@/api/sam/student'

export default {
  name: "GradePrediction",
  data() {
    return {
      activeTab: "student",
      // 学生风险
      studentQuery: { studentId: null },
      studentOptions: [],
      studentSearchLoading: false,
      studentLoading: false,
      risk: null,
      trendChart: null,
      // 课程难度
      semesterOptions: [],
      courseQuery: { semesterId: null },
      courseLoading: false,
      courseList: [],
      // 风险看板
      boardQuery: { semesterId: null, classId: null },
      classOptions: [],
      boardLoading: false,
      board: {}
    }
  },
  created() {
    this.loadSemesters()
  },
  beforeDestroy() {
    if (this.trendChart) { this.trendChart.dispose(); this.trendChart = null }
  },
  methods: {
    loadSemesters() {
      listSemester({ pageNum: 1, pageSize: 100 }).then(res => {
        this.semesterOptions = res.rows || []
      })
    },
    onTabClick(tab) {
      const name = typeof tab === 'string' ? tab : (tab && tab.name)
      if (name === 'course' && this.courseList.length === 0) { this.loadCourseDifficulty() }
      if (name === 'board' && this.classOptions.length === 0) { this.loadClasses() }
    },
    searchStudent(query) {
      if (!query) { this.studentOptions = []; return }
      this.studentSearchLoading = true
      listStudent({ studentNo: query, pageNum: 1, pageSize: 20 }).then(res => {
        this.studentOptions = res.rows || []
        this.studentSearchLoading = false
      }).catch(() => { this.studentSearchLoading = false })
    },
    loadStudentRisk() {
      if (!this.studentQuery.studentId) return
      this.studentLoading = true
      studentRisk(this.studentQuery.studentId).then(res => {
        this.risk = res.data || null
        this.renderTrendChart()
      }).finally(() => { this.studentLoading = false })
    },
    renderTrendChart() {
      this.$nextTick(() => {
        if (!this.$refs.trendChart) return
        if (!this.trendChart) { this.trendChart = echarts.init(this.$refs.trendChart) }
        const series = (this.risk && this.risk.series) || []
        this.trendChart.setOption({
          tooltip: { trigger: 'axis' },
          legend: { data: ['平均分', '平均绩点'] },
          grid: { left: 45, right: 45, top: 40, bottom: 30 },
          xAxis: { type: 'category', data: series.map(i => i.semesterName || ('学期' + i.semesterId)) },
          yAxis: [
            { type: 'value', name: '分数', min: 0, max: 100 },
            { type: 'value', name: '绩点', min: 0, max: 5 }
          ],
          series: [
            { name: '平均分', type: 'line', data: series.map(i => Number(i.avgScore) || 0), label: { show: true } },
            { name: '平均绩点', type: 'line', yAxisIndex: 1, data: series.map(i => Number(i.avgGpa) || 0) }
          ]
        }, true)
      })
    },
    loadCourseDifficulty() {
      this.courseLoading = true
      courseDifficulty(this.courseQuery.semesterId || undefined).then(res => {
        this.courseList = res.data || []
      }).finally(() => { this.courseLoading = false })
    },
    loadClasses() {
      listClazz({ pageNum: 1, pageSize: 500 }).then(res => {
        this.classOptions = res.rows || []
      })
    },
    loadRiskBoard() {
      if (!this.boardQuery.semesterId) { this.$modal.msgWarning("请选择学期"); return }
      this.boardLoading = true
      riskBoard(this.boardQuery.semesterId, this.boardQuery.classId || undefined).then(res => {
        this.board = res.data || {}
      }).finally(() => { this.boardLoading = false })
    },
    riskTagType(level) {
      return { '0': 'success', '1': 'info', '2': 'warning', '3': 'danger' }[level] || 'info'
    },
    difficultyTagType(level) {
      return { '高': 'danger', '中': 'warning', '低': 'success' }[level] || 'info'
    }
  }
}
</script>

<style scoped>
.risk-box { text-align:center;padding:10px 0; }
.risk-tag { font-size:20px;padding:14px 24px; }
.risk-tip { color:#909399;font-size:12px;margin-top:14px;line-height:1.6; }
.sug-list { margin:0;padding-left:20px;color:#606266;font-size:13px;line-height:1.9; }
.stat-cards .stat-card { background:#f5f7fa;border-radius:6px;padding:14px;text-align:center;margin-bottom:12px; }
.stat-cards .stat-card.danger { background:#fef0f0; }
.stat-cards .stat-card.warn { background:#fdf6ec; }
.stat-cards .stat-card.watch { background:#f4f4f5; }
.stat-cards .stat-card.pass { background:#f0f9eb; }
.stat-label { color:#909399;font-size:13px; }
.stat-value { font-size:22px;font-weight:600;color:#303133;margin-top:6px; }
.board-total { color:#909399;font-size:13px;margin-top:10px;text-align:right; }
</style>
