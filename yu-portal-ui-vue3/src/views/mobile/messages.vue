<template>
  <div class="mobile-messages">
    <!-- 顶部切换 -->
    <div class="seg-bar">
      <div class="seg-item" :class="{ active: tab === 'msg' }" @click="switchTab('msg')">
        消息<span v-if="unread > 0" class="seg-badge">{{ unread > 99 ? '99+' : unread }}</span>
      </div>
      <div class="seg-item" :class="{ active: tab === 'todo' }" @click="switchTab('todo')">
        待办<span v-if="pending > 0" class="seg-badge">{{ pending > 99 ? '99+' : pending }}</span>
      </div>
    </div>

    <!-- 消息列表 -->
    <template v-if="tab === 'msg'">
      <div class="filter-bar">
        <span class="filter-item" :class="{ active: msgFilter === '0' }" @click="setMsgFilter('0')">未读</span>
        <span class="filter-item" :class="{ active: msgFilter === '' }" @click="setMsgFilter('')">全部</span>
        <el-button v-if="unread > 0" link type="primary" size="small" class="read-all" @click="readAll">全部已读</el-button>
      </div>
      <div v-if="loading" class="loading-state"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</div>
      <div v-else-if="list.length === 0" class="empty-state">
        <el-icon><ChatLineSquare /></el-icon>
        <p>{{ msgFilter === '0' ? '暂无未读消息' : '暂无消息' }}</p>
      </div>
      <div v-for="item in list" :key="item.messageId" class="msg-card" :class="{ unread: item.readStatus === '0' }" @click="openMsg(item)">
        <div class="msg-top">
          <span class="msg-title">{{ item.title }}</span>
          <span v-if="item.readStatus === '0'" class="unread-dot"></span>
        </div>
        <div class="msg-content">{{ preview(item.content) }}</div>
        <div class="msg-footer">
          <span class="msg-type">{{ msgTypeText(item.msgType) }}</span>
          <span class="msg-time">{{ fmt(item.createTime) }}</span>
        </div>
      </div>
    </template>

    <!-- 待办列表 -->
    <template v-else>
      <div v-if="loading" class="loading-state"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</div>
      <div v-else-if="list.length === 0" class="empty-state">
        <el-icon><CircleCheck /></el-icon>
        <p>暂无待办事项</p>
      </div>
      <div v-for="item in list" :key="item.todoId" class="todo-card">
        <div class="todo-main">
          <div class="todo-title">{{ item.title }}</div>
          <div class="todo-meta">
            <span class="todo-type">{{ bizTypeText(item.businessType) }}</span>
            <span class="todo-time">{{ fmt(item.createTime) }}</span>
          </div>
        </div>
        <el-button v-if="item.status === '0'" type="primary" size="small" plain @click="finishTodo(item)">办结</el-button>
        <span v-else class="todo-done"><el-icon><Check /></el-icon> 已办结</span>
      </div>
    </template>

    <div v-if="!loading && list.length < total" class="load-more" @click="loadMore">点击加载更多</div>

    <!-- 消息详情弹层 -->
    <div v-if="detail" class="msg-mask" @click.self="detail = null">
      <div class="msg-dialog">
        <div class="dialog-title">{{ detail.title }}</div>
        <div class="dialog-meta">{{ msgTypeText(detail.msgType) }} · {{ fmt(detail.createTime) }}</div>
        <div class="dialog-content">{{ detail.content }}</div>
        <div class="dialog-actions">
          <el-button type="primary" size="small" @click="detail = null">关闭</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { listMyMessages, markMessageRead, markAllMessagesRead, getUnreadCount, listMyTodos, completeTodo, getPendingCount } from '@/api/portal/msg'

