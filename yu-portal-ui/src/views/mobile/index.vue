<template>
  <div class="mobile-home">
    <!-- 用户信息头部 -->
    <div class="user-banner">
      <div class="user-avatar">
        <i class="el-icon-user-solid"></i>
      </div>
      <div class="user-info">
        <div class="user-name">{{ userName }}</div>
        <div class="user-id">{{ userSub || '师生门户' }}</div>
      </div>
      <div class="user-role">
        <el-tag size="mini" type="info">{{ roleText }}</el-tag>
      </div>
    </div>

    <!-- 预警提醒横幅 -->
    <div v-if="warningCount > 0" class="warning-banner" @click="$router.push('/mobile/warning')">
      <i class="el-icon-warning-outline"></i>
      <span>您有 {{ warningCount }} 条未读预警，点击查看</span>
      <i class="el-icon-arrow-right"></i>
    </div>

    <!-- 宫格导航 -->
    <div class="grid-nav">
      <div class="grid-item" @click="$router.push('/mobile/schedule')">
        <div class="grid-icon icon-schedule">
          <i class="el-icon-date"></i>
        </div>
        <span class="grid-label">课表查询</span>
      </div>
      <div class="grid-item" @click="$router.push('/mobile/grades')">
        <div class="grid-icon icon-grades">
          <i class="el-icon-document"></i>
        </div>
        <span class="grid-label">成绩查询</span>
      </div>
      <div class="grid-item" @click="$router.push('/m/exam')">
        <div class="grid-icon icon-exam">
          <i class="el-icon-tickets"></i>
        </div>
        <span class="grid-label">考试安排</span>
      </div>
      <div class="grid-item" @click="$router.push('/m/invigilation')">
        <div class="grid-icon icon-invigilation">
          <i class="el-icon-view"></i>
        </div>
        <span class="grid-label">我的监考</span>
      </div>
      <div class="grid-item" @click="$router.push('/m/evaluation')">
        <div class="grid-icon icon-evaluation">
          <i class="el-icon-star-on"></i>
        </div>
        <span class="grid-label">教学评价</span>
      </div>
      <div class="grid-item" @click="$router.push('/mobile/selection')">
        <div class="grid-icon icon-selection">
          <i class="el-icon-edit-outline"></i>
        </div>
        <span class="grid-label">选课中心</span>
      </div>
      <div class="grid-item" @click="$router.push('/mobile/warning')">
        <div class="grid-icon icon-warning">
          <i class="el-icon-warning"></i>
          <span v-if="warningCount > 0" class="grid-badge">{{ warningCount }}</span>
        </div>
        <span class="grid-label">学业预警</span>
      </div>
      <!-- P5 办事入口（按角色展示） -->
      <div v-if="isStudent" class="grid-item" @click="$router.push('/m/studentStatus')">
        <div class="grid-icon icon-status">
          <i class="el-icon-postcard"></i>
        </div>
        <span class="grid-label">学籍服务</span>
      </div>
      <div v-if="isTeacher" class="grid-item" @click="$router.push('/m/adjustment')">
        <div class="grid-icon icon-adjust">
          <i class="el-icon-sort"></i>
          <span v-if="pendingTodo > 0" class="grid-badge">{{ pendingTodo }}</span>
        </div>
        <span class="grid-label">调停课</span>
      </div>
      <div class="grid-item" @click="$router.push('/m/borrow')">
        <div class="grid-icon icon-borrow">
          <i class="el-icon-office-building"></i>
        </div>
        <span class="grid-label">教室借用</span>
      </div>
      <div class="grid-item" @click="$router.push('/m/messages')">
        <div class="grid-icon icon-msg">
          <i class="el-icon-bell"></i>
          <span v-if="unreadMsg > 0" class="grid-badge">{{ unreadMsg > 99 ? '99+' : unreadMsg }}</span>
        </div>
        <span class="grid-label">消息待办</span>
      </div>
    </div>

    <!-- 快捷信息 -->
    <div class="quick-info">
      <el-card shadow="hover" class="info-card">
        <div slot="header" class="info-header">
          <i class="el-icon-time"></i> 今日课表
        </div>
        <div v-if="todaySchedule.length === 0" class="empty-tip">今天没有课程安排</div>
        <div v-else class="today-list">
          <div v-for="item in todaySchedule" :key="item.id" class="today-item">
            <span class="course-time">{{ item.startTime }}-{{ item.endTime }}</span>
            <span class="course-name">{{ item.courseName }}</span>
            <span class="course-room">{{ item.classroom }}</span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 切换PC版入口 -->
    <div class="switch-pc">
      <el-button type="text" size="small" @click="switchToPC">
        <i class="el-icon-monitor"></i> 切换到电脑版
      </el-button>
    </div>
  </div>
</template>

<script>
import { getMySchedule, getWarningStatistics } from '@/api/mobile'
import { getUnreadCount, getPendingCount } from '@/api/portal/msg'

