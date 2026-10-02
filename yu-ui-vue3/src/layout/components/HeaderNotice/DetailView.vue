<template>
  <el-drawer v-model="visible" title="公告详情" direction="rtl" size="50%" append-to-body @close="handleClose">
    <div v-loading="loading" class="notice-detail-drawer__body">
      <div v-if="!detail" class="notice-empty">
        <el-icon class="empty-icon"><Document /></el-icon>
        <span>暂无数据</span>
      </div>
      <div v-else class="notice-page">
        <div class="notice-type-wrap">
          <span v-if="detail.noticeType === '1'" class="notice-type-tag type-notify">
            <el-icon><Bell /></el-icon> 通知
          </span>
          <span v-else-if="detail.noticeType === '2'" class="notice-type-tag type-announce">
            <el-icon><Message /></el-icon> 公告
          </span>
          <span v-else class="notice-type-tag type-notify">
            <el-icon><Document /></el-icon> 消息
          </span>
        </div>

        <h1 class="notice-title">{{ detail.noticeTitle }}</h1>

        <div class="notice-meta">
          <span class="meta-item">
            <el-icon><User /></el-icon>
            <span>{{ detail.createBy || '—' }}</span>
          </span>
          <span class="meta-item">
            <el-icon><Clock /></el-icon>
            <span>{{ detail.createTime || '—' }}</span>
          </span>
          <span class="meta-item">
            <span :class="['status-dot', isStatusNormal ? 'status-ok' : 'status-off']"></span>
            <span>{{ isStatusNormal ? '正常' : '已关闭' }}</span>
          </span>
        </div>

        <div class="notice-divider">
          <span class="notice-divider-dot"></span>
          <span class="notice-divider-dot"></span>
          <span class="notice-divider-dot"></span>
        </div>

        <div class="notice-body">
          <div v-if="hasContent" class="notice-content" v-html="detail.noticeContent" />
          <div v-else class="notice-empty notice-empty--inner">
            <el-icon class="empty-icon"><Document /></el-icon> 暂无内容
          </div>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script>
// Vue3 迁移：el-drawer :visible.sync → v-model（custom-class/:before-close 移除，改 @close 清理）；
// el-icon-* 字体类 → <el-icon><Xxx/></el-icon> 组件；::v-deep → :deep()。展示逻辑与 Vue2 一致。
import { Document, Bell, Message, User, Clock } from '@element-plus/icons-vue'
import { getNotice } from '@/api/system/notice'

export default {
  name: 'NoticeDetailView',
  components: { Document, Bell, Message, User, Clock },
  data() {
    return {
      visible: false,
      loading: false,
      detail: null
    }
  },
  computed: {
    isStatusNormal() {
      const s = this.detail && this.detail.status
      return s === '0' || s === 0
    },
    hasContent() {
      const c = this.detail && this.detail.noticeContent
      return c != null && String(c).trim() !== ''
    }
  },
  methods: {
    open(payload) {
      let id = null
      let preset = null
      if (payload != null && typeof payload === 'object') {
        id = payload.noticeId
        if (payload.noticeContent != null) {
          preset = payload
        }
      } else {
        id = payload
      }
      this.visible = true
      if (preset) {
        this.detail = preset
        return
      }
      if (id == null || id === '') {
        this.detail = null
        return
      }
      this.loading = true
      this.detail = null
      getNotice(id).then(res => {
        this.detail = res.data
      }).catch(() => {
        this.detail = null
      }).finally(() => {
        this.loading = false
      })
    },
    handleClose() {
      this.visible = false
      this.detail = null
      this.loading = false
    }
  }
}
</script>

<style lang="scss" scoped>
.notice-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 8px 8px 20px;
  animation: notice-fade-up 0.28s ease both;
}

@keyframes notice-fade-up {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.notice-type-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 12px;
  border-radius: 2px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1px;
  text-transform: uppercase;
  margin-bottom: 14px;
}

.type-notify {
  background: var(--el-color-warning-light-9);
  color: #b7791f;
  border-left: 3px solid var(--el-color-warning);
}

.type-announce {
  background: var(--el-color-success-light-9);
  color: #276749;
  border-left: 3px solid var(--el-color-success);
}

.notice-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--dt-text-primary);
  line-height: 1.45;
  margin: 0 0 16px;
  letter-spacing: -0.2px;
}

.notice-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  padding: 12px 0;
  border-top: 1px solid var(--dt-border-color-light);
  border-bottom: 1px solid var(--dt-border-color-light);
  margin-bottom: 28px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--dt-text-secondary);

  .el-icon {
    font-size: 12px;
    color: var(--dt-text-placeholder);
  }
}

.status-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 4px;
}

.status-ok {
  background: var(--el-color-success);
}

.status-off {
  background: var(--el-color-danger);
}

.notice-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.notice-divider::before,
.notice-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: linear-gradient(to right, transparent, #dee2e6, transparent);
}

.notice-divider-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--dt-border-color);
}

.notice-body {
  background: var(--dt-bg-container);
  border-radius: 6px;
  padding: 28px 32px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06), 0 0 0 1px rgba(0, 0, 0, 0.04);
  min-height: 120px;
}

.notice-content {
  font-size: 14px;
  line-height: 1.85;
  color: var(--dt-text-primary);
  word-break: break-word;
}

.notice-content :deep(p) {
  margin: 0 0 1em;
}

.notice-content :deep(h1),
.notice-content :deep(h2),
.notice-content :deep(h3) {
  font-weight: 700;
  color: var(--dt-text-primary);
  margin: 1.4em 0 0.6em;
}

.notice-content :deep(h1) {
  font-size: 18px;
}

.notice-content :deep(h2) {
  font-size: 16px;
}

.notice-content :deep(h3) {
  font-size: 14px;
}

.notice-content :deep(a) {
  color: var(--el-color-primary);
  text-decoration: underline;
}

.notice-content :deep(a:hover) {
  color: var(--el-color-primary);
}

.notice-content :deep(img) {
  max-width: 100%;
  border-radius: 4px;
  margin: 8px 0;
}

.notice-content :deep(ul),
.notice-content :deep(ol) {
  padding-left: 20px;
  margin: 0 0 1em;
}

.notice-content :deep(li) {
  margin-bottom: 4px;
}

.notice-content :deep(blockquote) {
  border-left: 3px solid var(--dt-border-color);
  margin: 1em 0;
  padding: 6px 16px;
  color: var(--dt-text-secondary);
  background: var(--dt-fill-light);
}

.notice-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 1em 0;
  font-size: 13px;
}

.notice-content :deep(table th),
.notice-content :deep(table td) {
  border: 1px solid var(--dt-border-color-light);
  padding: 7px 12px;
}

.notice-content :deep(table th) {
  background: var(--dt-fill-light);
  font-weight: 600;
}

.notice-empty {
  text-align: center;
  padding: 40px 0;
  color: var(--dt-text-placeholder);
  font-size: 13px;
}

.notice-empty .empty-icon {
  font-size: 28px;
  display: block;
  margin-bottom: 10px;
}

.notice-empty--inner {
  padding: 32px 0;
}

.notice-empty--inner .empty-icon {
  font-size: 28px;
}

.notice-detail-drawer__body {
  height: 100%;
  overflow: auto;
  padding: 10px 16px 22px;
}
</style>
