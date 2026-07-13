<template>
  <div class="mobile-grades">
    <!-- 学期GPA顶部 -->
    <div class="gpa-banner">
      <div class="gpa-value">
        <span class="gpa-num">{{ semesterGpa || '--' }}</span>
        <span class="gpa-label">本学期 GPA</span>
      </div>
      <div class="gpa-stats">
        <div class="stat-item">
          <span class="stat-num">{{ totalCredit }}</span>
          <span class="stat-label">总学分</span>
        </div>
        <div class="stat-item">
          <span class="stat-num">{{ courseCount }}</span>
          <span class="stat-label">课程数</span>
        </div>
        <div class="stat-item">
          <span class="stat-num">{{ avgScore || '--' }}</span>
          <span class="stat-label">平均分</span>
        </div>
      </div>
    </div>

    <!-- 学期Tab切换 -->
    <div class="semester-tabs">
      <div
        v-for="sem in semesters"
        :key="sem"
        class="sem-tab"
        :class="{ active: currentSemester === sem }"
        @click="switchSemester(sem)"
      >{{ sem }}</div>
    </div>

    <!-- 成绩卡片列表 -->
    <div class="grade-list">
      <div v-if="loading" class="loading-state">
        <i class="el-icon-loading"></i> 加载中...
      </div>
      <div v-else-if="gradeList.length === 0" class="empty-state">
        <i class="el-icon-document"></i>
        <p>暂无成绩数据</p>
      </div>
      <div
        v-for="item in gradeList"
        :key="item.id"
        class="grade-card"
      >
        <div class="grade-top">
          <span class="grade-course">{{ item.courseName }}</span>
          <span class="grade-score" :class="getScoreClass(item.score)">{{ item.score || '--' }}</span>
        </div>
        <div class="grade-bottom">
          <span class="grade-info">
            <i class="el-icon-notebook-2"></i> {{ item.credit || 0 }} 学分
          </span>
          <span class="grade-info">
            <i class="el-icon-data-line"></i> 绩点 {{ item.gpa != null ? item.gpa : '--' }}
          </span>
          <span class="grade-level" :class="getLevelClass(item.gradeLevel)">
            {{ item.gradeLevel || '--' }}
          </span>
        </div>
      </div>
    </div>

    <!-- 加载更多 -->
    <div v-if="!loading && hasMore" class="load-more" @click="loadMore">
      点击加载更多
    </div>
  </div>
</template>

<script>
import { getMyGrades, getGradeStatistics } from '@/api/mobile'

export default {
  name: 'MobileGrades',
  data() {
    return {
      loading: false,
      gradeList: [],
      allGrades: [],
      semesters: [],
      currentSemester: '',
      semesterGpa: null,
      totalCredit: 0,
      courseCount: 0,
      avgScore: null,
      pageNum: 1,
      pageSize: 20,
      total: 0
    }
  },
  computed: {
    hasMore() {
      return this.gradeList.length < this.total
    }
  },
  mounted() {
    this.loadStatistics()
    this.loadAllGrades()
  },
  methods: {
    loadStatistics() {
      getGradeStatistics().then(r => {
        const data = r.data || r.rows || {}
        this.semesterGpa = data.avgGpa || data.gpa || null
        this.totalCredit = data.totalCredit || 0
        this.courseCount = data.courseCount || 0
        this.avgScore = data.avgScore || null
      }).catch(() => {})
    },
    loadAllGrades() {
      this.loading = true
      getMyGrades({ pageNum: 1, pageSize: 100 }).then(r => {
        this.allGrades = r.rows || []
        this.total = r.total || 0
        // 按学期分组
        const semSet = new Set()
        this.allGrades.forEach(g => {
          if (g.semester || g.termName) semSet.add(g.semester || g.termName)
        })
        this.semesters = [...semSet].sort().reverse()
        if (this.semesters.length > 0) {
          this.currentSemester = this.semesters[0]
          this.filterBySemester()
        } else {
          this.gradeList = this.allGrades.slice(0, this.pageSize)
        }
      }).finally(() => {
        this.loading = false
      })
    },
    filterBySemester() {
      const filtered = this.allGrades.filter(
        g => (g.semester || g.termName) === this.currentSemester
      )
      this.gradeList = filtered
      // 计算当前学期统计
      let totalGpaCredit = 0
      let totalCredit = 0
      let totalScore = 0
      filtered.forEach(g => {
        if (g.gpa != null && g.credit) {
          totalGpaCredit += g.gpa * g.credit
          totalCredit += g.credit
        }
        if (g.score != null) totalScore += g.score
      })
      this.semesterGpa = totalCredit > 0 ? (totalGpaCredit / totalCredit).toFixed(2) : '--'
      this.totalCredit = totalCredit
      this.courseCount = filtered.length
      this.avgScore = filtered.length > 0 ? (totalScore / filtered.length).toFixed(1) : '--'
    },
    switchSemester(sem) {
      this.currentSemester = sem
      this.filterBySemester()
    },
    loadMore() {
      const start = this.gradeList.length
      const more = this.allGrades.slice(start, start + this.pageSize)
      this.gradeList = this.gradeList.concat(more)
    },
    getScoreClass(score) {
      if (score == null) return ''
      if (score >= 90) return 'score-excellent'
      if (score >= 80) return 'score-good'
      if (score >= 70) return 'score-medium'
      if (score >= 60) return 'score-pass'
      return 'score-fail'
    },
    getLevelClass(level) {
      if (!level) return ''
      if (level.includes('优')) return 'level-excellent'
      if (level.includes('良')) return 'level-good'
      if (level.includes('中')) return 'level-medium'
      if (level.includes('及格')) return 'level-pass'
      if (level.includes('不及格')) return 'level-fail'
      return ''
    }
  }
}
</script>

