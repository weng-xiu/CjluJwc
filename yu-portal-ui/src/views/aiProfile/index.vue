<template>
  <div class="page-container">
    <el-card v-loading="loading">
      <div slot="header" class="card-header">
        <i class="el-icon-data-analysis"></i> 学业画像
        <el-button type="text" size="mini" icon="el-icon-refresh" style="float:right" @click="load">重新生成</el-button>
      </div>

      <el-alert v-if="!data.available" :title="data.note || '暂无法生成画像'" type="warning" :closable="false" show-icon />

      <template v-else>
        <div class="info-line">
          <span>{{ student.studentName }}（{{ student.studentNo }}）</span>
          <span class="sep">|</span>
          <span>{{ student.deptName }} / {{ student.majorName }} / {{ student.className }}</span>
          <span class="sep">|</span>
          <span>{{ student.enrollmentYear }} 级 {{ student.educationLevel || '' }}</span>
          <span class="sep">|</span>
          <span>{{ plan.planName || '未匹配到已发布培养方案' }}</span>
        </div>

        <el-row :gutter="16">
          <!-- 雷达图 -->
          <el-col :xs="24" :md="11">
            <div class="radar-wrap">
              <svg viewBox="0 0 340 300" class="radar">
                <polygon v-for="(ring, i) in rings" :key="'r' + i" :points="ring" fill="none" stroke="#ebeef5" />
                <line v-for="(axis, i) in axes" :key="'a' + i" :x1="cx" :y1="cy" :x2="axis.x" :y2="axis.y" stroke="#ebeef5" />
                <polygon :points="valuePolygon" fill="rgba(64,158,255,0.25)" stroke="#409eff" stroke-width="1.5" />
                <circle v-for="(axis, i) in axes" :key="'d' + i" :cx="axis.vx" :cy="axis.vy" r="3" fill="#409eff" />
                <text v-for="(axis, i) in axes" :key="'t' + i" :x="axis.lx" :y="axis.ly" class="radar-label"
                      :text-anchor="anchor(axis)">{{ axis.name }}</text>
              </svg>
              <div class="overall">综合达成度 <span class="overall-value">{{ data.overallScore === null || data.overallScore === undefined ? '—' : data.overallScore }}</span> / 100</div>
            </div>
          </el-col>

          <!-- 维度明细 -->
          <el-col :xs="24" :md="13">
            <div v-for="d in data.dims || []" :key="d.code" class="dim-row">
              <div class="dim-head">
                <span class="dim-name">{{ d.name }}</span>
                <span class="dim-value" :class="dimClass(d.value)">{{ d.value === null || d.value === undefined ? '无数据' : d.value }}</span>
              </div>
              <el-progress :percentage="Math.min(Number(d.value) || 0, 100)" :show-text="false" :stroke-width="8" :color="dimColor(d.value)" />
              <div class="dim-text">{{ d.text }}</div>
            </div>
          </el-col>
        </el-row>

        <!-- 关键指标 -->
        <h4 class="sub-title">关键指标</h4>
        <el-row :gutter="12" class="summary-row">
          <el-col :xs="12" :sm="8" :md="4"><div class="stat-box"><div class="stat-label">已修课程</div><div class="stat-value">{{ gradeSummary.courseCount || 0 }}</div></div></el-col>
          <el-col :xs="12" :sm="8" :md="4"><div class="stat-box"><div class="stat-label">加权均分</div><div class="stat-value">{{ num(gradeSummary.avgScore) }}</div></div></el-col>
          <el-col :xs="12" :sm="8" :md="4"><div class="stat-box"><div class="stat-label">GPA</div><div class="stat-value">{{ num(gradeSummary.gpa) }}</div></div></el-col>
          <el-col :xs="12" :sm="8" :md="4"><div class="stat-box"><div class="stat-label">已获学分</div><div class="stat-value">{{ num(gradeSummary.earnedCredit) }}</div></div></el-col>
          <el-col :xs="12" :sm="8" :md="4"><div class="stat-box"><div class="stat-label">不及格门数</div><div class="stat-value warn">{{ gradeSummary.failCount || 0 }}</div></div></el-col>
          <el-col :xs="12" :sm="8" :md="4"><div class="stat-box"><div class="stat-label">同类均分</div><div class="stat-value">{{ num(peerSummary.peerAvgScore) }}<span class="stat-sub">({{ peerSummary.peerCount || 0 }}人)</span></div></div></el-col>
        </el-row>

        <!-- 模块达成 -->
        <h4 class="sub-title">培养方案模块达成</h4>
        <el-table v-if="(data.modules || []).length" :data="data.modules" border stripe size="small">
          <el-table-column label="学分模块" prop="creditTypeName" min-width="140" show-overflow-tooltip />
          <el-table-column label="应修" prop="requiredCredit" width="80" align="center" />
          <el-table-column label="已获" prop="earnedCredit" width="80" align="center" />
          <el-table-column label="达成率" min-width="160">
            <template slot-scope="scope">
              <el-progress :percentage="Math.min(Number(scope.row.rate) || 0, 100)" :stroke-width="10"
                           :status="scope.row.rate >= 100 ? 'success' : undefined" />
            </template>
          </el-table-column>
          <el-table-column label="方案课程数" prop="courseCount" width="100" align="center" />
        </el-table>
        <div v-else class="empty-tip">未找到已发布培养方案的学分模块要求，无法评价模块达成</div>

        <!-- 分类成绩 -->
        <template v-if="(data.categoryScores || []).length">
          <h4 class="sub-title">各类课程成绩表现</h4>
          <el-table :data="data.categoryScores" border size="small">
            <el-table-column label="课程类别" min-width="140">
              <template slot-scope="scope">{{ categoryName(scope.row.creditType) }}</template>
            </el-table-column>
            <el-table-column label="课程门数" prop="courseCount" width="100" align="center" />
            <el-table-column label="平均分" prop="avgScore" width="100" align="center" />
            <el-table-column label="修读学分" prop="totalCredit" width="100" align="center" />
            <el-table-column label="获得学分" prop="earnedCredit" width="100" align="center" />
          </el-table>
        </template>

        <!-- 预警 -->
        <h4 class="sub-title">学业预警记录</h4>
        <el-table v-if="(data.warnings || []).length" :data="data.warnings" border size="small">
          <el-table-column label="预警类型" prop="warningType" width="140" :formatter="warningTypeText" />
          <el-table-column label="等级" width="80" align="center">
            <template slot-scope="scope">
              <el-tag :type="Number(scope.row.warningLevel) >= 2 ? 'danger' : 'warning'" size="mini">{{ scope.row.warningLevel }} 级</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="原因" prop="warningReason" min-width="220" show-overflow-tooltip />
          <el-table-column label="学期" prop="semesterName" width="130" align="center" />
          <el-table-column label="状态" width="90" align="center">
            <template slot-scope="scope">
              <el-tag :type="scope.row.isResolved === '1' ? 'success' : 'danger'" size="mini">{{ scope.row.isResolved === '1' ? '已解除' : '未解除' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-alert v-else type="success" :closable="false" show-icon title="无学业预警记录" />

        <!-- 最近成绩 -->
        <template v-if="(data.grades || []).length">
          <h4 class="sub-title">最近成绩明细</h4>
          <el-table :data="data.grades" border size="small" max-height="320">
            <el-table-column label="课程" prop="courseName" min-width="160" show-overflow-tooltip />
            <el-table-column label="学期" prop="semesterName" width="130" align="center" />
            <el-table-column label="学分" prop="credit" width="70" align="center" />
            <el-table-column label="总评" prop="totalScore" width="80" align="center" />
            <el-table-column label="绩点" prop="gradePoint" width="80" align="center" />
            <el-table-column label="等级" prop="gradeLevel" width="80" align="center" />
            <el-table-column label="是否通过" width="90" align="center">
              <template slot-scope="scope">
                <el-tag :type="scope.row.isPass === '1' ? 'success' : 'danger'" size="mini">{{ scope.row.isPass === '1' ? '通过' : '未通过' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </template>

        <!-- 建议 -->
        <h4 class="sub-title">针对性建议</h4>
        <ul class="tip-list">
          <li v-for="(tip, i) in data.suggestions || []" :key="i">{{ tip }}</li>
        </ul>

        <div class="disclaimer" v-if="data.note"><i class="el-icon-info"></i> {{ data.note }}</div>
        <div class="disclaimer"><i class="el-icon-info"></i> 画像由本人成绩、培养方案、选课与预警数据实时计算，仅供学业自查参考，不作为毕业与学位授予依据。</div>
      </template>
    </el-card>
  </div>
</template>

<script>
import { getPortrait } from '@/api/portal/ai'

const CATEGORY_NAME = {
  PUBLIC_BASE: '公共基础课',
  MAJOR: '专业课',
  FOREIGN_LANGUAGE: '外语课',
  PE: '体育课',
  REQUIRED: '必修课',
  ELECTIVE: '选修课',
  OPTIONAL: '任选课',
  PUBLIC: '公共课',
  PROFESSIONAL: '专业课',
  PRACTICE: '实践环节',
  QUALITY: '素质教育',
  UNCATEGORIZED: '未分类'
}
const WARNING_TYPE = {
  '0': '成绩预警',
  '1': '学分预警',
  '2': '旷课预警',
  '3': '其他预警'
}

export default {
  name: 'PortalAiPortrait',
  data() {
    return {
      loading: false,
      cx: 170,
      cy: 148,
      radius: 92,
      data: { available: false, dims: [] }
    }
  },
  computed: {
    student() { return this.data.student || {} },
    plan() { return this.data.plan || {} },
    gradeSummary() { return this.data.gradeSummary || {} },
    peerSummary() { return this.data.peerSummary || {} },
    dimList() {
      const dims = this.data.dims || []
      return dims.map(d => ({ name: d.name, value: Number(d.value) || 0 }))
    },
    points() {
      const n = this.dimList.length || 1
      return this.dimList.map((d, i) => {
        const angle = -Math.PI / 2 + (i * 2 * Math.PI) / n
        return {
          name: d.name,
          ratio: Math.max(Math.min(d.value, 100), 0) / 100,
          angle: angle,
          x: this.cx + this.radius * Math.cos(angle),
          y: this.cy + this.radius * Math.sin(angle)
        }
      })
    },
    axes() {
      return this.points.map(p => {
        const labelR = this.radius + 24
        return {
          name: p.name,
          x: p.x.toFixed(1),
          y: p.y.toFixed(1),
          vx: (this.cx + this.radius * p.ratio * Math.cos(p.angle)).toFixed(1),
          vy: (this.cy + this.radius * p.ratio * Math.sin(p.angle)).toFixed(1),
          lx: (this.cx + labelR * Math.cos(p.angle)).toFixed(1),
          ly: (this.cy + labelR * Math.sin(p.angle) + 4).toFixed(1),
          cos: Math.cos(p.angle)
        }
      })
    },
    rings() {
      return [0.25, 0.5, 0.75, 1].map(r => this.polygonOf(r))
    },
    valuePolygon() {
      return this.points.map(p =>
        (this.cx + this.radius * p.ratio * Math.cos(p.angle)).toFixed(1) + ',' +
        (this.cy + this.radius * p.ratio * Math.sin(p.angle)).toFixed(1)
      ).join(' ')
    }
  },
  created() { this.load() },
  methods: {
    load() {
      this.loading = true
      getPortrait().then(r => {
        this.data = r.data || { available: false }
      }).finally(() => { this.loading = false })
    },
    polygonOf(ratio) {
      const n = this.dimList.length || 1
      let pts = ''
      for (let i = 0; i < n; i++) {
        const angle = -Math.PI / 2 + (i * 2 * Math.PI) / n
        pts += (this.cx + this.radius * ratio * Math.cos(angle)).toFixed(1) + ',' +
               (this.cy + this.radius * ratio * Math.sin(angle)).toFixed(1) + ' '
      }
      return pts.trim()
    },
    anchor(axis) {
      if (Math.abs(axis.cos) < 0.35) return 'middle'
      return axis.cos > 0 ? 'start' : 'end'
    },
    dimClass(value) {
      if (value === null || value === undefined) return 'nodata'
      const v = Number(value)
      return v >= 80 ? 'good' : v >= 60 ? 'mid' : 'low'
    },
    dimColor(value) {
      const v = Number(value) || 0
      return v >= 80 ? '#67c23a' : v >= 60 ? '#409eff' : v >= 40 ? '#e6a23c' : '#f56c6c'
    },
    categoryName(code) { return CATEGORY_NAME[code] || code },
    warningTypeText(row) {
      const t = row.warningType
      return WARNING_TYPE[t] || t
    },
    num(v) { return (v === null || v === undefined) ? 0 : v }
  }
}
</script>

<style scoped>
.page-container { max-width: 1200px; margin: 0 auto; }
.card-header { font-size: 16px; font-weight: 600; }
.info-line { font-size: 13px; color: #606266; margin-bottom: 14px; }
.info-line .sep { margin: 0 8px; color: #dcdfe6; }
.radar-wrap { text-align: center; }
.radar { width: 100%; max-width: 360px; height: auto; }
.radar-label { font-size: 12px; fill: #606266; }
.overall { font-size: 13px; color: #909399; margin-top: 4px; }
.overall-value { font-size: 20px; font-weight: 600; color: #409eff; }
.dim-row { margin-bottom: 12px; }
.dim-head { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 3px; }
.dim-name { color: #303133; font-weight: 600; }
.dim-value { color: #909399; }
.dim-value.good { color: #67c23a; }
.dim-value.mid { color: #409eff; }
.dim-value.low { color: #f56c6c; }
.dim-value.nodata { color: #c0c4cc; }
.dim-text { font-size: 12px; color: #909399; line-height: 1.6; margin-top: 2px; }
.sub-title { font-size: 14px; font-weight: 600; margin: 20px 0 10px; color: #303133; }
.summary-row { margin-bottom: 6px; }
.summary-row .stat-box { background: #f5f7fa; border-radius: 6px; padding: 12px; text-align: center; margin-bottom: 8px; }
.stat-label { font-size: 13px; color: #909399; margin-bottom: 6px; }
.stat-value { font-size: 20px; font-weight: 600; color: #303133; }
.stat-value.warn { color: #e6a23c; }
.stat-sub { font-size: 12px; font-weight: 400; color: #909399; margin-left: 2px; }
.empty-tip { padding: 12px 0; font-size: 13px; color: #909399; text-align: center; background: #fafafa; border-radius: 4px; }
.tip-list { margin: 0; padding-left: 20px; line-height: 1.9; font-size: 13px; color: #303133; }
.disclaimer { margin-top: 14px; font-size: 12px; color: #909399; line-height: 1.8; }

@media (max-width: 768px) {
  .page-container { padding: 0; }
  ::v-deep .el-card__body { padding: 10px; }
  ::v-deep .el-table { overflow-x: auto; }
}
</style>
