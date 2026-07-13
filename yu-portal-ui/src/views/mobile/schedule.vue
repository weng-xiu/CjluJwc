<template>
  <div class="mobile-schedule">
    <!-- 周次/日期切换 -->
    <div class="week-bar">
      <div class="week-nav" @click="prevWeek">
        <i class="el-icon-arrow-left"></i>
      </div>
      <div class="week-label" @click="toggleView">
        {{ viewMode === 'week' ? '第' + currentWeek + '周' : formatDate(currentDate) }}
        <i class="el-icon-sort"></i>
      </div>
      <div class="week-nav" @click="nextWeek">
        <i class="el-icon-arrow-right"></i>
      </div>
    </div>

    <!-- 周视图 -->
    <div v-if="viewMode === 'week'" class="week-view">
      <!-- 星期表头 -->
      <div class="day-header">
        <div class="time-col">时间</div>
        <div
          v-for="d in weekDays"
          :key="d.dayOfWeek"
          class="day-col"
          :class="{ today: d.isToday }"
        >
          <div class="day-name">{{ d.label }}</div>
          <div class="day-date">{{ d.monthDay }}</div>
        </div>
      </div>

      <!-- 课程网格 -->
      <div class="schedule-grid">
        <div v-for="period in periods" :key="period.index" class="period-row">
          <div class="time-col period-label">{{ period.label }}</div>
          <div v-for="d in weekDays" :key="d.dayOfWeek" class="day-col period-cell">
            <div
              v-if="getCellCourse(d.dayOfWeek, period.index)"
              class="course-card"
              :style="{ background: getCourseColor(getCellCourse(d.dayOfWeek, period.index)) }"
              @click="showCourseDetail(getCellCourse(d.dayOfWeek, period.index))"
            >
              <div class="card-name">{{ getCellCourse(d.dayOfWeek, period.index).courseName | truncate }}</div>
              <div class="card-room">{{ getCellCourse(d.dayOfWeek, period.index).classroom }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 日视图 -->
    <div v-else class="day-view">
      <div v-if="dayCourses.length === 0" class="empty-state">
        <i class="el-icon-date"></i>
        <p>当天没有课程安排</p>
      </div>
      <div
        v-for="course in dayCourses"
        :key="course.id"
        class="day-course-card"
        @click="showCourseDetail(course)"
      >
        <div class="card-time-bar" :style="{ background: getCourseColor(course) }"></div>
        <div class="card-body">
          <div class="card-top">
            <span class="card-course-name">{{ course.courseName }}</span>
            <span class="card-period">{{ course.startTime }} - {{ course.endTime }}</span>
          </div>
          <div class="card-bottom">
            <span class="card-info"><i class="el-icon-user"></i> {{ course.teacherName || '待定' }}</span>
            <span class="card-info"><i class="el-icon-location-outline"></i> {{ course.classroom || '待定' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 课程详情弹窗 -->
    <el-dialog
      :visible.sync="detailVisible"
      :title="detailCourse.courseName"
      width="90%"
      top="20vh"
      custom-class="mobile-dialog"
    >
      <div class="detail-row"><i class="el-icon-user"></i> 教师：{{ detailCourse.teacherName || '待定' }}</div>
      <div class="detail-row"><i class="el-icon-location-outline"></i> 教室：{{ detailCourse.classroom || '待定' }}</div>
      <div class="detail-row"><i class="el-icon-time"></i> 时间：{{ detailCourse.startTime }} - {{ detailCourse.endTime }}</div>
      <div class="detail-row"><i class="el-icon-date"></i> 周次：第 {{ detailCourse.weekNum || '-' }} 周</div>
      <span slot="footer">
        <el-button size="small" @click="detailVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getMySchedule } from '@/api/mobile'

const COLORS = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#9b59b6', '#1abc9c', '#e74c3c']

export default {
  name: 'MobileSchedule',
  filters: {
    truncate(val) {
      if (!val) return ''
      return val.length > 6 ? val.substring(0, 6) + '..' : val
    }
  },
  data() {
    return {
      viewMode: 'day', // 'week' | 'day'
      currentWeek: 1,
      currentDate: new Date(),
      scheduleList: [],
      detailVisible: false,
      detailCourse: {},
      periods: [
        { index: 1, label: '1-2节' },
        { index: 2, label: '3-4节' },
        { index: 3, label: '5-6节' },
        { index: 4, label: '7-8节' },
        { index: 5, label: '9-10节' }
      ]
    }
  },
  computed: {
    weekDays() {
      const base = this.getMonday(this.currentDate)
      const days = []
      const labels = ['一', '二', '三', '四', '五', '六', '日']
      const today = new Date()
      const todayStr = today.toDateString()
      for (let i = 0; i < 7; i++) {
        const d = new Date(base)
        d.setDate(base.getDate() + i)
        days.push({
          dayOfWeek: i + 1,
          label: '周' + labels[i],
          monthDay: (d.getMonth() + 1) + '/' + d.getDate(),
          isToday: d.toDateString() === todayStr,
          date: d
        })
      }
      return days
    },
    dayCourses() {
      const dateStr = this.formatDateStr(this.currentDate)
      const filtered = this.scheduleList.filter(c => c.date === dateStr || c.dayOfWeek === this.currentDate.getDay())
      // 按节次排序
      return filtered.sort((a, b) => (a.periodIndex || 0) - (b.periodIndex || 0))
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    loadData() {
      getMySchedule({ weekNum: this.currentWeek }).then(r => {
        this.scheduleList = r.rows || r.data || []
      }).catch(() => {
        this.scheduleList = []
      })
    },
    toggleView() {
      this.viewMode = this.viewMode === 'week' ? 'day' : 'week'
    },
    prevWeek() {
      if (this.viewMode === 'week') {
        if (this.currentWeek > 1) this.currentWeek--
      } else {
        const d = new Date(this.currentDate)
        d.setDate(d.getDate() - 1)
        this.currentDate = d
      }
      this.loadData()
    },
    nextWeek() {
      if (this.viewMode === 'week') {
        this.currentWeek++
      } else {
        const d = new Date(this.currentDate)
        d.setDate(d.getDate() + 1)
        this.currentDate = d
      }
      this.loadData()
    },
    getCellCourse(dayOfWeek, periodIndex) {
      return this.scheduleList.find(
        c => c.dayOfWeek === dayOfWeek && c.periodIndex === periodIndex
      ) || null
    },
    getCourseColor(course) {
      if (!course) return COLORS[0]
      const code = course.courseCode || course.courseName || ''
      let hash = 0
      for (let i = 0; i < code.length; i++) {
        hash = code.charCodeAt(i) + ((hash << 5) - hash)
      }
      return COLORS[Math.abs(hash) % COLORS.length]
    },
    showCourseDetail(course) {
      this.detailCourse = course
      this.detailVisible = true
    },
    getMonday(d) {
      const date = new Date(d)
      const day = date.getDay()
      const diff = date.getDate() - day + (day === 0 ? -6 : 1)
      return new Date(date.setDate(diff))
    },
    formatDate(d) {
      return (d.getMonth() + 1) + '月' + d.getDate() + '日'
    },
    formatDateStr(d) {
      return d.getFullYear() + '-' +
        String(d.getMonth() + 1).padStart(2, '0') + '-' +
        String(d.getDate()).padStart(2, '0')
    }
  }
}
</script>

<style scoped>
.mobile-schedule {
  background: #f5f7fa;
  min-height: 100%;
}

/* 周次切换栏 */
.week-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 10px 16px;
  border-bottom: 1px solid #ebeef5;
}
.week-nav {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #f0f2f5;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
}
.week-nav:active { background: #e4e7ed; }
.week-label {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  cursor: pointer;
}
.week-label i { font-size: 12px; margin-left: 4px; color: #909399; }

/* 周视图 */
.day-header {
  display: flex;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}
.day-header .time-col {
  width: 44px;
  flex-shrink: 0;
  text-align: center;
  font-size: 10px;
  color: #909399;
  padding: 4px 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.day-header .day-col {
  flex: 1;
  text-align: center;
  padding: 6px 0;
  border-left: 1px solid #f0f2f5;
}
.day-name { font-size: 12px; color: #606266; }
.day-date { font-size: 10px; color: #909399; }
.day-col.today .day-name { color: #007ab8; font-weight: 600; }
.day-col.today { background: #ecf5ff; }

.schedule-grid { background: #fff; }
.period-row {
  display: flex;
  min-height: 60px;
  border-top: 1px solid #f0f2f5;
}
.period-label {
  width: 44px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: #909399;
}
.period-cell {
  flex: 1;
  border-left: 1px solid #f0f2f5;
  padding: 2px;
  min-height: 60px;
}
.course-card {
  border-radius: 4px;
  padding: 4px;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  cursor: pointer;
  color: #fff;
}
.card-name { font-size: 11px; font-weight: 500; line-height: 1.2; }
.card-room { font-size: 9px; opacity: 0.85; margin-top: 2px; }

/* 日视图 */
.day-view { padding: 12px; }
.empty-state {
  text-align: center;
  color: #c0c4cc;
  padding: 60px 0;
}
.empty-state i { font-size: 48px; display: block; margin-bottom: 12px; }
.empty-state p { font-size: 14px; }

.day-course-card {
  display: flex;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 10px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  cursor: pointer;
}
.card-time-bar {
  width: 4px;
  flex-shrink: 0;
}
.card-body {
  flex: 1;
  padding: 12px;
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}
.card-course-name { font-size: 15px; font-weight: 600; color: #303133; flex: 1; }
.card-period { font-size: 12px; color: #909399; white-space: nowrap; margin-left: 8px; }
.card-bottom { display: flex; gap: 16px; }
.card-info { font-size: 12px; color: #909399; }
.card-info i { margin-right: 2px; }

/* 详情弹窗 */
.detail-row {
  font-size: 14px;
  color: #606266;
  padding: 8px 0;
  border-bottom: 1px solid #f0f2f5;
}
.detail-row:last-child { border-bottom: none; }
.detail-row i { color: #007ab8; margin-right: 6px; }

::v-deep .mobile-dialog {
  border-radius: 12px;
}
</style>