<style scoped>
.mobile-grades {
  background: #f5f7fa;
  min-height: 100%;
}

/* GPA横幅 */
.gpa-banner {
  background: linear-gradient(135deg, #1a5276, #2e86c1);
  color: #fff;
  padding: 20px 16px;
  text-align: center;
}
.gpa-value { margin-bottom: 16px; }
.gpa-num { font-size: 36px; font-weight: 700; display: block; }
.gpa-label { font-size: 13px; opacity: 0.85; }
.gpa-stats {
  display: flex;
  justify-content: space-around;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  padding-top: 12px;
}
.stat-item { text-align: center; }
.stat-num { display: block; font-size: 18px; font-weight: 600; }
.stat-label { font-size: 11px; opacity: 0.8; }

/* 学期Tab */
.semester-tabs {
  display: flex;
  overflow-x: auto;
  background: #fff;
  padding: 0 12px;
  border-bottom: 1px solid #ebeef5;
  -webkit-overflow-scrolling: touch;
}
.sem-tab {
  padding: 10px 14px;
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  flex-shrink: 0;
}
.sem-tab.active {
  color: #2e86c1;
  border-bottom-color: #2e86c1;
  font-weight: 600;
}

/* 成绩卡片列表 */
.grade-list { padding: 12px; }
.loading-state, .empty-state {
  text-align: center;
  color: #c0c4cc;
  padding: 40px 0;
  font-size: 14px;
}
.loading-state i, .empty-state i { font-size: 32px; display: block; margin-bottom: 8px; }

.grade-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.grade-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.grade-course { font-size: 15px; font-weight: 600; color: #303133; flex: 1; }
.grade-score {
  font-size: 20px;
  font-weight: 700;
  min-width: 40px;
  text-align: right;
}
.score-excellent { color: #67c23a; }
.score-good { color: #409eff; }
.score-medium { color: #e6a23c; }
.score-pass { color: #909399; }
.score-fail { color: #f56c6c; }

.grade-bottom {
  display: flex;
  align-items: center;
  gap: 12px;
}
.grade-info { font-size: 12px; color: #909399; }
.grade-info i { margin-right: 2px; }
.grade-level {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  margin-left: auto;
}
.level-excellent { background: #f0f9eb; color: #67c23a; }
.level-good { background: #ecf5ff; color: #409eff; }
.level-medium { background: #fdf6ec; color: #e6a23c; }
.level-pass { background: #f4f4f5; color: #909399; }
.level-fail { background: #fef0f0; color: #f56c6c; }

/* 加载更多 */
.load-more {
  text-align: center;
  padding: 16px;
  color: #2e86c1;
  font-size: 13px;
  cursor: pointer;
}
</style>
