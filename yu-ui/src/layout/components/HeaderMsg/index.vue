<template>
  <el-dropdown class="msg-trigger" trigger="click" @command="onCommand">
    <div class="right-menu-item hover-effect msg-inner">
      <svg-icon icon-class="message" />
      <span v-if="total > 0" class="msg-badge">{{ total > 99 ? '99+' : total }}</span>
    </div>
    <el-dropdown-menu slot="dropdown">
      <el-dropdown-item command="message">
        <i class="el-icon-chat-dot-round" /> 我的消息
        <span v-if="unread > 0" class="msg-count">（未读 {{ unread }}）</span>
      </el-dropdown-item>
      <el-dropdown-item command="todo">
        <i class="el-icon-finished" /> 我的待办
        <span v-if="pending > 0" class="msg-count">（待办 {{ pending }}）</span>
      </el-dropdown-item>
    </el-dropdown-menu>
  </el-dropdown>
</template>

<script>
import { getUnreadCount, getPendingCount } from '@/api/system/msgCenter'

export default {
  name: 'HeaderMsg',
  data() {
    return { unread: 0, pending: 0, timer: null }
  },
  computed: {
    total() { return this.unread + this.pending }
  },
  mounted() {
    this.refresh()
    // 每 60 秒轮询一次未读/待办数量
    this.timer = setInterval(this.refresh, 60000)
  },
  beforeDestroy() {
    if (this.timer) clearInterval(this.timer)
  },
  methods: {
    refresh() {
      getUnreadCount().then(res => { this.unread = res.data || 0 }).catch(() => {})
      getPendingCount().then(res => { this.pending = res.data || 0 }).catch(() => {})
    },
    onCommand(cmd) {
      const path = cmd === 'message' ? '/msgcenter/message' : '/msgcenter/todo'
      this.$router.push(path).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.msg-trigger {
  .msg-inner {
    position: relative;
    display: inline-block;
    line-height: 50px;
  }
  .svg-icon { width: 1.2em; height: 1.2em; vertical-align: -0.2em; }
  .msg-badge {
    position: absolute;
    top: 8px;
    right: 2px;
    background: #f56c6c;
    color: #fff;
    border-radius: 10px;
    font-size: 10px;
    height: 16px;
    line-height: 16px;
    padding: 0 4px;
    min-width: 16px;
    text-align: center;
    white-space: nowrap;
    pointer-events: none;
  }
  .msg-count { color: #f56c6c; font-size: 12px; margin-left: 4px; }
}
</style>
