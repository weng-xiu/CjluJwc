<template>
  <div
    class="mobile-exam"
    @touchstart="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
  >
    <!-- 下拉刷新提示（Vue3 迁移：字体图标 → el-icon 组件） -->
    <div class="pull-refresh" :style="{ height: pullDistance + 'px' }">
      <el-icon v-if="refreshing" class="is-loading"><Loading /></el-icon>
      <el-icon v-else-if="pullDistance >= triggerDistance"><ArrowDown /></el-icon>
      <el-icon v-else class="rotate"><ArrowDown /></el-icon>
      <span>{{ refreshText }}</span>
    </div>

    <!-- 类型筛选 -->
    <div class="filter-bar">
      <div
        v-for="opt in typeOptions"
        :key="String(opt.value)"
        class="filter-item"
        :class="{ active: queryParams.examType === opt.value }"
        @click="switchType(opt.value)"
      >{{ opt.label }}</div>
    </div>

    <!-- 考试卡片列表 -->
    <div class="exam-list">
      <div v-if="loading && list.length === 0" class="loading-state">
        <el-icon class="is-loading"><Loading /></el-icon> 加载中...
      </div>
      <div v-else-if="list.length === 0" class="empty-state">
        <el-icon><Tickets /></el-icon>
        <p>暂无考试安排</p>
      </div>

      <div v-for="item in list" :key="item.examId" class="exam-card">
        <div class="card-top">
          <span class="exam-name">{{ item.examName || '考试安排' }}</span>
          <span class="exam-type-tag" :class="getTypeClass(item.examType)">
            {{ getTypeText(item.examType) }}
          </span>
        </div>
        <div class="card-info">
          <div class="info-row">
            <el-icon><Calendar /></el-icon>
            <span class="info-label">考试日期</span>
            <span class="info-value">{{ item.examDate || '--' }}</span>
          </div>
          <div class="info-row">
            <el-icon><Clock /></el-icon>
            <span class="info-label">考试时间</span>
            <span class="info-value">{{ formatTime(item) }}</span>
          </div>
          <div class="info-row" v-if="item.duration != null">
            <el-icon><Timer /></el-icon>
            <span class="info-label">考试时长</span>
            <span class="info-value">{{ item.duration }} 分钟</span>
          </div>
          <div class="info-row" v-if="item.totalStudents != null">
            <el-icon><User /></el-icon>
            <span class="info-label">考生人数</span>
            <span class="info-value">{{ item.totalStudents }} 人</span>
          </div>
        </div>
        <div class="card-status" v-if="item.planStatus != null && item.planStatus !== ''">
          <span class="status-dot" :class="getStatusClass(item.planStatus)"></span>
          {{ getStatusText(item.planStatus) }}
        </div>
      </div>
    </div>

    <!-- 底部状态 -->
    <div class="list-footer">
      <span v-if="loading && list.length > 0"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</span>
      <span v-else-if="finished">没有更多了</span>
      <span v-else-if="list.length > 0" class="load-more" @click="loadMore">点击加载更多</span>
    </div>
  </div>
</template>

<script>
import { listExams } from '@/api/portal/exam'
import mobileList from '@/mixins/mobileList'

export default {
  name: 'MobileExam',
  mixins: [mobileList],
  data() {
    return {
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        examType: null
      },
      // examType 字典：0期末考试 1补考 2重修考试
      typeOptions: [
        { label: '全部', value: null },
        { label: '期末考试', value: '0' },
        { label: '补考', value: '1' },
        { label: '重修考试', value: '2' }
      ]
    }
  },
  methods: {
    fetchList() {
      return listExams(this.queryParams)
    },
    switchType(value) {
      if (this.queryParams.examType === value) return
      this.queryParams.examType = value
      this.resetQuery()
    },
    formatTime(item) {
      if (item.startTime || item.endTime) {
        return (item.startTime || '') + (item.endTime ? ' ~ ' + item.endTime : '')
      }
      return '--'
    },
    getTypeText(type) {
      const map = { 0: '期末考试', 1: '补考', 2: '重修考试' }
      return map[type] || type || '考试'
    },
    getTypeClass(type) {
      if (type === '0') return 'type-final'
      if (type === '1') return 'type-makeup'
      if (type === '2') return 'type-retake'
      return 'type-default'
    },
    // planStatus 字典：0未安排 1已安排 2已发布
    getStatusText(status) {
      const map = { 0: '待安排', 1: '已安排', 2: '已发布' }
      return map[status] || ''
    },
    getStatusClass(status) {
      if (status === '2') return 'status-done'
      if (status === '0') return 'status-pending'
      return 'status-arranged'
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
.pull-refresh .el-icon {
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
  background: linear-gradient(135deg, #003366, #007ab8);
}

/* 列表 */
.exam-list { padding: 12px; }
.loading-state, .empty-state {
  text-align: center;
  color: #c0c4cc;
  padding: 40px 0;
  font-size: 14px;
}
.loading-state .el-icon, .empty-state .el-icon {
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
.type-final { background: #e8f4fa; color: #007ab8; }
.type-makeup { background: #fdf6ec; color: #e6a23c; }
.type-retake { background: #f0f9eb; color: #67c23a; }
.type-default { background: #f4f4f5; color: #909399; }

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
.info-row .el-icon {
  color: #007ab8;
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
.status-arranged { background: #007ab8; }
.status-pending { background: #e6a23c; }
.status-done { background: #67c23a; }

/* 底部 */
.list-footer {
  text-align: center;
  padding: 16px;
  color: #c0c4cc;
  font-size: 13px;
}
.list-footer .el-icon { margin-right: 4px; }
.load-more {
  color: #007ab8;
  cursor: pointer;
}
</style>
