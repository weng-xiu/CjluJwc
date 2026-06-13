<template>
  <div class="portal-home">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <h2>欢迎使用师生互动服务门户</h2>
      <p>{{ greeting }}, {{ userName }}！今天是 {{ today }}，祝您工作学习愉快！</p>
    </div>
    <!-- 快捷入口 -->
    <div class="quick-entries">
      <h3 class="section-title">快捷入口</h3>
      <el-row :gutter="16">
        <template v-if="activeRole === 'student'">
          <el-col :span="6" v-for="item in studentMenus" :key="item.path">
            <div class="entry-card" @click="$router.push(item.path)">
              <i :class="item.icon"></i>
              <span>{{ item.name }}</span>
            </div>
          </el-col>
        </template>
        <template v-else>
          <el-col :span="6" v-for="item in teacherMenus" :key="item.path">
            <div class="entry-card" @click="$router.push(item.path)">
              <i :class="item.icon"></i>
              <span>{{ item.name }}</span>
            </div>
          </el-col>
        </template>
      </el-row>
    </div>
    <!-- 最新通知 -->
    <div class="notice-section">
      <h3 class="section-title">最新教务通知 <span class="more-link" @click="$router.push('/notice')">查看更多 →</span></h3>
      <el-table :data="noticeList" style="width:100%" v-loading="noticeLoading" :show-header="false">
        <el-table-column prop="noticeTitle" show-overflow-tooltip />
        <el-table-column prop="publishDate" width="120" align="center" />
      </el-table>
    </div>
  </div>
</template>

<script>
import { listNotice } from '@/api/portal/notice'

export default {
  name: 'PortalHome',
  data() {
    return {
      noticeList: [], noticeLoading: false,
      studentMenus: [
        { name: '课表查询', path: '/schedule', icon: 'el-icon-date' },
        { name: '成绩查询', path: '/grade', icon: 'el-icon-document' },
        { name: '选课中心', path: '/selection', icon: 'el-icon-edit-outline' },
        { name: '考试安排', path: '/exam', icon: 'el-icon-tickets' }
      ],
      teacherMenus: [
        { name: '个人课表', path: '/teacherSchedule', icon: 'el-icon-date' },
        { name: '成绩录入', path: '/gradeEntry', icon: 'el-icon-edit' },
        { name: '教学任务查询', path: '/teachingTask', icon: 'el-icon-notebook-2' },
        { name: '监考安排', path: '/invigilation', icon: 'el-icon-view' }
      ]
    }
  },
  computed: {
    userName() { return this.$store.state.user.nickName || this.$store.state.user.name || '用户' },
    activeRole() { return this.$store.state.user.userCategory || 'student' },
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px; padding: 32px; color: #fff; margin-bottom: 24px;
}
.welcome-banner h2 { margin: 0 0 8px; font-size: 22px; }
.welcome-banner p { margin: 0; font-size: 14px; opacity: 0.9; }
.section-title { font-size: 16px; color: #303133; margin: 0 0 16px; padding-bottom: 8px; border-bottom: 2px solid #2e86c1; display: flex; justify-content: space-between; align-items: center; }
.more-link { font-size: 13px; color: #2e86c1; cursor: pointer; font-weight: normal; }
.entry-card {
  background: #fff; border-radius: 8px; padding: 28px 16px; text-align: center;
  cursor: pointer; transition: all 0.3s; box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.entry-card:hover { transform: translateY(-4px); box-shadow: 0 6px 20px rgba(0,0,0,0.1); }
.entry-card i { font-size: 36px; color: #2e86c1; display: block; margin-bottom: 12px; }
.entry-card span { font-size: 14px; color: #606266; }
.notice-section { margin-top: 24px; background: #fff; border-radius: 8px; padding: 20px; }
</style>
