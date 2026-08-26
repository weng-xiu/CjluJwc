<template>
  <div class="mobile-invigilation">
    <!-- 监考卡片列表 -->
    <div class="invigilation-list">
      <div v-if="loading && list.length === 0" class="loading-state">
        <i class="el-icon-loading"></i> 加载中...
      </div>
      <div v-else-if="list.length === 0" class="empty-state">
        <i class="el-icon-view"></i>
        <p>暂无监考安排</p>
      </div>

      <div v-for="item in list" :key="item.id" class="invigilation-card">
        <div class="card-date-col">
          <div class="date-day">{{ getDay(item.examDate) }}</div>
          <div class="date-month">{{ getMonth(item.examDate) }}</div>
        </div>
        <div class="card-body">
          <div class="card-top">
            <span class="course-name">{{ item.courseName || item.examName || '考试科目' }}</span>
            <span class="duty-tag" :class="getDutyClass(item.invigilationType)">
              {{ getDutyText(item.invigilationType) }}
            </span>
          </div>
          <div class="card-info">
            <div class="info-row">
              <i class="el-icon-time"></i>
              <span class="info-label">时段</span>
              <span class="info-value">{{ formatTime(item) }}</span>
            </div>
            <div class="info-row">
              <i class="el-icon-location-outline"></i>
              <span class="info-label">教室</span>
              <span class="info-value">{{ item.classroom || item.examLocation || item.roomName || '--' }}</span>
            </div>
            <div class="info-row" v-if="item.remark">
              <i class="el-icon-document"></i>
              <span class="info-label">备注</span>
              <span class="info-value">{{ item.remark }}</span>
            </div>
          </div>
          <div class="card-footer">
            <span class="status-tag" :class="getStatusClass(item.status)">
              {{ getStatusText(item.status) }}
            </span>
            <span class="exam-date-full">{{ item.examDate || '--' }}</span>
          </div>
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
import { listInvigilations } from '@/api/portal/invigilation'

export default {
  name: 'MobileInvigilation',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10 },
      scrollEl: null
    }
  },
  computed: {
    finished() {
      return this.list.length >= this.total && this.list.length > 0
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
      listInvigilations(this.queryParams).then(r => {
        const rows = r.rows || r.data || []
        if (this.queryParams.pageNum === 1) {
          this.list = rows
        } else {
          this.list = this.list.concat(rows)
        }
        this.total = r.total || 0
        if (!r.total && rows.length < this.queryParams.pageSize) {
          this.total = this.list.length
        }
      }).finally(() => {
        this.loading = false
      })
    },
    loadMore() {
      if (this.loading || this.finished) return
      this.queryParams.pageNum++
      this.getList()
    },
    formatTime(item) {
      if (item.examTime) return item.examTime
      if (item.timeSlot) return item.timeSlot
      if (item.startTime || item.endTime) {
        return (item.startTime || '') + (item.endTime ? ' ~ ' + item.endTime : '')
      }
      return '--'
    },
    getDutyText(type) {
      const map = { main: '主监考', deputy: '副监考', assistant: '监考助理', vice: '副监考' }
      return map[type] || type || '监考'
    },
    getDutyClass(type) {
      if (type === 'main') return 'duty-main'
      if (type === 'deputy' || type === 'vice') return 'duty-deputy'
      return 'duty-other'
    },
    getStatusText(status) {
      const map = {
        0: '待监考', 1: '已确认', 2: '已完成', 3: '已缺勤',
        pending: '待监考', confirmed: '已确认', finished: '已完成', absent: '已缺勤'
      }
      return map[status] || (typeof status === 'string' ? status : '待监考')
    },
    getStatusClass(status) {
      if (status === 2 || status === 'finished') return 'status-finished'
      if (status === 3 || status === 'absent') return 'status-absent'
      if (status === 1 || status === 'confirmed') return 'status-confirmed'
      return 'status-pending'
    },
    getDay(dateStr) {
      if (!dateStr) return '--'
      const parts = String(dateStr).split('-')
      return parts.length === 3 ? parts[2] : '--'
    },
    getMonth(dateStr) {
      if (!dateStr) return ''
      const parts = String(dateStr).split('-')
      return parts.length === 3 ? parts[1] + '月' : ''
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
    }
  }
}
</script>

<style scoped>
.mobile-invigilation {
  background: #f5f7fa;
  min-height: 100%;
}

.invigilation-list { padding: 12px; }
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

.invigilation-card {
  display: flex;
  background: #fff;
  border-radius: 10px;
  margin-bottom: 10px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.card-date-col {
  width: 58px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #1a5276, #2e86c1);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 10px 0;
}
.date-day { font-size: 22px; font-weight: 700; line-height: 1.1; }
.date-month { font-size: 11px; opacity: 0.9; margin-top: 2px; }

.card-body {
  flex: 1;
  padding: 12px;
  min-width: 0;
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.course-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  flex: 1;
  margin-right: 8px;
  word-break: break-all;
}
.duty-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  white-space: nowrap;
  flex-shrink: 0;
}
.duty-main { background: #ecf5ff; color: #2e86c1; }
.duty-deputy { background: #f0f9eb; color: #67c23a; }
.duty-other { background: #f4f4f5; color: #909399; }

.card-info {
  background: #fafbfc;
  border-radius: 8px;
  padding: 6px 12px;
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
  width: 44px;
  flex-shrink: 0;
}
.info-value {
  color: #303133;
  flex: 1;
  word-break: break-all;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}
.status-tag {
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 10px;
}
.status-pending { background: #fdf6ec; color: #e6a23c; }
.status-confirmed { background: #ecf5ff; color: #2e86c1; }
.status-finished { background: #f0f9eb; color: #67c23a; }
.status-absent { background: #fef0f0; color: #f56c6c; }
.exam-date-full { font-size: 11px; color: #c0c4cc; }

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
