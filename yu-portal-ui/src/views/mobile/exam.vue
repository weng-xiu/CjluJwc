<template>
  <div
    class="mobile-exam"
    @touchstart="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
  >
    <!-- 下拉刷新提示 -->
    <div class="pull-refresh" :style="{ height: pullDistance + 'px' }">
      <i v-if="refreshing" class="el-icon-loading"></i>
      <i v-else-if="pullDistance >= triggerDistance" class="el-icon-arrow-down"></i>
      <i v-else class="el-icon-arrow-down rotate"></i>
      <span>{{ refreshText }}</span>
    </div>

    <!-- 类型筛选 -->
    <div class="filter-bar">
      <div
        v-for="opt in typeOptions"
        :key="opt.value"
        class="filter-item"
        :class="{ active: queryParams.examType === opt.value }"
        @click="switchType(opt.value)"
      >{{ opt.label }}</div>
    </div>

    <!-- 考试卡片列表 -->
    <div class="exam-list">
      <div v-if="loading && list.length === 0" class="loading-state">
        <i class="el-icon-loading"></i> 加载中...
      </div>
      <div v-else-if="list.length === 0" class="empty-state">
        <i class="el-icon-tickets"></i>
        <p>暂无考试安排</p>
      </div>

      <div v-for="item in list" :key="item.id" class="exam-card">
        <div class="card-top">
          <span class="exam-name">{{ item.examName || item.courseName || '考试安排' }}</span>
          <span class="exam-type-tag" :class="getTypeClass(item.examType)">
            {{ getTypeText(item.examType) }}
          </span>
        </div>
        <div class="card-course" v-if="item.examName && item.courseName">
          <i class="el-icon-notebook-2"></i> {{ item.courseName }}
        </div>
        <div class="card-info">
          <div class="info-row">
            <i class="el-icon-date"></i>
            <span class="info-label">考试日期</span>
            <span class="info-value">{{ item.examDate || '--' }}</span>
          </div>
          <div class="info-row">
            <i class="el-icon-time"></i>
            <span class="info-label">考试时间</span>
            <span class="info-value">{{ formatTime(item) }}</span>
          </div>
          <div class="info-row">
            <i class="el-icon-location-outline"></i>
            <span class="info-label">考试地点</span>
            <span class="info-value">{{ item.classroom || item.examLocation || item.roomName || item.place || '--' }}</span>
          </div>
          <div class="info-row" v-if="item.seatNo">
            <i class="el-icon-postcard"></i>
            <span class="info-label">座位号</span>
            <span class="info-value">{{ item.seatNo }}</span>
          </div>
        </div>
        <div class="card-status" v-if="item.arrangeStatus != null && item.arrangeStatus !== ''">
          <span class="status-dot" :class="getStatusClass(item.arrangeStatus)"></span>
          {{ getStatusText(item.arrangeStatus) }}
        </div>
      </div>
    </div>

    <!-- 底部状态 -->
    <div class="list-footer">
      <span v-if="loading && list.length > 0"><i class="el-icon-loading"></i> 加载中...</span>
      <span v-else-if="finished">没有更多了</span>
      <span v-else-if="list.length > 0" class="load-more" @click="loadMore">点击加载更多</span>
    </div>
  </div>
</template>

<script>
import { listExams } from '@/api/portal/exam'

