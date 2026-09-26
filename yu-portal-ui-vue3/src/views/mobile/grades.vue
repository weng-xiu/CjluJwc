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

    <!-- 成绩卡片列表（Vue3 迁移：el-icon-loading 字体类 → Loading 组件；字体图标 → el-icon） -->
    <div class="grade-list">
      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading"><Loading /></el-icon> 加载中...
      </div>
      <div v-else-if="gradeList.length === 0" class="empty-state">
        <el-icon><Document /></el-icon>
        <p>暂无成绩数据</p>
      </div>
      <div
        v-for="item in gradeList"
        :key="item.gradeId"
        class="grade-card"
      >
        <div class="grade-top">
          <span class="grade-course">{{ item.courseName }}</span>
          <span class="grade-score" :class="getScoreClass(item.totalScore)">{{ item.totalScore != null ? item.totalScore : '--' }}</span>
        </div>
        <div class="grade-bottom">
          <span class="grade-info">
            <el-icon><Notebook /></el-icon> {{ item.credit || 0 }} 学分
          </span>
          <span class="grade-info">
            <el-icon><DataLine /></el-icon> 绩点 {{ item.gradePoint != null ? item.gradePoint : '--' }}
          </span>
          <span class="grade-level" :class="getLevelClass(item.gradeLevel)">
            {{ item.gradeLevel || '--' }}
          </span>
        </div>
      </div>
    </div>

    <!-- 加载更多 -->
    <div class="list-footer">
      <span v-if="loading && gradeList.length > 0"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</span>
      <span v-else-if="finished">没有更多了</span>
      <span v-else-if="gradeList.length > 0" class="load-more" @click="loadMore">点击加载更多</span>
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
      total: 0,
      allFiltered: [],
      finished: false,
      scrollEl: null
    }
  },
  mounted() {
    this.loadStatistics()
    this.loadAllGrades()
    this.bindScroll()
  },
  activated() {
    this.bindScroll()
  },
  // Vue3 迁移：beforeDestroy → beforeUnmount
  beforeUnmount() {
    this.unbindScroll()
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
          if (g.semesterName) semSet.add(g.semesterName)
        })
        this.semesters = [...semSet].sort().reverse()
        if (this.semesters.length > 0) {
          this.currentSemester = this.semesters[0]
          this.filterBySemester()
        } else {
          this.applyDisplayPage()
        }
      }).finally(() => {
        this.loading = false
      })
    },
    applyDisplayPage() {
      // 根据当前页截取当前学期（或全部）数据用于展示
      const source = this.allFiltered.length > 0 ? this.allFiltered : this.allGrades
      this.gradeList = source.slice(0, this.pageNum * this.pageSize)
      this.finished = this.gradeList.length >= source.length
    },
    filterBySemester() {
      const filtered = this.allGrades.filter(
        g => g.semesterName === this.currentSemester
      )
      this.allFiltered = filtered
      this.pageNum = 1
      this.finished = false
      this.applyDisplayPage()
      // 计算当前学期统计
      let totalGpaCredit = 0
      let totalCredit = 0
      let totalScore = 0
      filtered.forEach(g => {
        if (g.gradePoint != null && g.credit) {
          totalGpaCredit += g.gradePoint * g.credit
          totalCredit += g.credit
        }
        if (g.totalScore != null) totalScore += g.totalScore
      })
      this.semesterGpa = totalCredit > 0 ? (totalGpaCredit / totalCredit).toFixed(2) : '--'
      this.totalCredit = totalCredit
      this.courseCount = filtered.length
      this.avgScore = filtered.length > 0 ? (totalScore / filtered.length).toFixed(1) : '--'
    },
    switchSemester(sem) {
      if (this.currentSemester === sem) return
      this.currentSemester = sem
      this.filterBySemester()
    },
    loadMore() {
      // 防重复加载；无更多数据时设置 finished
      if (this.loading || this.finished) return
      this.pageNum++
      this.applyDisplayPage()
    },
    getScrollContainer() {
      let node = this.$el
      while (node && node.tagName !== 'BODY') {
        if (node.classList && node.classList.contains('mobile-content')) return node
        node = node.parentNode
      }
      return window
    },
    bindScroll() {
      this.scrollEl = this.getScrollContainer()
      if (this.scrollEl) {
        this.scrollEl.addEventListener('scroll', this.onScroll, { passive: true })
      }
    },
    unbindScroll() {
      if (this.scrollEl) {
        this.scrollEl.removeEventListener('scroll', this.onScroll)
        this.scrollEl = null
      }
    },
    onScroll() {
      if (this.loading || this.finished) return
      const el = this.scrollEl
      let scrollTop, clientHeight, scrollHeight
      if (el === window) {
        scrollTop = window.pageYOffset || document.documentElement.scrollTop
        clientHeight = window.innerHeight
        scrollHeight = document.documentElement.scrollHeight
      } else {
        scrollTop = el.scrollTop
        clientHeight = el.clientHeight
        scrollHeight = el.scrollHeight
      }
      if (scrollTop + clientHeight >= scrollHeight - 80) {
        this.loadMore()
      }
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
  background: linear-gradient(135deg, #003366, #007ab8);
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
  color: #007ab8;
  border-bottom-color: #007ab8;
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
.loading-state .el-icon, .empty-state .el-icon { font-size: 32px; display: block; margin: 0 auto 8px; }

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
.grade-info .el-icon { margin-right: 2px; vertical-align: middle; }
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
.list-footer {
  text-align: center;
  padding: 16px;
  color: #c0c4cc;
  font-size: 13px;
}
.list-footer .el-icon { margin-right: 4px; vertical-align: middle; }
.load-more {
  color: #007ab8;
  cursor: pointer;
}
</style>