export default {
  name: 'MobileMessages',
  data() {
    return {
      tab: 'msg',
      loading: false,
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 20,
      msgFilter: '0',
      unread: 0,
      pending: 0,
      detail: null
    }
  },
  mounted() {
    this.loadCounts()
    this.reload()
  },
  methods: {
    switchTab(t) {
      if (this.tab === t) return
      this.tab = t
      this.list = []
      this.total = 0
      this.reload()
    },
    setMsgFilter(v) {
      this.msgFilter = v
      this.list = []
      this.total = 0
      this.reload()
    },
    reload() {
      this.pageNum = 1
      this.fetch()
    },
    loadMore() {
      this.pageNum++
      this.fetch()
    },
    fetch() {
      this.loading = true
      const params = { pageNum: this.pageNum, pageSize: this.pageSize }
      const req = this.tab === 'msg'
        ? listMyMessages(this.msgFilter === '' ? params : Object.assign({ readStatus: this.msgFilter }, params))
        : listMyTodos(params)
      req.then(r => {
        const rows = r.rows || []
        this.list = this.pageNum === 1 ? rows : this.list.concat(rows)
        this.total = r.total || 0
      }).finally(() => { this.loading = false })
    },
    loadCounts() {
      getUnreadCount().then(r => { this.unread = r.data || 0 }).catch(() => {})
      getPendingCount().then(r => { this.pending = r.data || 0 }).catch(() => {})
    },
    openMsg(item) {
      this.detail = item
      if (item.readStatus === '0') {
        markMessageRead(item.messageId).then(() => {
          item.readStatus = '1'
          this.unread = Math.max(0, this.unread - 1)
        }).catch(() => {})
      }
    },
    readAll() {
      markAllMessagesRead().then(() => {
        this.modalMsg('已全部标记为已读')
        this.unread = 0
        this.reload()
      }).catch(() => {})
    },
    finishTodo(item) {
      completeTodo(item.todoId).then(() => {
        item.status = '1'
        this.$confirm('已办结。是否前往处理对应业务？', '提示', { confirmButtonText: '去处理', cancelButtonText: '留在本页', type: 'success' })
          .then(() => this.goBusiness(item))
          .catch(() => { this.loadCounts() })
      }).catch(() => {})
    },
    goBusiness(item) {
      const map = { scheduleAdjust: '/m/adjustment', borrow: '/m/borrow', statusChange: '/m/studentStatus' }
      this.$router.push(map[item.businessType] || '/m/adjustment')
    },
    preview(c) {
      if (!c) return ''
      return c.length > 60 ? c.substring(0, 60) + '...' : c
    },
    msgTypeText(t) {
      const map = { '0': '系统通知', '1': '预警提醒', '2': '审批消息', '3': '变更消息' }
      return map[t] || '通知'
    },
    bizTypeText(t) {
      const map = { scheduleAdjust: '调停课审批', borrow: '教室借用审批', statusChange: '学籍异动审批', warning: '学业预警' }
      return map[t] || (t || '业务待办')
    },
    fmt(v) {
      if (!v) return '--'
      return this.parseTime ? this.parseTime(v, '{y}-{m}-{d} {h}:{i}') : v
    },
    modalMsg(m) {
      this.$message ? this.$message.success(m) : window.alert(m)
    }
  }
}
</script>

<style scoped>
.mobile-messages { padding: 12px; }
.seg-bar { display: flex; background: #fff; border-radius: 8px; padding: 4px; margin-bottom: 10px; }
.seg-item { flex: 1; text-align: center; line-height: 32px; font-size: 14px; color: #606266; border-radius: 6px; cursor: pointer; position: relative; }
.seg-item.active { background: linear-gradient(135deg, #003366, #007ab8); color: #fff; }
.seg-badge { position: absolute; top: 2px; right: 18px; background: #e74c3c; color: #fff; font-size: 10px; min-width: 16px; height: 16px; line-height: 16px; border-radius: 8px; padding: 0 4px; }
.filter-bar { display: flex; align-items: center; padding: 0 4px 8px; }
.filter-item { font-size: 13px; color: #909399; margin-right: 16px; cursor: pointer; }
.filter-item.active { color: #007ab8; font-weight: 600; }
.read-all { margin-left: auto; }
.loading-state, .empty-state { text-align: center; color: #c0c4cc; padding: 40px 0; font-size: 13px; }
.empty-state .el-icon { font-size: 40px; display: block; margin-bottom: 8px; }
.msg-card { background: #fff; border-radius: 8px; padding: 12px; margin-bottom: 10px; position: relative; }
.msg-card.unread { border-left: 3px solid #007ab8; }
.msg-top { display: flex; align-items: center; }
.msg-title { font-size: 14px; font-weight: 600; color: #303133; flex: 1; }
.unread-dot { width: 8px; height: 8px; border-radius: 50%; background: #e74c3c; flex-shrink: 0; }
.msg-content { font-size: 13px; color: #606266; margin: 6px 0; }
.msg-footer { display: flex; justify-content: space-between; font-size: 12px; color: #909399; }
.todo-card { background: #fff; border-radius: 8px; padding: 12px; margin-bottom: 10px; display: flex; align-items: center; }
.todo-main { flex: 1; min-width: 0; }
.todo-title { font-size: 14px; color: #303133; font-weight: 500; margin-bottom: 4px; }
.todo-meta { font-size: 12px; color: #909399; display: flex; gap: 10px; }
.todo-type { color: #007ab8; }
.todo-done { font-size: 12px; color: #67c23a; white-space: nowrap; }
.todo-done .el-icon { vertical-align: middle; }
.load-more { text-align: center; font-size: 13px; color: #007ab8; padding: 12px 0 20px; cursor: pointer; }
.msg-mask { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.45); z-index: 999; display: flex; align-items: center; justify-content: center; }
.msg-dialog { width: 86%; max-height: 70vh; overflow-y: auto; background: #fff; border-radius: 10px; padding: 16px; }
.dialog-title { font-size: 16px; font-weight: 600; color: #303133; }
.dialog-meta { font-size: 12px; color: #909399; margin: 6px 0 10px; }
.dialog-content { font-size: 14px; color: #606266; line-height: 1.7; white-space: pre-wrap; }
.dialog-actions { text-align: right; margin-top: 14px; }
</style>
