<template>
  <div class="mobile-selection">
    <!-- 选课轮次提示（Vue3 迁移：字体图标 → el-icon） -->
    <div v-if="currentRound" class="round-banner">
      <el-icon><Clock /></el-icon>
      <span>{{ currentRound.roundName }} | {{ fmtDate(currentRound.startTime) }} 至 {{ fmtDate(currentRound.endTime) }}</span>
    </div>
    <div v-else class="round-banner round-closed">
      <el-icon><Warning /></el-icon>
      <span>当前无开放的选课轮次</span>
    </div>

    <!-- 搜索过滤（Vue3 迁移：prefix-icon 字体类 → #prefix 插槽；.native 移除） -->
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索课程名称/教师"
        size="small"
        clearable
        @clear="filterCourses"
        @keyup.enter="filterCourses"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- 课程列表 -->
    <div class="course-list">
      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading"><Loading /></el-icon> 加载中...
      </div>
      <div v-else-if="filteredCourses.length === 0" class="empty-state">
        <el-icon><Notebook /></el-icon>
        <p>暂无可选课程</p>
      </div>

      <div
        v-for="course in filteredCourses"
        :key="course.offeringId"
        class="course-card"
      >
        <div class="card-header-row">
          <span class="card-course-name">{{ course.courseName }}</span>
          <span class="card-credit">{{ course.credit }}学分</span>
        </div>
        <div class="card-info-row">
          <span class="info-item"><el-icon><User /></el-icon> {{ course.teacherName || '待定' }}</span>
          <span class="info-item"><el-icon><Collection /></el-icon> {{ course.courseCode || '' }}</span>
        </div>
        <div class="card-footer-row">
          <div class="capacity-info">
            <span class="capacity-text">容量 {{ course.enrolledCount || 0 }}/{{ course.maxStudents || 0 }}</span>
            <div class="capacity-bar">
              <div
                class="capacity-fill"
                :style="{ width: getCapacityPercent(course) + '%' }"
                :class="{ full: getCapacityPercent(course) >= 100 }"
              ></div>
            </div>
          </div>
          <!-- Vue3 迁移：size="mini" → "small" -->
          <el-button
            v-if="(course.enrolledCount || 0) < (course.maxStudents || 0)"
            type="primary"
            size="small"
            :loading="enrollingId === course.offeringId"
            @click="handleEnroll(course)"
          >选课</el-button>
          <el-button v-else type="info" size="small" disabled>已满</el-button>
        </div>
      </div>
    </div>

    <!-- 加载更多 -->
    <div v-if="!loading && hasMore" class="load-more" @click="loadMore">
      点击加载更多
    </div>

    <!-- 冲突提示弹窗（底部弹窗效果） -->
    <div v-if="conflictVisible" class="conflict-overlay" @click.self="conflictVisible = false">
      <div class="conflict-sheet">
        <div class="sheet-header">
          <el-icon><WarningFilled /></el-icon> 选课冲突提示
        </div>
        <div class="sheet-body">
          <div v-for="(msg, idx) in conflictMessages" :key="idx" class="conflict-item">
            {{ msg }}
          </div>
        </div>
        <div class="sheet-footer">
          <el-button size="small" @click="conflictVisible = false">我知道了</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getCourseList, validateSelection, enrollCourse, listSelectionRound } from '@/api/mobile'

