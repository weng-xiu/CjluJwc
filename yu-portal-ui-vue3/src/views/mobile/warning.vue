<template>
  <div class="mobile-warning">
    <!-- 预警统计 -->
    <div class="stats-bar">
      <div class="stat-item stat-danger">
        <span class="stat-count">{{ statCounts.high || 0 }}</span>
        <span class="stat-label">高危</span>
      </div>
      <div class="stat-item stat-warning">
        <span class="stat-count">{{ statCounts.medium || 0 }}</span>
        <span class="stat-label">严重</span>
      </div>
      <div class="stat-item stat-info">
        <span class="stat-count">{{ statCounts.low || 0 }}</span>
        <span class="stat-label">一般</span>
      </div>
    </div>

    <!-- 预警列表（Vue3 迁移：字体图标 → el-icon；el-icon-loading → Loading 组件） -->
    <div class="warning-list">
      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading"><Loading /></el-icon> 加载中...
      </div>
      <div v-else-if="warningList.length === 0" class="empty-state">
        <el-icon><SuccessFilled /></el-icon>
        <p>暂无预警，学业正常</p>
      </div>

      <div
        v-for="item in warningList"
        :key="item.warningId"
        class="warning-card"
        :class="getLevelClass(item)"
      >
        <div class="card-level-bar"></div>
        <div class="card-content">
          <div class="card-top">
            <span class="card-type">{{ getTypeText(item) }}</span>
            <span class="card-level-tag" :class="getLevelTagClass(item)">
              {{ getLevelText(item) }}
            </span>
          </div>
          <div class="card-reason">{{ item.warningReason || '系统检测到学业异常' }}</div>
          <div class="card-footer">
            <span class="card-date"><el-icon><Calendar /></el-icon> {{ fmtDate(item.warningDate || item.createTime) }}</span>
            <span class="card-status" :class="{ read: item.isResolved === '1' }">
              {{ item.isResolved === '1' ? '已解除' : '未解除' }}
            </span>
          </div>
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
import { getMyWarnings, getWarningStatistics } from '@/api/mobile'

export default {
  name: 'MobileWarning',
  data() {
    return {
      loading: false,
      warningList: [],
      statCounts: { high: 0, medium: 0, low: 0 },
      pageNum: 1,
      pageSize: 20,
      total: 0
    }
  },
  computed: {
    hasMore() {
      return this.warningList.length < this.total
    }
  },
  mounted() {
    this.loadStatistics()
    this.loadWarnings()
  },
  methods: {
    loadStatistics() {
      getWarningStatistics().then(r => {
        const data = r.data || {}
        // 后端返回字段：highRiskCount(高危) / seriousCount(严重) / normalCount(一般)
        this.statCounts = {
          high: data.highRiskCount || data.dangerCount || 0,
          medium: data.seriousCount || data.mediumRiskCount || data.warningCount || 0,
          low: data.normalCount || data.lowRiskCount || data.infoCount || 0
        }
      }).catch(() => {})
    },
    loadWarnings() {
      this.loading = true
      getMyWarnings({ pageNum: this.pageNum, pageSize: this.pageSize }).then(r => {
        const rows = r.rows || r.data || []
        this.warningList = this.warningList.concat(rows)
        this.total = r.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    loadMore() {
      this.pageNum++
      this.loadWarnings()
    },
    getLevel(item) {
      // warning_level: 0一般 1严重 2高危
      return item.warningLevel
    },
    getTypeText(item) {
      const map = { '0': 'GPA预警', '1': '学分预警', '2': '出勤预警', '3': '综合预警' }
      return map[item.warningType] || '学业预警'
    },
    fmtDate(v) {
      if (!v) return '--'
      return String(v).substring(0, 10)
    },
    getLevelClass(item) {
      const level = this.getLevel(item)
      if (level === '2') return 'card-high'
      if (level === '1') return 'card-medium'
      return 'card-low'
    },
    getLevelTagClass(item) {
      const level = this.getLevel(item)
      if (level === '2') return 'tag-high'
      if (level === '1') return 'tag-medium'
      return 'tag-low'
    },
    getLevelText(item) {
      const level = this.getLevel(item)
      if (level === '2') return '高危'
      if (level === '1') return '严重'
      return '一般'
    }
  }
}
</script>

<style scoped>
.mobile-warning {
  background: #f5f7fa;
  min-height: 100%;
}

/* 统计栏 */
.stats-bar {
  display: flex;
  background: #fff;
  padding: 16px;
  gap: 12px;
  border-bottom: 1px solid #ebeef5;
}
.stat-item {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  border-radius: 8px;
}
.stat-count { display: block; font-size: 22px; font-weight: 700; }
.stat-label { font-size: 11px; }
.stat-danger { background: #fef0f0; color: #f56c6c; }
.stat-warning { background: #fdf6ec; color: #e6a23c; }
.stat-info { background: #fdf6ec; color: #f0c78a; }

/* 预警列表 */
.warning-list { padding: 12px; }
.loading-state, .empty-state {
  text-align: center;
  color: #c0c4cc;
  padding: 40px 0;
  font-size: 14px;
}
.loading-state .el-icon, .empty-state .el-icon { font-size: 32px; display: block; margin: 0 auto 8px; }
.empty-state .el-icon { color: #67c23a; }

.warning-card {
  display: flex;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 10px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.card-level-bar {
  width: 4px;
  flex-shrink: 0;
}
.card-high .card-level-bar { background: #f56c6c; }
.card-medium .card-level-bar { background: #e6a23c; }
.card-low .card-level-bar { background: #f0c78a; }

.card-content {
  flex: 1;
  padding: 12px;
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.card-type { font-size: 15px; font-weight: 600; color: #303133; }
.card-level-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
}
.tag-high { background: #fef0f0; color: #f56c6c; }
.tag-medium { background: #fdf6ec; color: #e6a23c; }
.tag-low { background: #fef9e7; color: #d4a017; }

.card-reason {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  margin-bottom: 10px;
}
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-date { font-size: 11px; color: #909399; }
.card-date .el-icon { margin-right: 2px; vertical-align: middle; }
.card-status {
  font-size: 11px;
  color: #f56c6c;
  font-weight: 500;
}
.card-status.read { color: #909399; font-weight: normal; }

/* 加载更多 */
.load-more {
  text-align: center;
  padding: 16px;
  color: #007ab8;
  font-size: 13px;
  cursor: pointer;
}
</style>