export default {
  name: 'MobileHome',
  data() {
    return {
      warningCount: 0,
      todaySchedule: [],
      unreadMsg: 0,
      pendingTodo: 0
    }
  },
  computed: {
    roles() {
      return this.$store.state.user.roles || []
    },
    isStudent() {
      return this.roles.includes('student') || !this.roles.includes('teacher')
    },
    isTeacher() {
      return this.roles.includes('teacher')
    },
    roleText() {
      if (this.roles.includes('admin')) return '管理员'
      return this.isTeacher ? '教师' : '学生'
    },
    userName() {
      return this.$store.state.user.nickName || this.$store.state.user.name || '用户'
    },
    userSub() {
      return this.$store.state.user.userName || ''
    }
  },
  mounted() {
    this.loadWarningCount()
    this.loadTodaySchedule()
    this.loadMsgCounts()
  },
  methods: {
    loadMsgCounts() {
      getUnreadCount().then(r => { this.unreadMsg = r.data || 0 }).catch(() => {})
      getPendingCount().then(r => { this.pendingTodo = r.data || 0 }).catch(() => {})
    },
    loadWarningCount() {
      getWarningStatistics().then(r => {
        // 未解除预警数作为提醒角标；无则回退到总数
        this.warningCount = r.data?.unresolvedCount || r.data?.totalCount || 0
      }).catch(() => {})
    },
    loadTodaySchedule() {
      const today = new Date()
      const dateStr = today.getFullYear() + '-' +
        String(today.getMonth() + 1).padStart(2, '0') + '-' +
        String(today.getDate()).padStart(2, '0')
      getMySchedule({ date: dateStr }).then(r => {
        this.todaySchedule = r.rows || r.data || []
      }).catch(() => {})
    },
    switchToPC() {
      // 设置标记后跳转到PC版首页
      sessionStorage.setItem('preferPC', 'true')
      this.$router.push('/home')
    }
  }
}
</script>

<style scoped>
.mobile-home {
  padding: 0;
  min-height: 100%;
}

/* 用户信息横幅 */
.user-banner {
  display: flex;
  align-items: center;
  padding: 20px 16px;
  background: linear-gradient(135deg, #003366, #007ab8);
  color: #fff;
}
.user-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  margin-right: 14px;
  flex-shrink: 0;
}
.user-info { flex: 1; }
.user-name { font-size: 18px; font-weight: 600; margin-bottom: 4px; }
.user-id { font-size: 13px; opacity: 0.85; }

/* 预警提醒横幅 */
.warning-banner {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background: #fdf6ec;
  color: #e6a23c;
  font-size: 13px;
  border-bottom: 1px solid #faecd8;
  cursor: pointer;
}
.warning-banner i:first-child { font-size: 16px; margin-right: 8px; }
.warning-banner span { flex: 1; }
.warning-banner i:last-child { font-size: 14px; color: #c0c4cc; }

/* 宫格导航 */
.grid-nav {
  display: flex;
  flex-wrap: wrap;
  padding: 16px 12px 8px;
  background: #fff;
  margin-bottom: 12px;
}
.grid-item {
  width: 25%;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 12px;
  cursor: pointer;
}
.grid-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #fff;
  margin-bottom: 8px;
  position: relative;
}
.icon-schedule { background: linear-gradient(135deg, #409eff, #66b1ff); }
.icon-grades { background: linear-gradient(135deg, #67c23a, #85ce61); }
.icon-exam { background: linear-gradient(135deg, #003366, #007ab8); }
.icon-invigilation { background: linear-gradient(135deg, #8e44ad, #bb8fce); }
.icon-evaluation { background: linear-gradient(135deg, #e67e22, #f0b27a); }
.icon-selection { background: linear-gradient(135deg, #e6a23c, #f0c78a); }
.icon-warning { background: linear-gradient(135deg, #f56c6c, #f89898); }
.icon-status { background: linear-gradient(135deg, #16a085, #1abc9c); }
.icon-adjust { background: linear-gradient(135deg, #2c3e50, #4a6491); }
.icon-borrow { background: linear-gradient(135deg, #d35400, #e67e22); }
.icon-msg { background: linear-gradient(135deg, #8e44ad, #c39bd3); }
.grid-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #e74c3c;
  color: #fff;
  font-size: 10px;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  border-radius: 8px;
  padding: 0 4px;
}
.grid-label {
  font-size: 12px;
  color: #606266;
}

/* 快捷信息 */
.quick-info { padding: 0 12px; }
.info-card { border-radius: 8px; }
.info-header { font-size: 14px; font-weight: 600; color: #303133; }
.empty-tip { font-size: 13px; color: #c0c4cc; text-align: center; padding: 20px 0; }
.today-list { font-size: 13px; }
.today-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f2f5;
}
.today-item:last-child { border-bottom: none; }
.course-time { color: #007ab8; font-weight: 500; margin-right: 10px; white-space: nowrap; }
.course-name { flex: 1; color: #303133; }
.course-room { color: #909399; margin-left: 8px; }

/* 切换PC版 */
.switch-pc {
  text-align: center;
  padding: 20px 0 30px;
}
</style>
