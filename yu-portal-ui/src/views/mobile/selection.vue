<template>
  <div class="mobile-selection">
    <!-- 选课轮次提示 -->
    <div v-if="currentRound" class="round-banner">
      <i class="el-icon-time"></i>
      <span>{{ currentRound.roundName }} | {{ currentRound.startDate }} 至 {{ currentRound.endDate }}</span>
    </div>
    <div v-else class="round-banner round-closed">
      <i class="el-icon-warning-outline"></i>
      <span>当前无开放的选课轮次</span>
    </div>

    <!-- 搜索过滤 -->
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索课程名称/教师"
        prefix-icon="el-icon-search"
        size="small"
        clearable
        @clear="filterCourses"
        @keyup.enter.native="filterCourses"
      ></el-input>
    </div>

    <!-- 课程列表 -->
    <div class="course-list">
      <div v-if="loading" class="loading-state">
        <i class="el-icon-loading"></i> 加载中...
      </div>
      <div v-else-if="filteredCourses.length === 0" class="empty-state">
        <i class="el-icon-notebook-2"></i>
        <p>暂无可选课程</p>
      </div>

      <div
        v-for="course in filteredCourses"
        :key="course.id || course.courseId"
        class="course-card"
      >
        <div class="card-header-row">
          <span class="card-course-name">{{ course.courseName }}</span>
          <span class="card-credit">{{ course.credit }}学分</span>
        </div>
        <div class="card-info-row">
          <span class="info-item"><i class="el-icon-user"></i> {{ course.teacherName || '待定' }}</span>
          <span class="info-item"><i class="el-icon-time"></i> {{ course.classTime || '待定' }}</span>
        </div>
        <div class="card-footer-row">
          <div class="capacity-info">
            <span class="capacity-text">容量 {{ course.enrolled || 0 }}/{{ course.capacity || 0 }}</span>
            <div class="capacity-bar">
              <div
                class="capacity-fill"
                :style="{ width: getCapacityPercent(course) + '%' }"
                :class="{ full: getCapacityPercent(course) >= 100 }"
              ></div>
            </div>
          </div>
          <el-button
            v-if="(course.enrolled || 0) < (course.capacity || 0)"
            type="primary"
            size="mini"
            :loading="enrollingId === (course.courseId || course.id)"
            @click="handleEnroll(course)"
          >选课</el-button>
          <el-button v-else type="info" size="mini" disabled>已满</el-button>
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
          <i class="el-icon-warning"></i> 选课冲突提示
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
      listSelectionRound().then(r => {
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
      const cap = course.capacity || 1
      return Math.min(Math.round(((course.enrolled || 0) / cap) * 100), 100)
    },
    async handleEnroll(course) {
      const courseId = course.courseId || course.id
      // 先进行冲突验证
      try {
        this.enrollingId = courseId
        const validResult = await validateSelection({ courseId })
        if (validResult.data && validResult.data.hasConflict) {
          // 有冲突，显示底部弹窗
          this.conflictMessages = validResult.data.conflictMessages || ['该课程与已选课程存在时间冲突']
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

      // 执行选课
      try {
        await enrollCourse({ courseId })
        this.$message.success('选课成功！')
        // 刷新列表
        this.courseList = []
        this.pageNum = 1
        this.loadCourses()
      } catch (e) {
        this.$message.error('选课失败，请重试')
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
.round-banner i { margin-right: 6px; font-size: 14px; }
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
.loading-state i, .empty-state i { font-size: 32px; display: block; margin-bottom: 8px; }

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
  color: #2e86c1;
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
.info-item i { margin-right: 2px; }

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
  color: #2e86c1;
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
.sheet-header i { margin-right: 6px; }
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
