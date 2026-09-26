<template>
  <div class="portal-home">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <h2>欢迎使用师生互动服务门户</h2>
      <p>{{ greeting }}, {{ userName }}！今天是 {{ today }}，祝您工作学习愉快！</p>
    </div>
    <!-- 快捷入口（基于后端权限动态渲染） -->
    <div class="quick-entries" v-if="quickEntries.length > 0">
      <h3 class="section-title">快捷入口</h3>
      <el-row :gutter="16">
        <el-col :span="6" v-for="item in quickEntries" :key="item.path">
          <div class="entry-card" @click="$router.push(item.path)">
            <!-- 迁移：动态类名图标 → 按配置组件名渲染图标组件（el-icon 根节点仍是 <i>，下方 .entry-card i 样式继续生效） -->
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </div>
        </el-col>
      </el-row>
    </div>
    <!-- 最新通知 -->
    <div class="notice-section">
      <h3 class="section-title">最新教务通知 <span class="more-link" v-if="canViewNotice" @click="$router.push('/notice')">查看更多 →</span></h3>
      <!-- v-loading 指令在 element-plus 由 app.use(ElementPlus) 全局注册 -->
      <el-table :data="noticeList" style="width:100%" v-loading="noticeLoading" :show-header="false">
        <el-table-column prop="noticeTitle" show-overflow-tooltip />
        <el-table-column prop="publishDate" width="120" align="center" />
      </el-table>
    </div>
  </div>
</template>

<script>
import { listNotice } from '@/api/portal/notice'
import { getQuickEntries, hasPermission } from '@/utils/permission'

export default {
  name: 'PortalHome',
  data() {
    return {
      noticeList: [], noticeLoading: false
    }
  },
  computed: {
    userName() { return this.$store.state.user.nickName || this.$store.state.user.name || '用户' },
    quickEntries() { return getQuickEntries() },
    canViewNotice() { return hasPermission('portal:notice:list') },
    greeting() {
      const h = new Date().getHours()
      return h < 6 ? '夜深了' : h < 12 ? '上午好' : h < 14 ? '中午好' : h < 18 ? '下午好' : '晚上好'
    },
    today() {
      return new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
    }
  },
  created() {
    this.fetchNotices()
  },
  methods: {
    fetchNotices() {
      this.noticeLoading = true
      listNotice({ pageNum: 1, pageSize: 5 }).then(res => {
        this.noticeList = res.rows || []
      }).finally(() => { this.noticeLoading = false })
    }
  }
}
</script>

<style scoped>
.portal-home { max-width: 1200px; margin: 0 auto; }
.welcome-banner {
  background: linear-gradient(135deg, #007ab8 0%, #008ed6 100%);
  border-radius: 8px; padding: 32px; color: #fff; margin-bottom: 24px;
}
.welcome-banner h2 { margin: 0 0 8px; font-size: 22px; }
.welcome-banner p { margin: 0; font-size: 14px; opacity: 0.9; }
.section-title { font-size: 16px; color: #303133; margin: 0 0 16px; padding-bottom: 8px; border-bottom: 2px solid #007ab8; display: flex; justify-content: space-between; align-items: center; }
.more-link { font-size: 13px; color: #007ab8; cursor: pointer; font-weight: normal; }
.entry-card {
  background: #fff; border-radius: 8px; padding: 28px 16px; text-align: center;
  cursor: pointer; transition: all 0.3s; box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.entry-card:hover { transform: translateY(-4px); box-shadow: 0 6px 20px rgba(0,0,0,0.1); }
.entry-card i { font-size: 36px; color: #007ab8; display: block; margin-bottom: 12px; }
.entry-card span { font-size: 14px; color: #606266; }
.notice-section { margin-top: 24px; background: #fff; border-radius: 8px; padding: 20px; }
</style>
