<template>
  <div class="page-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span><el-icon><MagicStick /></el-icon> 个性化选课推荐</span>
          <el-button link type="primary" size="small" icon="Refresh" @click="load">刷新推荐</el-button>
        </div>
      </template>

      <el-alert v-if="!data.available" :title="data.note || '暂无可推荐内容'" type="warning" :closable="false" show-icon />

      <template v-else>
        <!-- 学生与轮次概览 -->
        <div class="info-line">
          <span>{{ student.studentName }}（{{ student.studentNo }}）</span>
          <span class="sep">|</span>
          <span>{{ student.deptName }} / {{ student.majorName }} / {{ student.className }}</span>
          <span class="sep">|</span>
          <span>{{ round.roundName }}</span>
        </div>

        <el-row :gutter="12" class="summary-row">
          <el-col :xs="12" :sm="6"><div class="stat-box"><div class="stat-label">已选门数</div><div class="stat-value">{{ quota.selectedCourses || 0 }}<span class="stat-sub">/{{ quota.maxCourses || '∞' }}</span></div></div></el-col>
          <el-col :xs="12" :sm="6"><div class="stat-box"><div class="stat-label">已选学分</div><div class="stat-value">{{ num(quota.selectedCredit) }}<span class="stat-sub">/{{ num(quota.maxCredits) }}</span></div></div></el-col>
          <el-col :xs="12" :sm="6"><div class="stat-box"><div class="stat-label">剩余可选门数</div><div class="stat-value ok">{{ quota.remainingCourses === null || quota.remainingCourses === undefined ? '不限' : quota.remainingCourses }}</div></div></el-col>
          <el-col :xs="12" :sm="6"><div class="stat-box"><div class="stat-label">剩余学分额度</div><div class="stat-value ok">{{ num(quota.remainingCredit) }}</div></div></el-col>
        </el-row>

        <el-alert v-if="data.note" :title="data.note" type="info" :closable="false" show-icon style="margin-bottom:12px" />

        <!-- 推荐清单 -->
        <h4 class="sub-title">推荐课程（按匹配度排序）</h4>
        <el-table :data="data.items || []" border stripe size="small" row-key="offeringId">
          <el-table-column type="expand">
            <template #default="scope">
              <div class="signal-box">
                <div class="signal-title">匹配度构成（加权后满分 100）</div>
                <el-table :data="scope.row.signals || []" size="small" :show-header="false">
                  <el-table-column prop="name" width="120" />
                  <el-table-column width="200">
                    <template #default="s">
                      <el-progress :percentage="signalPercent(s.row.value)" :stroke-width="8"
                                   :color="signalColor(s.row.value)" />
                      <span class="signal-weight">权重 {{ Math.round((s.row.weight || 0) * 100) }}%</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="detail" show-overflow-tooltip />
                </el-table>
                <div v-if="!(scope.row.signals || []).length" class="signal-empty">仅按开课与冲突约束入围，无可用的个性化信号</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="排名" prop="rank" width="60" align="center" />
          <el-table-column label="课程名称" min-width="160" show-overflow-tooltip>
            <template #default="scope">
              <span>{{ scope.row.courseName }}</span>
              <el-tag v-if="scope.row.courseCategory" size="small" type="info" style="margin-left:4px">{{ categoryName(scope.row.courseCategory) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="授课教师" prop="teacherName" width="90" align="center" />
          <el-table-column label="学分" prop="credit" width="60" align="center" />
          <el-table-column label="容量" width="100" align="center">
            <template #default="scope">{{ num(scope.row.enrolledCount) }}/{{ scope.row.maxStudents || '—' }}</template>
          </el-table-column>
          <el-table-column label="匹配度" width="120" align="center">
            <template #default="scope">
              <el-tag :type="scoreTag(scope.row.score)" size="small">{{ scope.row.score === null || scope.row.score === undefined ? '—' : scope.row.score }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="推荐理由" min-width="200">
            <template #default="scope">
              <div v-for="(r, i) in scope.row.reasons || []" :key="i" class="reason-line">· {{ r }}</div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center" fixed="right">
            <template #default="scope">
              <el-button type="primary" size="small" :loading="enrollingId === scope.row.offeringId" @click="handleEnroll(scope.row)">选课</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!(data.items || []).length && !loading" class="empty-tip">本轮暂无新的推荐课程</div>

        <!-- 未推荐原因 -->
        <template v-if="(data.excluded || []).length">
          <h4 class="sub-title">未推荐原因（已按选课硬性约束过滤）</h4>
          <el-table :data="data.excluded" border size="small">
            <el-table-column label="课程名称" prop="courseName" min-width="160" show-overflow-tooltip />
            <el-table-column label="授课教师" prop="teacherName" width="90" align="center" />
            <el-table-column label="被过滤原因" min-width="260">
              <template #default="scope">
                <div v-for="(r, i) in scope.row.reasons || []" :key="i" class="reason-line warn">· {{ r }}</div>
              </template>
            </el-table-column>
          </el-table>
        </template>

        <!-- 本人已选 -->
        <template v-if="(data.selected || []).length">
          <h4 class="sub-title">本轮已选课程</h4>
          <el-table :data="data.selected" border size="small">
            <el-table-column label="课程名称" prop="courseName" min-width="160" show-overflow-tooltip />
            <el-table-column label="授课教师" prop="teacherName" width="100" align="center" />
            <el-table-column label="学分" prop="credit" width="80" align="center" />
          </el-table>
        </template>

        <div class="disclaimer" v-if="data.signalNote">
          <el-icon><InfoFilled /></el-icon> 评分信号：{{ data.signalNote }}
        </div>
        <div class="disclaimer">
          <el-icon><InfoFilled /></el-icon> 推荐结果仅依据培养方案、成绩、评教与容量等校内实数计算，不构成选课指令；正式选课仍以选课中心提交结果为准。
        </div>
      </template>
    </el-card>
  </div>
</template>

<script>
import { recommendCourses } from '@/api/portal/ai'
import { enrollCourse } from '@/api/portal/selection'

const CATEGORY_NAME = {
  PUBLIC_BASE: '公共基础课',
  MAJOR: '专业课',
  FOREIGN_LANGUAGE: '外语课',
  PE: '体育课',
  PRACTICE: '实践环节',
  QUALITY: '素质教育',
  UNCATEGORIZED: '未分类',
  REQUIRED: '必修课',
  ELECTIVE: '选修课',
  OPTIONAL: '任选课',
  PUBLIC: '公共课',
  PROFESSIONAL: '专业课'
}

export default {
  name: 'PortalAiRecommend',
  data() {
    return {
      loading: false,
      enrollingId: null,
      data: { available: false, items: [], excluded: [], selected: [] }
    }
  },
  computed: {
    student() { return this.data.student || {} },
    round() { return this.data.round || {} },
    quota() { return this.data.quota || {} }
  },
  created() { this.load() },
  methods: {
    load() {
      this.loading = true
      recommendCourses().then(r => {
        this.data = r.data || { available: false }
      }).finally(() => { this.loading = false })
    },
    handleEnroll(row) {
      this.$confirm('确认选择课程「' + row.courseName + '」？推荐结果不自动占位，需以选课中心提交结果为准。', '选课确认', { type: 'warning' }).then(() => {
        this.enrollingId = row.offeringId
        enrollCourse({ courseOfferingId: row.offeringId, roundId: this.round.roundId }).then(() => {
          this.$message.success('选课成功，已按最新情况重算推荐')
          this.load()
        }).catch(e => {
          this.$message.error((e && e.msg) || '选课失败，请前往选课中心处理')
        }).finally(() => { this.enrollingId = null })
      }).catch(() => {})
    },
    categoryName(code) { return CATEGORY_NAME[code] || code },
    scoreTag(score) {
      const v = Number(score) || 0
      return v >= 80 ? 'success' : v >= 60 ? '' : v >= 40 ? 'warning' : 'info'
    },
    // 后端已将信号原始比例归一为 0~100 的百分数，此处直接作为进度条百分比
    signalPercent(value) {
      const v = Math.round(Number(value) || 0)
      return v < 0 ? 0 : v > 100 ? 100 : v
    },
    signalColor(value) {
      const v = Number(value) || 0
      return v >= 80 ? '#67c23a' : v >= 50 ? '#409eff' : v >= 30 ? '#e6a23c' : '#c0c4cc'
    },
    num(v) { return (v === null || v === undefined) ? 0 : v }
  }
}
</script>

<style scoped>
.page-container { max-width: 1200px; margin: 0 auto; }
.card-header { font-size: 16px; font-weight: 600; display: flex; justify-content: space-between; align-items: center; }
.info-line { font-size: 13px; color: #606266; margin-bottom: 12px; }
.info-line .sep { margin: 0 8px; color: #dcdfe6; }
.summary-row { margin-bottom: 14px; }
.summary-row .stat-box { background: #f5f7fa; border-radius: 6px; padding: 12px; text-align: center; margin-bottom: 8px; }
.stat-label { font-size: 13px; color: #909399; margin-bottom: 6px; }
.stat-value { font-size: 20px; font-weight: 600; color: #303133; }
.stat-value.ok { color: #67c23a; }
.stat-sub { font-size: 12px; font-weight: 400; color: #909399; margin-left: 2px; }
.sub-title { font-size: 14px; font-weight: 600; margin: 20px 0 10px; color: #303133; }
.reason-line { font-size: 12px; line-height: 20px; color: #606266; }
.reason-line.warn { color: #e6a23c; }
.signal-box { padding: 6px 20px 10px; }
.signal-title { font-size: 12px; color: #909399; margin-bottom: 6px; }
.signal-weight { font-size: 12px; color: #909399; margin-left: 8px; }
.signal-empty { font-size: 12px; color: #c0c4cc; }
.empty-tip { padding: 16px 0; text-align: center; font-size: 13px; color: #909399; }
.disclaimer { margin-top: 14px; font-size: 12px; color: #909399; line-height: 1.8; }

@media (max-width: 768px) {
  .page-container { padding: 0; }
  :deep(.el-card__body) { padding: 10px; }
  :deep(.el-table) { overflow-x: auto; }
}
</style>