export default {
  name: 'MobileSelection',
  data() {
    return {
      loading: false,
      courseList: [],
      filteredCourses: [],
      currentRound: null,
      keyword: '',
      pageNum: 1,
      pageSize: 20,
      total: 0,
      enrollingId: null,
      conflictVisible: false,
      conflictMessages: []
    }
  },
  computed: {
    hasMore() {
      return this.filteredCourses.length < this.total
    }
  },
  mounted() {
    this.loadRound()
    this.loadCourses()
  },
  methods: {
    loadRound() {
      // 只取进行中的轮次（round_status=1）
      listSelectionRound({ roundStatus: '1', pageNum: 1, pageSize: 10 }).then(r => {
        const rows = r.rows || r.data || []
        this.currentRound = rows.length > 0 ? rows[0] : null
      }).catch(() => {})
    },
    loadCourses() {
      this.loading = true
      getCourseList({ pageNum: this.pageNum, pageSize: this.pageSize }).then(r => {
        const rows = r.rows || []
        this.courseList = this.courseList.concat(rows)
        this.total = r.total || 0
        this.filterCourses()
      }).finally(() => {
        this.loading = false
      })
    },
    filterCourses() {
      if (!this.keyword) {
        this.filteredCourses = this.courseList
      } else {
        const kw = this.keyword.toLowerCase()
        this.filteredCourses = this.courseList.filter(c =>
          (c.courseName && c.courseName.toLowerCase().includes(kw)) ||
          (c.teacherName && c.teacherName.toLowerCase().includes(kw))
        )
      }
    },
    loadMore() {
      this.pageNum++
      this.loadCourses()
    },
    getCapacityPercent(course) {
      const cap = course.maxStudents || 1
      return Math.min(Math.round(((course.enrolledCount || 0) / cap) * 100), 100)
    },
    fmtDate(v) {
      if (!v) return '--'
      return String(v).substring(0, 10)
    },
    async handleEnroll(course) {
      const offeringId = course.offeringId
      const roundId = this.currentRound ? this.currentRound.roundId : null
      if (!roundId) {
        this.$message.warning('当前没有进行中的选课轮次')
        return
      }
      // 先进行冲突验证（后端返回 List<ConflictWarning>，空数组表示无冲突）
      try {
        this.enrollingId = offeringId
        const validResult = await validateSelection({ courseOfferingId: offeringId, roundId })
        const conflicts = validResult.data || []
        if (Array.isArray(conflicts) && conflicts.length > 0) {
          this.conflictMessages = conflicts.map(c => c.message || '该课程与已选课程存在时间冲突')
          this.conflictVisible = true
          this.enrollingId = null
          return
        }
      } catch (e) {
        // 验证接口报错时继续尝试选课
      }

      // 确认选课
      try {
        await this.$confirm('确认选择课程: ' + course.courseName + ' ?', '选课确认', {
          confirmButtonText: '确认选课',
          cancelButtonText: '取消',
          type: 'info'
        })
      } catch {
        this.enrollingId = null
        return
      }

      // 执行选课（后端已做轮次/门数/冲突/容量校验）
      try {
        await enrollCourse({ courseOfferingId: offeringId, roundId })
        this.$message.success('选课成功！')
        this.courseList = []
        this.filteredCourses = []
        this.pageNum = 1
        this.total = 0
        this.loadCourses()
      } catch (e) {
        // 失败提示已由请求拦截器统一弹出
      } finally {
        this.enrollingId = null
      }
    }
  }
}
</script>

<style scoped>
.mobile-selection {
  background: #f5f7fa;
  min-height: 100%;
  padding-bottom: 60px;
}

/* 选课轮次 */
.round-banner {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background: #ecf5ff;
  color: #409eff;
  font-size: 12px;
  border-bottom: 1px solid #d9ecff;
}
.round-banner .el-icon { margin-right: 6px; font-size: 14px; }
.round-closed {
  background: #fef0f0;
  color: #f56c6c;
  border-bottom-color: #fde2e2;
}

/* 搜索栏 */
.search-bar {
  padding: 12px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

/* 课程列表 */
.course-list { padding: 12px; }
.loading-state, .empty-state {
  text-align: center;
  color: #c0c4cc;
  padding: 40px 0;
  font-size: 14px;
}
.loading-state .el-icon, .empty-state .el-icon { font-size: 32px; display: block; margin: 0 auto 8px; }

.course-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.card-course-name { font-size: 15px; font-weight: 600; color: #303133; flex: 1; }
.card-credit {
  font-size: 11px;
  color: #007ab8;
  background: #ecf5ff;
  padding: 2px 8px;
  border-radius: 10px;
  white-space: nowrap;
  margin-left: 8px;
}
.card-info-row {
  display: flex;
  gap: 16px;
  margin-bottom: 10px;
}
.info-item { font-size: 12px; color: #909399; }
.info-item .el-icon { margin-right: 2px; vertical-align: middle; }

.card-footer-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.capacity-info { flex: 1; margin-right: 12px; }
.capacity-text { font-size: 11px; color: #909399; display: block; margin-bottom: 4px; }
.capacity-bar {
  height: 4px;
  background: #f0f2f5;
  border-radius: 2px;
  overflow: hidden;
}
.capacity-fill {
  height: 100%;
  background: #409eff;
  border-radius: 2px;
  transition: width 0.3s;
}
.capacity-fill.full { background: #f56c6c; }

/* 加载更多 */
.load-more {
  text-align: center;
  padding: 16px;
  color: #007ab8;
  font-size: 13px;
  cursor: pointer;
}

/* 底部冲突弹窗 */
.conflict-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 2000;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.conflict-sheet {
  background: #fff;
  border-radius: 12px 12px 0 0;
  width: 100%;
  max-width: 768px;
  padding: 0 0 20px;
  animation: slideUp 0.3s ease;
}
@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}
.sheet-header {
  padding: 16px;
  font-size: 16px;
  font-weight: 600;
  color: #e6a23c;
  border-bottom: 1px solid #ebeef5;
  text-align: center;
}
.sheet-header .el-icon { margin-right: 6px; vertical-align: middle; }
.sheet-body { padding: 16px; max-height: 40vh; overflow-y: auto; }
.conflict-item {
  padding: 10px 12px;
  background: #fdf6ec;
  border-radius: 6px;
  font-size: 13px;
  color: #e6a23c;
  margin-bottom: 8px;
  border-left: 3px solid #e6a23c;
}
.sheet-footer { text-align: center; padding: 0 16px; }
</style>