export default {
  name: 'MobileExam',
  data() {
    return {
      loading: false,
      refreshing: false,
      list: [],
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        examType: null,
        examName: null,
        arrangeStatus: null
      },
      typeOptions: [
        { label: '全部', value: null },
        { label: '期末考试', value: 'final' },
        { label: '补考', value: 'makeup' },
        { label: '期中考试', value: 'midterm' }
      ],
      // 下拉刷新
      pullDistance: 0,
      triggerDistance: 50,
      startY: 0,
      pulling: false,
      scrollEl: null
    }
  },
  computed: {
    finished() {
      return this.list.length >= this.total && this.list.length > 0
    },
    refreshText() {
      if (this.refreshing) return '正在刷新...'
      if (this.pullDistance >= this.triggerDistance) return '松开立即刷新'
      return '下拉刷新'
    }
  },
  mounted() {
    this.getList()
    this.bindScroll()
  },
  activated() {
    this.bindScroll()
  },
  beforeDestroy() {
    this.unbindScroll()
  },
  methods: {
    getList() {
      this.loading = true
      listExams(this.queryParams).then(r => {
        const rows = r.rows || r.data || []
        if (this.queryParams.pageNum === 1) {
          this.list = rows
        } else {
          this.list = this.list.concat(rows)
        }
        this.total = r.total || 0
        // 后端未返回 total 时，按本页条数判断是否结束
        if (!r.total && rows.length < this.queryParams.pageSize) {
          this.total = this.list.length
        }
      }).finally(() => {
        this.loading = false
        this.refreshing = false
        this.pullDistance = 0
      })
    },
    loadMore() {
      if (this.loading || this.finished) return
      this.queryParams.pageNum++
      this.getList()
    },
    refresh() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    switchType(value) {
      if (this.queryParams.examType === value) return
      this.queryParams.examType = value
      this.queryParams.pageNum = 1
      this.getList()
    },
    formatTime(item) {
      if (item.examTime) return item.examTime
      if (item.startTime || item.endTime) {
        return (item.startTime || '') + (item.endTime ? ' ~ ' + item.endTime : '')
      }
      return '--'
    },
    getTypeText(type) {
      const map = { final: '期末考试', makeup: '补考', midterm: '期中考试', normal: '常规考试' }
      return map[type] || type || '考试'
    },
    getTypeClass(type) {
      if (type === 'final') return 'type-final'
      if (type === 'makeup') return 'type-makeup'
      if (type === 'midterm') return 'type-midterm'
      return 'type-default'
    },
    getStatusText(status) {
      const map = { 0: '待安排', 1: '已安排', 2: '已结束', arranged: '已安排', pending: '待安排' }
      return map[status] || (typeof status === 'string' ? status : '已安排')
    },
    getStatusClass(status) {
      if (status === 2 || status === 'finished') return 'status-done'
      if (status === 0 || status === 'pending') return 'status-pending'
      return 'status-arranged'
    },
    // 滚动容器查找（MobileLayout 的 .mobile-content）
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
      // 距底部 80px 触发加载
      if (scrollTop + clientHeight >= scrollHeight - 80) {
        this.loadMore()
      }
    },
    // 下拉刷新
    onTouchStart(e) {
      const el = this.scrollEl
      const top = el === window ? (window.pageYOffset || document.documentElement.scrollTop) : el.scrollTop
      if (top <= 0 && !this.refreshing) {
        this.startY = e.touches[0].clientY
        this.pulling = true
      }
    },
    onTouchMove(e) {
      if (!this.pulling || this.refreshing) return
      const delta = e.touches[0].clientY - this.startY
      if (delta > 0) {
        // 阻尼效果
        this.pullDistance = Math.min(delta * 0.5, 80)
      }
    },
    onTouchEnd() {
      if (!this.pulling) return
      this.pulling = false
      if (this.pullDistance >= this.triggerDistance) {
        this.refreshing = true
        this.pullDistance = 40
        this.refresh()
      } else {
        this.pullDistance = 0
      }
    }
  }
}
</script>

<style scoped>
.mobile-exam {
  background: #f5f7fa;
  min-height: 100%;
}

/* 下拉刷新 */
.pull-refresh {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 12px;
  overflow: hidden;
  background: #f5f7fa;
}
.pull-refresh i {
  margin-right: 6px;
  font-size: 14px;
}
.pull-refresh .rotate {
  transform: rotate(180deg);
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  background: #fff;
  padding: 8px 12px;
  border-bottom: 1px solid #ebeef5;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}
.filter-item {
  flex-shrink: 0;
  padding: 6px 16px;
  margin-right: 8px;
  font-size: 13px;
  color: #606266;
  background: #f4f4f5;
  border-radius: 16px;
  cursor: pointer;
}
.filter-item.active {
  color: #fff;
  background: linear-gradient(135deg, #1a5276, #2e86c1);
}

/* 列表 */
.exam-list { padding: 12px; }
.loading-state, .empty-state {
  text-align: center;
  color: #c0c4cc;
  padding: 40px 0;
  font-size: 14px;
}
.loading-state i, .empty-state i {
  font-size: 32px;
  display: block;
  margin-bottom: 8px;
}

.exam-card {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.exam-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  flex: 1;
  margin-right: 8px;
  word-break: break-all;
}
.exam-type-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  white-space: nowrap;
  flex-shrink: 0;
}
.type-final { background: #ecf5ff; color: #2e86c1; }
.type-makeup { background: #fdf6ec; color: #e6a23c; }
.type-midterm { background: #f0f9eb; color: #67c23a; }
.type-default { background: #f4f4f5; color: #909399; }

.card-course {
  font-size: 13px;
  color: #606266;
  margin-bottom: 10px;
}
.card-course i { margin-right: 4px; color: #2e86c1; }

.card-info {
  background: #fafbfc;
  border-radius: 8px;
  padding: 8px 12px;
}
.info-row {
  display: flex;
  align-items: center;
  font-size: 13px;
  padding: 5px 0;
  color: #606266;
}
.info-row i {
  color: #2e86c1;
  margin-right: 8px;
  width: 16px;
  text-align: center;
  flex-shrink: 0;
}
.info-label {
  color: #909399;
  width: 64px;
  flex-shrink: 0;
}
.info-value {
  color: #303133;
  flex: 1;
  word-break: break-all;
}

.card-status {
  display: flex;
  align-items: center;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
  font-size: 12px;
  color: #909399;
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
}
.status-arranged { background: #2e86c1; }
.status-pending { background: #e6a23c; }
.status-done { background: #67c23a; }

/* 底部 */
.list-footer {
  text-align: center;
  padding: 16px;
  color: #c0c4cc;
  font-size: 13px;
}
.list-footer i { margin-right: 4px; }
.load-more {
  color: #2e86c1;
  cursor: pointer;
}
</style>
