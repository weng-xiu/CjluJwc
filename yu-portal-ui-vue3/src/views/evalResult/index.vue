<template>
  <div class="page-container">
    <!-- 分析报告总览 -->
    <el-card class="mb16" v-loading="reportLoading">
      <template #header>
        <div class="card-header"><el-icon><DataLine /></el-icon> 我的评教分析报告</div>
      </template>
      <el-row :gutter="12" class="sum-row">
        <el-col :xs="12" :sm="6"><div class="sum-card"><div class="s-label">参评人次</div><div class="s-value">{{ summary.totalCount || 0 }}</div></div></el-col>
        <el-col :xs="12" :sm="6"><div class="sum-card"><div class="s-label">覆盖课程</div><div class="s-value">{{ summary.courseCount || 0 }}</div></div></el-col>
        <el-col :xs="12" :sm="6"><div class="sum-card pass"><div class="s-label">平均得分</div><div class="s-value">{{ summary.avgScore || 0 }}</div></div></el-col>
        <el-col :xs="12" :sm="6"><div class="sum-card pass"><div class="s-label">满意度</div><div class="s-value">{{ summary.satisfactionRate || 0 }}%</div></div></el-col>
      </el-row>
      <!-- 评分分布 -->
      <div class="block-title">评分分布</div>
      <div class="dist-list">
        <div v-for="d in distRows" :key="d.name" class="dist-item">
          <span class="d-name">{{ d.name }}</span>
          <el-progress :percentage="d.pct" :color="d.color" :stroke-width="14" style="flex:1" />
          <span class="d-count">{{ d.count }}人</span>
        </div>
        <div v-if="!distRows.length" class="empty-tip">暂无分布数据</div>
      </div>
      <!-- 评语关键词 -->
      <div class="block-title">评语关键词分析（共 {{ commentAnalysis.commentCount || 0 }} 条评语）</div>
      <div v-if="commentAnalysis.commentCount">
        <div class="word-group">
          <span class="w-tag-label">正面：</span>
          <el-tag v-for="w in commentAnalysis.positive" :key="'p'+w.word" type="success" size="small" class="w-tag">{{ w.word }} ×{{ w.count }}</el-tag>
          <span v-if="!commentAnalysis.positive || !commentAnalysis.positive.length" class="empty-inline">—</span>
        </div>
        <div class="word-group">
          <span class="w-tag-label">待改进：</span>
          <el-tag v-for="w in commentAnalysis.improve" :key="'i'+w.word" type="warning" size="small" class="w-tag">{{ w.word }} ×{{ w.count }}</el-tag>
          <span v-if="!commentAnalysis.improve || !commentAnalysis.improve.length" class="empty-inline">—</span>
        </div>
      </div>
      <el-empty v-else description="暂无评语数据" :image-size="60" />
    </el-card>

    <!-- 分课程结果 -->
    <el-card>
      <template #header>
        <div class="card-header"><el-icon><Notebook /></el-icon> 分课程评教结果</div>
      </template>
      <el-table v-loading="loading" :data="evalResults" border stripe>
        <el-table-column label="课程名称" prop="courseName" min-width="160" show-overflow-tooltip />
        <el-table-column label="学期" prop="semesterName" width="140" />
        <el-table-column label="参评人数" prop="totalCount" width="90" align="center" />
        <el-table-column label="平均分" prop="avgScore" width="90" align="center" />
        <el-table-column label="最高/最低" width="110" align="center">
          <template #default="scope">{{ scope.row.maxScore }} / {{ scope.row.minScore }}</template>
        </el-table-column>
        <el-table-column label="满意度" width="100" align="center">
          <template #default="scope"><el-tag :type="scope.row.satisfactionRate >= 85 ? 'success' : 'info'" size="small">{{ scope.row.satisfactionRate }}%</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="scope"><el-button type="primary" size="small" @click="viewDetail(scope.row)">查看评语</el-button></template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !evalResults.length" description="暂无评教数据" />
    </el-card>

    <el-dialog title="课程评教评语" v-model="detailVisible" width="700px">
      <div v-if="currentDetail">
        <el-alert :title="currentDetail.courseName + ' | 平均分 ' + currentDetail.avgScore + ' | 满意度 ' + currentDetail.satisfactionRate + '%'" type="info" :closable="false" style="margin-bottom:16px" />
        <div v-if="commentsLoading" style="text-align:center;color:#909399;padding:12px"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</div>
        <div v-else-if="currentDetail.comments && currentDetail.comments.length > 0">
          <div v-for="(c, idx) in currentDetail.comments" :key="idx" style="padding:8px;margin-bottom:4px;background:#f5f7fa;border-radius:4px">{{ idx + 1 }}. {{ c }}</div>
        </div>
        <el-empty v-else description="暂无评语" />
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getTeacherEvalResults, getCourseComments, getMyEvalReport } from '@/api/portal/evaluation'
export default {
  name: 'TeacherEvalResult',
  data() {
    return {
      loading: false, reportLoading: false,
      evalResults: [], summary: {}, distribution: {}, commentAnalysis: {},
      detailVisible: false, currentDetail: null, commentsLoading: false
    }
  },
  computed: {
    distRows() {
      const d = this.distribution || {}
      const total = ['excellentCount', 'goodCount', 'mediumCount', 'passCount', 'needsCount']
        .reduce((s, k) => s + (Number(d[k]) || 0), 0)
      const rows = [
        { name: '优秀(≥90)', count: Number(d.excellentCount) || 0, color: '#67c23a' },
        { name: '良好(80-89)', count: Number(d.goodCount) || 0, color: '#409eff' },
        { name: '中等(70-79)', count: Number(d.mediumCount) || 0, color: '#909399' },
        { name: '及格(60-69)', count: Number(d.passCount) || 0, color: '#e6a23c' },
        { name: '待改进(<60)', count: Number(d.needsCount) || 0, color: '#f56c6c' }
      ]
      rows.forEach(r => { r.pct = total > 0 ? Math.round(r.count * 100 / total) : 0 })
      return total > 0 ? rows : []
    }
  },
  created() { this.getList(); this.getReport() },
  methods: {
    getList() {
      this.loading = true
      getTeacherEvalResults().then(r => { this.evalResults = r.data || [] })
        .finally(() => { this.loading = false })
    },
    getReport() {
      this.reportLoading = true
      getMyEvalReport().then(r => {
        const d = r.data || {}
        this.summary = d.summary || {}
        this.distribution = d.distribution || {}
        this.commentAnalysis = d.commentAnalysis || {}
      }).finally(() => { this.reportLoading = false })
    },
    viewDetail(row) {
      this.currentDetail = { ...row, comments: [] }
      this.detailVisible = true
      this.commentsLoading = true
      getCourseComments(row.courseId).then(r => {
        // Vue3 移除 $set，currentDetail 已是响应式对象，直接赋值
        this.currentDetail.comments = r.data || []
      }).finally(() => { this.commentsLoading = false })
    }
  }
}
</script>

<style scoped>
.page-container { max-width: 1200px; margin: 0 auto; }
.card-header { font-size: 16px; font-weight: 600; }
.mb16 { margin-bottom: 16px; }
.sum-row .sum-card { background: #f5f7fa; border-radius: 6px; padding: 14px; text-align: center; margin-bottom: 8px; }
.sum-row .sum-card.pass { background: #f0f9eb; }
.s-label { color: #909399; font-size: 13px; }
.s-value { font-size: 22px; font-weight: 600; color: #303133; margin-top: 6px; }
.block-title { font-size: 14px; font-weight: 600; color: #303133; margin: 16px 0 8px; border-left: 3px solid #409eff; padding-left: 8px; }
.dist-list .dist-item { display: flex; align-items: center; margin-bottom: 8px; }
.dist-list .d-name { width: 100px; font-size: 13px; color: #606266; }
.dist-list .d-count { width: 60px; text-align: right; font-size: 13px; color: #909399; }
.word-group { margin-bottom: 8px; }
.w-tag-label { font-size: 13px; color: #606266; }
.w-tag { margin: 2px 4px 2px 0; }
.empty-inline { color: #c0c4cc; }
.empty-tip { color: #909399; font-size: 13px; padding: 8px 0; }
</style>
