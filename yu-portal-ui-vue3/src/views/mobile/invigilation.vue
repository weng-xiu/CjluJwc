<template>
  <div
    class="mobile-invigilation"
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

    <!-- 职责筛选 -->
    <div class="filter-bar">
      <div
        v-for="opt in dutyOptions"
        :key="String(opt.value)"
        class="filter-item"
        :class="{ active: queryParams.dutyType === opt.value }"
        @click="switchDuty(opt.value)"
      >{{ opt.label }}</div>
    </div>

    <!-- 监考卡片列表 -->
    <div class="invigilation-list">
      <div v-if="loading && list.length === 0" class="loading-state">
        <el-icon class="is-loading"><Loading /></el-icon> 加载中...
      </div>
      <div v-else-if="list.length === 0" class="empty-state">
        <el-icon><View /></el-icon>
        <p>暂无监考安排</p>
      </div>

      <div v-for="item in list" :key="item.invigilationId" class="invigilation-card">
        <div class="card-date-col">
          <div class="date-day">{{ getDay(item.examDate) }}</div>
          <div class="date-month">{{ getMonth(item.examDate) }}</div>
        </div>
        <div class="card-body">
          <div class="card-top">
            <span class="course-name">{{ item.examName || '监考任务' }}</span>
            <span class="duty-tag" :class="getDutyClass(item.dutyType)">
              {{ getDutyText(item.dutyType) }}
            </span>
          </div>
          <div class="card-info">
            <div class="info-row">
              <el-icon><Clock /></el-icon>
              <span class="info-label">时段</span>
              <span class="info-value">{{ formatTime(item) }}</span>
            </div>
            <div class="info-row">
              <el-icon><Location /></el-icon>
              <span class="info-label">教室</span>
              <span class="info-value">{{ item.classroomName || '--' }}</span>
            </div>
            <div class="info-row" v-if="item.remark">
              <el-icon><Document /></el-icon>
              <span class="info-label">备注</span>
              <span class="info-value">{{ item.remark }}</span>
            </div>
          </div>
          <div class="card-footer">
            <span class="exam-date-full">{{ item.examDate || '--' }}</span>
          </div>
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
import { listInvigilations } from '@/api/portal/invigilation'
import mobileList from '@/mixins/mobileList'

export default {
  name: 'MobileInvigilation',
  mixins: [mobileList],
  data() {
    return {
      queryParams: { pageNum: 1, pageSize: 10, dutyType: null },
      // dutyType 字典：0主监考 1副监考 2巡考
      dutyOptions: [
        { label: '全部', value: null },
        { label: '主监考', value: '0' },
        { label: '副监考', value: '1' },
        { label: '巡考', value: '2' }
      ]
    }
  },
  methods: {
    fetchList() {
      return listInvigilations(this.queryParams)
    },
    switchDuty(value) {
      if (this.queryParams.dutyType === value) return
      this.queryParams.dutyType = value
      this.resetQuery()
    },
    formatTime(item) {
      if (item.startTime || item.endTime) {
        return (item.startTime || '') + (item.endTime ? ' ~ ' + item.endTime : '')
      }
      return '--'
    },
    getDutyText(type) {
      const map = { 0: '主监考', 1: '副监考', 2: '巡考' }
      return map[type] || '监考'
    },
    getDutyClass(type) {
      if (type === '0') return 'duty-main'
      if (type === '1') return 'duty-deputy'
      return 'duty-other'
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
    }
  }
}
</script>

<style scoped>
.mobile-invigilation {
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

.invigilation-list { padding: 12px; }
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
  background: linear-gradient(135deg, #003366, #007ab8);
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
.duty-main { background: #e8f4fa; color: #007ab8; }
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
.info-row .el-icon {
  color: #007ab8;
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
  justify-content: flex-end;
  align-items: center;
  margin-top: 10px;
}
.exam-date-full { font-size: 11px; color: #c0c4cc; }

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
